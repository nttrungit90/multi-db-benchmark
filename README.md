# Multi DB Benchmark

Benchmark insert, upsert, and read operations across databases with a dashboard UI for comparison.

## Databases

- MySQL 8.0
- MongoDB 7.0
- Extensible: implement `DatabaseDriver` interface to add more

## Data Model

Table/Collection: `hotel_room_latest_price`

| Field | Type |
|---|---|
| hotel_id | bigint |
| wego_room_type_id | bigint |
| supplier_code | string |
| supplier_channel | string |
| checkin_date | date |
| final_price | decimal |
| updated_at | timestamp |

**Primary Key:** `(hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date)`

## Quick Start

```bash
docker compose up -d --build
```

- Dashboard: http://localhost:3000
- Backend API: http://localhost:8080

## Workloads

| Workload | Description |
|---|---|
| insert | Batch insert with synthetic data |
| upsert | Batch upsert by primary key |
| read | Read by hotel_id |

## Benchmark Plans

Each benchmark run can be assigned a **plan name** (e.g. `mysql-1M-b500`, `mongo-1M-b1000`). Plans allow you to:

- Group results from a single run
- Compare multiple plans side-by-side with charts
- Filter history by plan

## API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/bench/run` | Run benchmark |
| GET | `/bench/results` | Get results (filter: `?db=` or `?plan=`) |
| GET | `/bench/databases` | List available databases |
| GET | `/bench/plans` | List all plan names |
| GET | `/bench/schemas` | Schema, indexes, and queries per database |
| GET | `/bench/resources` | Resource limits per database |
| DELETE | `/bench/results/{id}` | Delete a single result |
| DELETE | `/bench/runs/{runId}` | Delete all results from a run |
| DELETE | `/bench/results` | Clear all results |

### Run Benchmark

```bash
curl -X POST http://localhost:8080/bench/run \
  -H 'Content-Type: application/json' \
  -d '{
    "database": "mysql",
    "workloads": ["insert", "upsert", "read"],
    "recordCount": 1000000,
    "batchSize": 500,
    "planName": "mysql-1M-b500"
  }'
```

## Resource Fairness

All databases run with **identical Docker resource limits** to ensure fair benchmarking:

| Resource | Default | Config |
|---|---|---|
| CPU | 2.0 cores | `DB_CPU_LIMIT` |
| Memory | 2 GB | `DB_MEM_LIMIT` |
| CPU Reserve | 1.0 cores | `DB_CPU_RESERVE` |
| Memory Reserve | 1 GB | `DB_MEM_RESERVE` |
| Network | Same bridge | `bench-net` |
| Storage | Same local disk | `./data/<db>` |

Override via `.env`:

```env
DB_CPU_LIMIT=4.0
DB_MEM_LIMIT=4G
```

## Dashboard

### Benchmark Tab
- Select database, workloads, record count, batch size, and plan name
- Run benchmark and view results grouped by run
- Delete individual results or entire runs

### Compare Plans Tab
- Select multiple plans to compare side-by-side
- Charts: throughput, latency, storage
- Detail comparison table

### Schema & Queries Tab
- Schema definition per database
- Indexes, primary key
- Insert, upsert, and read commands used

## Project Structure

```
backend/
  src/main/java/com/benchmark/
    core/           # Data model, DatabaseDriver interface
    drivers/        # MySqlDriver, MongoDriver
    workloads/      # DataGenerator, BenchmarkEngine
    api/            # REST controllers
frontend/
  src/
    App.jsx         # Main app with tab navigation
    Charts.jsx      # Throughput/latency/storage charts
    CompareCharts.jsx # Multi-plan comparison charts
    SchemaPage.jsx  # Schema & query viewer
    ResourceInfo.jsx # Resource config display
docker/
  mysql/init.sql    # MySQL schema
```

## Adding a New Database

1. Add the database service to `docker-compose.yml` with `<<: [*db-resources, *db-network]`
2. Implement `DatabaseDriver` interface as a Spring `@Component`
3. The driver auto-registers — no other changes needed
