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
docker compose -f applications/demo-app/docker-compose.yml up -d   # or: valkey-server --io-threads 4
./gradlew :applications:demo-app:bootRun
open http://localhost:9090/demo.html
```

## API

- `GET  /api/products/{id}` read-through cache
- `PUT  /api/products/{id}/price?price=9.99` write-through + publish event
- `GET  /api/popular?limit=5` top viewed (ZSET)
- `GET  /api/events` SSE stream of price changes
- `POST /api/benchmark?threads=16&ops=50000&writePercent=10`
- `GET  /api/stats`, `DELETE /api/cache`

## Suggested talk track

1. GET product 1 twice: ~150+ ms MISS, then HIT. Show the key with `valkey-cli GET catalog:products::1`.
2. Run "200 random views", then show the leaderboard and `ZREVRANGE catalog:views 0 4 WITHSCORES`.
3. Open the page in two tabs, change a price, watch both feeds update. Run `valkey-cli SUBSCRIBE product-events` too.
4. Run the benchmark, then compare with the database ops/sec line.
5. Point out that swapping Valkey for Redis (or back) needs only a host/port change.
