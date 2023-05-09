<img src="/paidy.png?raw=true" width=300 style="background-color:white;">

# Paidy Take-Home Coding Exercises

## Approach
The current implement generate and fetch all possible pairs of currency during a call and cache/memoize the result locally. We also store the timestamp along with the data so that if the data is older than a predefined time (5 minutes) in our cases then it'll refetch all the data and also update the cache. One initial call will be slower but subsequent calls will be faster. 

## Limitations
- If we have many concurrent requests at the end of 5th minute and oneframe is slow we might have more than 1000 calls to one frame. 
The solution to this limitation is asynchoronously calling and caching result from oneframe api by schedulling a job.
I've not implemented the cron job part as it requires a lot of setup and handling different cases and the instructions says to keep things simple. We may discuss the async solution further in the interview if I get a chance.


## Possible improvements
- Using redis to memoize the results insted of local so that multiple instances can be created and use the same cache.
- Unit tests.
- using a cron job to call oneframe asynchronously in the background and storing the result in the cache.
- Retring if oneframe returns an error. Maybe like 3 times before giving an error. 
- OpenAPI specifications.

## Tests
I've not written unit tests because of lack of time so I am adding what tests would I write and how I would write them here.
- I'd mock the oneframe api for testing.
- I'd write test for:
    - happy path where mock service returns response and currency pairs are valid.
    - a test for invalid currency pairs.
    - a case where mock service is down.
    - a case to see if caching is working correctly and data is coming from cache.

