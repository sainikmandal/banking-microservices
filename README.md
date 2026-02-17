# Banking Microservices

A production-ready banking system built with Spring Boot microservices, Spring Cloud, and Keycloak OAuth2.

## Architecture

```
                         ┌──────────────────┐
                         │   Keycloak       │
                         │   (Port 8180)    │
                         │   OAuth2 / OIDC  │
                         └────────┬─────────┘
                                  │ JWT validation
                         ┌────────▼─────────┐
  Clients ──────────────►│   API Gateway    │
                         │   (Port 8080)    │
                         └──┬─────┬──────┬──┘
                            │     │      │
              ┌─────────────┤     │      ├──────────────┐
              │             │     │      │              │
     ┌────────▼───────┐ ┌──▼─────▼──┐ ┌─▼────────────┐│
     │  Accounts      │ │ Customers │ │ Transactions  ││
     │  Service       │ │ Service   │ │ Service       ││
     │  (Port 8081)   │ │ (8082)    │ │ (8083)        ││
     └───────┬────────┘ └─────┬─────┘ └──┬────────────┘│
             │                │           │              │
     ┌───────▼────────┐ ┌────▼──────┐ ┌──▼───────────┐ │
     │ PostgreSQL     │ │PostgreSQL │ │ PostgreSQL   │ │
     │ accounts_db    │ │customers  │ │ transactions │ │
     │ (Port 5432)    │ │(Port 5433)│ │ (Port 5434)  │ │
     └────────────────┘ └───────────┘ └──────────────┘ │
                                                        │
     ┌──────────────────────────────────────────────────┘
     │
     │  ┌─────────────────┐    ┌──────────────────┐
     └──│  Eureka Server  │    │  Config Server   │
        │  (Port 8761)    │    │  (Port 8888)     │
        │  Discovery      │    │  Centralized     │
        └─────────────────┘    └──────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3.5 |
| Cloud | Spring Cloud 2023.0.3 |
| Security | Keycloak 23 + OAuth2/OIDC |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Discovery | Netflix Eureka |
| Gateway | Spring Cloud Gateway |
| Inter-service | OpenFeign + Resilience4j |
| API Docs | SpringDoc OpenAPI 3 |
| Build | Maven |
| Containers | Docker + Docker Compose |

## Service Communication

### How Services Talk to Each Other

**1. Service Discovery (Eureka)**

All services register with Eureka Server on startup. When one service needs to call another, it looks up the target's address from Eureka rather than using hardcoded URLs. This allows services to scale horizontally and handles instance failures gracefully.

**2. API Gateway Routing**

All external traffic enters through the API Gateway (port 8080). The gateway uses Eureka to discover service instances and load-balance requests:

- `/api/accounts/**` routes to `accounts-service`
- `/api/customers/**` routes to `customers-service`
- `/api/transactions/**` routes to `transactions-service`

**3. Synchronous Communication (Feign)**

The Transactions Service calls the Accounts Service via OpenFeign to validate accounts and update balances during transaction processing. JWT tokens are automatically propagated between services using a Feign request interceptor.

**4. Circuit Breaker (Resilience4j)**

The Transactions Service uses Resilience4j circuit breaker when calling the Accounts Service. If the Accounts Service is down, the circuit opens and a fallback response is returned instead of cascading failures.

**5. Centralized Configuration**

The Config Server serves configuration to all services at startup. Service-specific YAML files are stored in `config-server/src/main/resources/config/` and loaded via Spring Cloud Config.

### Authentication Flow

```
1. Client authenticates with Keycloak → receives JWT token
2. Client sends request to API Gateway with Bearer token
3. Gateway validates JWT with Keycloak's JWKS endpoint
4. Gateway forwards request to target service (token included)
5. Service validates JWT independently using Keycloak's public keys
6. Keycloak roles (realm_access.roles) mapped to Spring Security authorities
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- (Optional) IntelliJ IDEA or VS Code

## Getting Started

### Option 1: Docker Compose (Recommended)

Start the entire stack with one command:

```bash
docker-compose up -d
```

This spins up Keycloak, three PostgreSQL instances, Eureka, Config Server, API Gateway, and all three business services.

### Option 2: Local Development

**Step 1 — Start infrastructure**

```bash
docker-compose up -d keycloak postgres-accounts postgres-customers postgres-transactions
```

**Step 2 — Build the project**

```bash
mvn clean install -DskipTests
```

**Step 3 — Start services in order**

Start each service in a separate terminal. Order matters because of dependency chains:

```bash
# Terminal 1: Eureka Server (must start first)
cd eureka-server && mvn spring-boot:run

# Terminal 2: Config Server (needs Eureka)
cd config-server && mvn spring-boot:run

# Terminal 3: Accounts Service
cd accounts-service && mvn spring-boot:run

# Terminal 4: Customers Service
cd customers-service && mvn spring-boot:run

# Terminal 5: Transactions Service (needs Accounts Service)
cd transactions-service && mvn spring-boot:run

# Terminal 6: API Gateway (start last)
cd api-gateway && mvn spring-boot:run
```

### Step 4 — Configure Keycloak

1. Open Keycloak admin console: http://localhost:8180
2. Login with `admin` / `admin`
3. Create a new realm called `banking`
4. Under the `banking` realm, create a client:
   - Client ID: `banking-app`
   - Client authentication: ON
   - Valid redirect URIs: `http://localhost:3000/*`
   - Web origins: `http://localhost:3000`
5. Create realm roles: `ADMIN`, `USER`, `TELLER`
6. Create a test user and assign roles

### Step 5 — Get a Token and Test

```bash
# Get an access token from Keycloak
TOKEN=$(curl -s -X POST http://localhost:8180/realms/banking/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=banking-app" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=testuser" \
  -d "password=testpass" | jq -r '.access_token')

# Create a customer
curl -s -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jordan",
    "lastName": "Lee",
    "email": "jordan.lee@bank.com",
    "phone": "+31 690000000",
    "address": "10 Bank Avenue, Rotterdam"
  }' | jq

# Create an account
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "type": "SAVINGS"
  }' | jq

# Make a deposit
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "DEPOSIT",
    "amount": 5000.00,
    "description": "Initial deposit"
  }' | jq
```

## API Documentation

Each service exposes Swagger UI when running:

| Service | Swagger UI |
|---------|-----------|
| Accounts | http://localhost:8081/swagger-ui.html |
| Customers | http://localhost:8082/swagger-ui.html |
| Transactions | http://localhost:8083/swagger-ui.html |

## Port Reference

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Accounts Service | 8081 |
| Customers Service | 8082 |
| Transactions Service | 8083 |
| Eureka Server | 8761 |
| Config Server | 8888 |
| Keycloak | 8180 |
| PostgreSQL (accounts) | 5432 |
| PostgreSQL (customers) | 5433 |
| PostgreSQL (transactions) | 5434 |

## Debugging

### Check Eureka Dashboard

Visit http://localhost:8761 to see which services are registered and healthy.

### Check Service Health

```bash
# Eureka
curl http://localhost:8761/actuator/health

# Config Server
curl http://localhost:8888/actuator/health

# Accounts Service
curl http://localhost:8081/actuator/health

# Customers Service
curl http://localhost:8082/actuator/health

# Transactions Service
curl http://localhost:8083/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health
```

### View Config Server Entries

```bash
# See what config the accounts-service receives
curl http://localhost:8888/accounts-service/default
```

### Common Issues

**Services not registering with Eureka**
- Verify Eureka is running first: `curl http://localhost:8761/actuator/health`
- Check `eureka.client.service-url.defaultZone` in each service's config
- If running in Docker, use container hostname (`eureka-server`) not `localhost`

**JWT validation failing (401 Unauthorized)**
- Confirm Keycloak is running: `curl http://localhost:8180/realms/banking/.well-known/openid-configuration`
- Check that the `banking` realm exists in Keycloak
- Verify `issuer-uri` and `jwk-set-uri` point to the correct Keycloak address
- In Docker, services must use `http://keycloak:8180` (container name), not `localhost`

**Database connection refused**
- Check PostgreSQL containers are running: `docker-compose ps`
- Verify port mappings (5432, 5433, 5434) aren't conflicting with local PostgreSQL
- Confirm credentials match between `docker-compose.yml` and service configs

**Config Server not serving config**
- Ensure config files exist in `config-server/src/main/resources/config/`
- File names must match the `spring.application.name` of each service (e.g., `accounts-service.yml`)
- Test directly: `curl http://localhost:8888/accounts-service/default`

**Transactions failing with "Account service unavailable"**
- This means the Resilience4j circuit breaker tripped. The Accounts Service may be down.
- Check Accounts Service health and Eureka registration
- The circuit breaker will auto-recover after `wait-duration-in-open-state` (default 10s)

### Enable Debug Logging

Add to any service's `application.yml`:

```yaml
logging:
  level:
    com.banking: DEBUG
    org.springframework.cloud: DEBUG
    org.springframework.security: DEBUG
```

## Project Structure

```
banking-microservices/
├── common-lib/              # Shared DTOs, exceptions, utilities, Keycloak converter
├── config-server/           # Centralized configuration (port 8888)
├── eureka-server/           # Service discovery (port 8761)
├── api-gateway/             # Entry point, routing, security (port 8080)
├── accounts-service/        # Account CRUD + balance operations (port 8081)
├── customers-service/       # Customer registration + profile management (port 8082)
├── transactions-service/    # Transaction processing with Feign + circuit breaker (port 8083)
├── docker-compose.yml       # Full stack orchestration
├── pom.xml                  # Parent POM with shared dependencies
└── README.md
```
