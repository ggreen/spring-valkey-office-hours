x a# Spring + Valkey Product Catalog Demo

Cache-aside demo for Spring Java developers. A simulated slow system of record (150 ms per call)
holds 1,000 products; Valkey sits in front of it through Spring's cache abstraction.

| Why caching | Where to see it |
|---|---|
| Fast access, low latency | `ProductService.get` (`@Cacheable`). `X-Cache: MISS/HIT` and `X-Elapsed-Ms` headers |
| Drop-in Redis replacement | Plain `spring-boot-starter-data-redis` (Lettuce), no Valkey-specific code |
| High-throughput, multi-threaded | `POST /api/benchmark` against a server started with `--io-threads 4` |
| Key/value data store | JSON values under `catalog:products::<id>`; sorted-set leaderboard `catalog:views` |
| Event-driven data | `ProductEvents`: price updates published on pub/sub channel `product-events`, streamed to the browser via SSE |

## Run

```bash
./deployments/local/valkey.sh
```

## API

- `GET  /api/products/{id}` read-through cache
- `PUT  /api/products/{id}/price?price=9.99` write-through + publish event
- `GET  /api/popular?limit=5` top viewed (ZSET)
- `GET  /api/events` SSE stream of price changes
- `POST /api/benchmark?threads=16&ops=50000&writePercent=10`
- `GET  /api/stats`, `DELETE /api/cache`


```shell
curl -X 'POST' \
  'http://localhost:9090/api/products/product' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "id": "101",
  "name": "Spring Peanut Butter",
  "category": "food",
  "price": 2500000,
  "stock": 1000,
  "updatedAt": 1795742340
}'
```

```shell
curl -i "http://localhost:9090/api/products/101"
```

```shell
curl -N -H "Accept: text/event-stream" "http://localhost:9090/api/events"
```

Write through

```shell
curl -i -X PUT "http://localhost:9090/api/products/101/price?price=9.99"
```

Fetches the top N products tracked in your Valkey ZSET (call GET /api/products/{id} a few times first to populate view counts):

Bash
```shell
curl -s "http://localhost:9090/api/popular?limit=5"
```

Run Benchmark (POST /api/benchmark)
Triggers the load/throughput benchmark with your query parameters:

Bash
```shell
curl -i -X POST "http://localhost:9090/api/benchmark?threads=16&ops=50000&writePercent=10"
```


# Get cache statistics
```shell
curl -s "http://localhost:9090/api/stats"
```
Flush Cache

```shell
curl -i -X DELETE "http://localhost:9090/api/cache"
```

## Suggested talk track

1. GET product 1 twice: ~150+ ms MISS, then HIT. Show the key with `valkey-cli GET catalog:products::1`.
2. Run "200 random views", then show the leaderboard and `ZREVRANGE catalog:views 0 4 WITHSCORES`.
3. Open the page in two tabs, change a price, watch both feeds update. Run `valkey-cli SUBSCRIBE product-events` too.
4. Run the benchmark, then compare with the database ops/sec line.
5. Point out that swapping Valkey for Redis (or back) needs only a host/port change.
