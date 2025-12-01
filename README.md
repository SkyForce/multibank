# 📈 MultibankFX – High-Performance Real-Time Market Data Aggregation

MultibankFX is a **high-throughput real-time candle engine** built with:

- **Spring Boot 3 (Java 21)**
- **TimescaleDB (PostgreSQL 17)**
- **Liquibase** for schema migrations
- **JdbcTemplate** with optimized batch/UPSERT logic
- **Continuous Aggregates** for ultra-fast historical reads
- **Testcontainers** for full integration tests
- Real-time **Bid/Ask → Candlesticks** aggregation

The system ingests market events at high speed, aggregates them into multi-interval candles (1m, 5m, 1h, etc.), and exposes them via a REST API.

---

## 🚀 Features

### ✔ Real-Time Candle Aggregation
- Ingests bid/ask events and computes mid-price
- Generates real-time candles and UPSERT in DB
- Stores raw ticks data
- Continuous aggregates for adding new intervals on past data
- Supports multiple trading symbols

### ✔ TimescaleDB Optimized Storage
- `TIMESTAMPTZ`-based hypertables
- Automatic chunking
- Continuous aggregates for new intervals on past data:
    - example: 7m interval
- Low-latency historical queries

### ✔ High-Speed Write Pipeline
- JDBC batch inserts
- Optimized UPSERT:
  ```sql
  ON CONFLICT (symbol, time)
  DO UPDATE SET
      high   = GREATEST(candles_rt.high, EXCLUDED.high),
      low    = LEAST(candles_rt.low, EXCLUDED.low),
      close  = EXCLUDED.close,
      volume = candles_rt.volume + EXCLUDED.volume;
- COPY mode support for bulk loads

### ✔ Liquibase Migrations

- Automatic schema creation

- Hypertables + indexes

- Continuous aggregate definitions

### ✔ Integration Testing

- Testcontainers with TimescaleDB pg17

- Liquibase auto-loaded inside each container

### 🛠 Installation
1. Run TimescaleDB locally in Docker

2. Configure Spring Boot
src/main/resources/application.properties:

- spring.datasource.url=jdbc:postgresql://localhost:5432/multi

- spring.datasource.username=postgres

- spring.datasource.password=123

- spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml

3. Start the application

 -   mvn spring-boot:run

🧪 Running Tests

Integration tests use Testcontainers:

- mvn test


During test execution:

- A TimescaleDB pg17 container starts automatically

- Liquibase initializes schema inside the container

- JDBC batch writer tests run
