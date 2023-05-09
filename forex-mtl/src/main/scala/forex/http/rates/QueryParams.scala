package forex.http.rates

import forex.domain.Currency
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.ParseFailure
import cats.implicits._

object QueryParams {
  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap( currencyCode => {
      Either
        .catchOnly[MatchError](Currency.fromString(currencyCode))
        .leftMap(_ => ParseFailure(s"unsupported currency: $currencyCode", s"currency parse error, $currencyCode"))
    })
  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

}
