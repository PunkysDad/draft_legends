# Draft Legends - Backend API

Spring Boot / Kotlin backend API for Legends Clash.

## Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot 3.2.x
- **Build Tool:** Gradle (Kotlin DSL)
- **Database:** PostgreSQL
- **Java:** 17

## Dependencies

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Boot Actuator

## Getting Started

### Prerequisites

- JDK 17+
- PostgreSQL running locally (or configure via environment variables)

### Configuration

The application uses environment variables with sensible defaults. Override as needed:

| Variable       | Default       | Description          |
|----------------|---------------|----------------------|
| `DB_HOST`      | `localhost`   | Database host        |
| `DB_PORT`      | `5432`        | Database port        |
| `DB_NAME`      | `draftlegends`| Database name        |
| `DB_USERNAME`  | `postgres`    | Database username    |
| `DB_PASSWORD`  | `postgres`    | Database password    |
| `SERVER_PORT`  | `8080`        | Application port     |
| `JPA_DDL_AUTO` | `update`      | Hibernate DDL mode   |

### Build and Run

```bash
./gradlew build
./gradlew bootRun
```

### Actuator Endpoints

Health check and metrics are available at:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
