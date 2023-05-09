package forex.services.rates.interpreters

import cats.Applicative
import forex.services.rates.Algebra
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.OneFrameConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.implicits.OneFrameRate
import forex.services.rates.errors._
import io.circe.parser._

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.{Duration, Instant, ZoneOffset}




class OneFrameLive[F[_]: Applicative](config : OneFrameConfig) extends Algebra[F] {
  private var cache : Map[Rate.Pair, (Price, Timestamp)] = Map.empty
  private val request = getRequest()

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] = {
    getRateFromCache(pair) match {
      case Some(rate) => rate.asRight[Error].pure[F]
      case None => getRateFromApi(pair).pure[F]
    }
  }

  private def getRateFromApi(pair: Rate.Pair): Either[Error, Rate] = {
    getAllPairRates().flatMap { rates =>
      val (price, timestamp) = rates(pair)
      cache = rates
      Right(Rate(pair, price, timestamp))
    }
  }

  private def getRateFromCache(pair: Rate.Pair): Option[Rate] = {
    cache.get(pair).flatMap {
      case (price, timestamp) =>
        val timeSinceFetch = Duration.between(timestamp.value, Timestamp.now.value).toSeconds()
        if (timeSinceFetch < 60) Some(Rate(pair, price, timestamp)) else None
    }
  }

  private def getRequest(): HttpRequest = {
    val queryParams: List[String] = for {
      pair <- Currency.allPairs
    } yield s"pair=${pair.from}${pair.to}"
    val endpoint = s"${config.url}/rates?" + queryParams.mkString("&")
    HttpRequest.newBuilder()
      .uri(new URI(endpoint))
      .header("token", config.token)
      .GET()
      .build()
  }

  private def getAllPairRates(): Either[Error, Map[Rate.Pair, (Price, Timestamp)]] = {
    val response = HttpClient.newHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
    for {
      jsonResp <- decode[List[OneFrameRate]](response.body()).left.map(error =>
        Error.OneFrameLookupFailed(s"Fail to parse OneFrame response. Error: ${error.getMessage}")
      )
      pairs = jsonResp.map(x => {
          (Rate.Pair(Currency.fromString(x.from), Currency.fromString(x.to)), (Price(x.price),
            Timestamp(Instant.parse(x.timestamp).atOffset(ZoneOffset.UTC))))
        }
      )
    } yield pairs.toMap
  }
}
