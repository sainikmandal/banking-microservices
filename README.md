# Banking Microservices — Complete Project Guide

> A production-ready microservices architecture for core banking operations: Account Management, Customer Profiles, and Transaction Processing — secured with Keycloak OAuth2/JWT, load-balanced via Spring Cloud Gateway, and discovered through Eureka.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Services at a Glance](#2-services-at-a-glance)
3. [Interprocess Communication & Request Flow](#3-interprocess-communication--request-flow)
4. [Implementation Status vs. Use Case 7 Requirements](#4-implementation-status-vs-use-case-7-requirements)
5. [Best Practices & Code Review](#5-best-practices--code-review)
6. [Prerequisites](#6-prerequisites)
7. [Step 1 — MySQL Setup](#7-step-1--mysql-setup)
8. [Step 2 — Keycloak Setup (Full Guide)](#8-step-2--keycloak-setup-full-guide)
9. [Step 3 — Start All Servers (Correct Order)](#9-step-3--start-all-servers-correct-order)
10. [Step 4 — Verify Everything is Running](#10-step-4--verify-everything-is-running)
11. [Step 5 — Obtain a Token & Call APIs](#11-step-5--obtain-a-token--call-apis)
12. [Swagger UI Access](#12-swagger-ui-access)
13. [Docker Compose (Gateway)](#13-docker-compose-gateway)
14. [Common Pitfalls & Warnings](#14-common-pitfalls--warnings)
15. [API Reference Summary](#15-api-reference-summary)
16. [Known Gaps & Recommended Next Steps](#16-known-gaps--recommended-next-steps)

---

## 1. Architecture Overview

```
                        ┌─────────────────────────────┐
                        │         KEYCLOAK            │
                        │   http://localhost:8080     │
                        │   Realm: banking-realm      │
                        │   Issues JWT Tokens         │
                        └──────────┬──────────────────┘
                                   │ (JWKS endpoint for token validation)
                                   ▼
          Client (Postman / Browser / Frontend)
                        │
                        │  Bearer JWT Token
                        ▼
          ┌─────────────────────────────┐
          │      API GATEWAY            │
          │   http://localhost:8765     │
          │   Spring Cloud Gateway      │
          │   Validates JWT             │
          │   Routes + Load Balances    │
          └───────┬──────────┬──────────┘
                  │          │ Discovers services via
                  │          ▼
          ┌───────────────────────┐
          │   EUREKA SERVER       │
          │ http://localhost:8761 │
          │ Service Registry      │
          └───────────────────────┘
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
  ┌─────────┐ ┌──────────┐ ┌──────────────┐
  │ ACCOUNT │ │ CUSTOMER │ │ TRANSACTION  │
  │ :8081   │ │ :8082    │ │ :8083        │
  │         │ │          │ │              │
  │Validates│ │Validates │ │Validates     │
  │  JWT    │ │  JWT     │ │  JWT         │
  └────┬────┘ └────┬─────┘ └──────┬───────┘
       │           │               │
       └───────────┼───────────────┘
                   ▼
          ┌──────────────────┐
          │    MySQL DB      │
          │ localhost:3306   │
          │ database:        │
          │  bankingdb       │
          └──────────────────┘
```

---

## 2. Services at a Glance

| Service | Module Path | Port | Spring Boot | Role |
|---|---|---|---|---|
| **Eureka Discovery Server** | `eurekadiscoveryserver/` | `8761` | 3.5.9 | Service Registry |
| **API Gateway** | `cloudapigateway/` | `8765` | 3.3.5 | Routing + JWT Guard |
| **Account Service** | `accountservice/accountserviceapi/` | `8081` | 3.4.2 | Account CRUD |
| **Customer Service** | `customerservice/customerserviceapi/` | `8082` | 3.4.2 | Customer CRUD |
| **Transaction Service** | `transactionservice/transactionserviceapi/` | `8083` | 3.4.2 | Transaction CRUD |

> **Note:** Eureka uses Spring Boot `3.5.9` and Spring Cloud `2025.0.1` — a newer version than the other services. While this works in practice, for a team project it is best to align all services to the same Spring Boot + Spring Cloud version pair.

---

## 3. Interprocess Communication & Request Flow

### How Services Talk to Each Other

This project uses **Eureka-based service discovery with load-balanced HTTP routing through the Gateway**. There is no direct service-to-service HTTP call in the current implementation (that would be the next enhancement, e.g., TransactionService calling AccountService to verify balance).

### Detailed Request Lifecycle

**Step 1 — Service Registration (on startup)**

Each microservice (Account, Customer, Transaction) registers itself with Eureka:
```
Service → POST http://localhost:8761/eureka/apps/{serviceName}
         "I am accountservice, running at 192.168.x.x:8081"
```
The Gateway also registers with Eureka and continuously fetches the service registry every 30 seconds.

**Step 2 — Client Authenticates with Keycloak**
```
Client → POST http://localhost:8080/realms/banking-realm/protocol/openid-connect/token
         grant_type=client_credentials&client_id=...&client_secret=...
         ← { access_token: "eyJhbGci..." }
```

**Step 3 — Client Calls API via Gateway**
```
Client → GET http://localhost:8765/accounts/v1.0
         Authorization: Bearer eyJhbGci...
```

**Step 4 — Gateway Validates the JWT**

The Gateway fetches Keycloak's public keys from:
```
http://localhost:8080/realms/banking-realm/protocol/openid-connect/certs
```
It verifies the token's signature, expiry, and issuer. If invalid, it returns `401` immediately — the downstream service never sees the request.

**Step 5 — Gateway Routes to the Correct Service**

The Gateway uses Eureka to resolve `lb://accountservice` to the actual IP/port, then forwards the request:
```
Gateway → GET http://192.168.x.x:8081/accounts/v1.0
          (original JWT is forwarded in the Authorization header)
```

**Step 6 — Service Validates the JWT Again**

Each service is also configured as an OAuth2 Resource Server. This is intentional **defence-in-depth** — if a service were ever called directly (bypassing the gateway in a container network), it still validates the token independently.

**Step 7 — Method-Level Authorization**

For write operations (`POST`, `PUT`, `DELETE`), the service checks:
```java
@PreAuthorize("hasAnyAuthority('SCOPE_developer')")
```
This means the JWT must contain a scope claim of `developer`. Read operations (`GET`) only require a valid authenticated token.

### Route Mapping (Gateway → Service)

| Gateway Path | Routes To | Service Port |
|---|---|---|
| `/accounts/**` | `lb://accountservice` | 8081 |
| `/customers/**` | `lb://customerservice` | 8082 |
| `/transactions/**` | `lb://transactionservice` | 8083 |
| `/accountservice/v3/api-docs/**` | Account Service Swagger docs | 8081 |
| `/customerservice/v3/api-docs/**` | Customer Service Swagger docs | 8082 |
| `/transactionservice/v3/api-docs/**` | Transaction Service Swagger docs | 8083 |

---

## 4. Implementation Status vs. Use Case 7 Requirements

### ✅ Fully Implemented

| Requirement | Details |
|---|---|
| Full CRUD — Accounts | `POST/GET/GET{id}/GET{number}/PUT/DELETE` all implemented |
| Full CRUD — Customers | `POST/GET/GET{id}/GET{email}/PUT/DELETE` all implemented |
| Full CRUD — Transactions | `POST/GET/GET{id}/GET{accountId}/PUT/DELETE` all implemented |
| Field Validation | Jakarta Validation on all DTOs (`@NotNull`, `@Email`, `@Pattern`, `@Positive`, `@Min`) |
| Error Handling | `GlobalExceptionHandler` returns structured JSON, no stack traces exposed |
| Standard HTTP Status Codes | `201 Created`, `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| Swagger / OpenAPI | SpringDoc configured with OAuth2 Authorize button on all services |
| Service Discovery (Eureka) | All 3 domain services register and the gateway discovers them |
| API Gateway | Spring Cloud Gateway with path-based routing and load balancing |
| Spring Security / JWT Auth | OAuth2 Resource Server at both Gateway and service level |
| Unique Constraint Enforcement | `accountNumber` and `email` are unique; duplicate inserts return `409` |
| Business Logic in Transactions | Only `PENDING` transactions can be amended or cancelled |
| Circuit Breaker (Resilience4j) | Configured on all 3 domain services |
| MapStruct Mappers | DTO ↔ Entity mapping across all services |
| Generic Response Wrapper | All endpoints return `GenericResponse<T>` with `status`, `message`, `data` |
| Method Comments (TransactionController) | Full Javadoc + Swagger `@Operation` annotations |
| HikariCP Connection Pool | Configured in TransactionService's `DBConfiguration` |
| Positive Amount Validation | Transactions validate `amount > 0` at DTO and service level |

### ⚠️ Partially Implemented

| Requirement | Gap |
|---|---|
| **Tests** | Only `contextLoads()` tests exist. No unit tests (JUnit/Mockito) or integration tests as required by the use case. |
| **Docker Compose** | Only `cloudapigateway/docker-compose.yml` exists. No Compose for Eureka or the 3 domain services. |
| **Swagger Comments** | `AccountController` and `CustomerController` lack `@Operation` annotations (TransactionController has them). |
| **Database Isolation** | All services share `bankingdb`. The use case specifies "each service has its own database tables for maximum isolation" — but separate *schemas* (e.g., `accountdb`, `customerdb`, `transactiondb`) would be truer isolation. |

### 🔴 Not Yet Implemented

| Requirement | Notes |
|---|---|
| **Kafka Event Streaming** | Dependency is in the pom.xml but is explicitly *excluded* via `spring.autoconfigure.exclude`. No producers, consumers, or topics are defined. |
| **HashiCorp Vault** | `VaultConfiguration` is misleadingly named — it is just a `@ConfigurationProperties` class reading credentials from `application.properties`. No actual Vault integration. |
| **Cross-Service Calls** | TransactionService does not call AccountService to verify account existence or check balance before creating a transaction. This is a significant business logic gap. |
| **Unit + Integration Tests** | No JUnit/Mockito unit tests for service logic. No Spring Integration tests for API flows. |
| **Spring Cloud Config Server** | No centralised config server. Each service manages its own `application.properties`. |

---

## 5. Best Practices & Code Review

### What's Done Well

**Consistent architecture across services.** All three domain services follow the same layered pattern: `Controller → Service → Repository`, with DTOs, mappers, custom exceptions, and a global handler. This makes the codebase highly predictable and maintainable.

**Stateless, JWT-based security.** Sessions are explicitly set to `STATELESS`. Form login and HTTP Basic are disabled. This is the correct approach for microservices.

**Generic response wrapper.** `GenericResponse<T>` wraps all responses uniformly (`success`, `error` factory methods). Clients always get a consistent envelope.

**Separation of credentials from datasource config.** The `VaultConfiguration` + `DBConfiguration` pattern correctly separates the URL from the credentials, making it ready to drop in a real secrets manager by just changing the source of `VaultConfiguration`'s values.

**Versioned API paths.** All endpoints are versioned (`/v1.0`), which is excellent practice for backward-compatible API evolution.

**Business-rule validation in TransactionService.** The guard against amending or cancelling non-`PENDING` transactions is proper domain logic, not just input validation.

**TransactionService uses `@RequiredArgsConstructor` (constructor injection).** This is the preferred style over `@Autowired` field injection — immutable, easier to test, and explicit. AccountService and CustomerService still use `@Autowired` field injection and should be migrated.

### Issues to Address

**`@CrossOrigin(origins = "*")` is overly permissive.** In production this allows any domain to call your services. Restrict it to your actual frontend origin(s):
```java
// Before (dangerous in production)
@CrossOrigin(origins = "*")

// After
@CrossOrigin(origins = {"https://yourapp.com", "http://localhost:3000"})
```

**Credentials hardcoded in `application.properties`.** The `banking.vault.mysqlpassword=pass` value is committed to source control. Use environment variables, Spring profiles, or a real secrets manager. See the "Common Pitfalls" section below for the full warning.

**Package naming inconsistencies.** The Eureka server package is `com.cognizant.hospitalmgmt` and the Gateway is `com.siemens`. These are clearly from projects that were repurposed. Rename them to `com.sainik.banking.eureka` and `com.sainik.banking.gateway` for professionalism and clarity.

**`spring-boot-starter-data-rest` is in the Account Service pom.xml but unused.** This dependency automatically exposes HATEOAS/HAL endpoints for all JPA repositories at `/accounts` (the Spring Data REST path, separate from your controller). This can expose unintended endpoints and cause route conflicts. Remove it unless you intentionally want HATEOAS support.

**Spring Boot version drift.** Eureka is on `3.5.9`, Gateway on `3.3.5`, the domain services on `3.4.2`. While they may work, version drift creates subtle bugs and makes dependency management harder. Standardise to a single version.

**Account and Customer Services use field injection (`@Autowired`).** Prefer constructor injection (`@RequiredArgsConstructor`) as used in `TransactionService`. Field injection hides dependencies and makes unit testing awkward.

**`GenericResponse` error method leaks exception message.** The catch-all handler in `GlobalException` returns `ex.getMessage()` directly:
```java
// Potentially leaks internal details
GenericResponse.error("An unexpected error occurred: " + ex.getMessage())
```
For production, log the full exception internally and return a generic message to the client.

---

## 6. Prerequisites

Before starting any server, make sure you have:

- **Java 21** — `java -version` should show `21.x.x`
- **Maven 3.9+** — `mvn -version`
- **MySQL 8.x** running on port `3306`
- **Keycloak 24+** (or latest) running on port `8080`
- At least **1.5 GB free RAM** (all 5 services running together)

---

## 7. Step 1 — MySQL Setup

All three domain services connect to the same MySQL instance. Create the database and user:

```sql
-- Connect as root
CREATE DATABASE IF NOT EXISTS bankingdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Optional: create a dedicated user instead of using root
CREATE USER IF NOT EXISTS 'bankinguser'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON bankingdb.* TO 'bankinguser'@'localhost';
FLUSH PRIVILEGES;
```

> **⚠️ IMPORTANT:** If you change the password from `pass` to something else, you must update `banking.vault.mysqlpassword` in **all three** `application.properties` files:
> - `accountservice/accountserviceapi/src/main/resources/application.properties`
> - `customerservice/customerserviceapi/src/main/resources/application.properties`
> - `transactionservice/transactionserviceapi/src/main/resources/application.properties`
>
> See [Common Pitfalls](#14-common-pitfalls--warnings) for the full warning on this.

Hibernate is set to `ddl-auto=update`, which means all tables (`accounts`, `customers`, `transactions`) are **created automatically** on first startup. You do not need to run any SQL migration scripts.

---

## 8. Step 2 — Keycloak Setup (Full Guide)

Keycloak is the Identity Provider that issues JWT tokens. Every service validates tokens against it.

### 8.1 Start Keycloak

**Using Docker (recommended):**
```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

Or download from [keycloak.org](https://www.keycloak.org/downloads) and run:
```bash
# Linux/Mac
bin/kc.sh start-dev

# Windows
bin\kc.bat start-dev
```

Open `http://localhost:8080` and log in with `admin / admin`.

---

### 8.2 Create the Realm

1. In the top-left dropdown (it shows `master`), click **Create Realm**.
2. Set **Realm name** to exactly: `banking-realm`
3. Leave **Enabled** toggled ON.
4. Click **Create**.

> **⚠️ The realm name must be `banking-realm` exactly** — all `application.properties` files and the Gateway `application.yml` reference this name. If you use a different name, update every occurrence of `keycloak.realm` and every URL containing `/realms/banking-realm/`.

---

### 8.3 Create a Client

A "client" in Keycloak represents an application (your Gateway or a service) that can request tokens.

1. In the left menu, go to **Clients** → **Create client**.
2. Set **Client ID** to: `banking-client`
3. Set **Client type** to: `OpenID Connect`
4. Click **Next**.
5. On the **Capability config** page:
   - Toggle **Client authentication** to **ON** (this makes it a "confidential" client, which has a client secret).
   - Toggle **Authorization** to **OFF**.
   - Under "Authentication flow", check **Standard flow** (Authorization Code) and **Service accounts roles** (for client credentials flow).
6. Click **Next**.
7. On **Login settings**, set:
   - **Root URL**: `http://localhost:8765` (the gateway URL)
   - **Valid redirect URIs**: `http://localhost:8765/*`, `http://localhost:8081/*`, `http://localhost:8082/*`, `http://localhost:8083/*`
   - **Valid post logout redirect URIs**: `http://localhost:8765/*`
   - **Web origins**: `*` (for development; restrict in production)
8. Click **Save**.

> **⚠️ Root URL matters.** The Root URL is prepended to relative redirect URIs. If you set it to `http://localhost:8081` but your client also needs to call from the gateway at `8765`, add explicit entries to "Valid redirect URIs" for each port. Incorrect Root URL / redirect URI mismatches are the #1 cause of Keycloak `redirect_uri_mismatch` errors.

---

### 8.4 Get the Client Secret

1. Go to **Clients** → `banking-client` → **Credentials** tab.
2. Copy the **Client secret** value. You will use this to get tokens via `client_credentials` grant.

---

### 8.5 Create a Scope Named `developer`

The services use `@PreAuthorize("hasAnyAuthority('SCOPE_developer')")`. This checks for a scope called `developer` in the JWT. You must create and assign this scope.

1. In the left menu, go to **Client scopes** → **Create client scope**.
2. Set **Name** to: `developer`
3. Set **Type** to: `Optional`
4. Leave **Protocol** as `openid-connect`.
5. Click **Save**.

Now assign this scope to your client:

1. Go to **Clients** → `banking-client` → **Client scopes** tab.
2. Click **Add client scope**.
3. Find `developer` in the list, select it, and choose **Add** → **Optional**.

> **⚠️ If the `developer` scope is not assigned**, all `POST`, `PUT`, and `DELETE` requests will return `403 Forbidden`, even with a valid token. The `GET` endpoints will still work because they only require `authenticated()`.

---

### 8.6 Create a Test User (for Authorization Code / Password Grant)

For testing via Swagger UI or Postman with a user-facing flow:

1. Go to **Users** → **Add user**.
2. Set **Username**: `bankingadmin`
3. Toggle **Email verified** to ON.
4. Click **Create**.
5. Go to the **Credentials** tab → **Set password**.
6. Enter a password, toggle **Temporary** to OFF, click **Save password**.

Now assign the `developer` scope/role to this user via the service account or directly through a client scope mapping if using the password grant.

---

### 8.7 Verify the Realm is Working

Test the token endpoint directly:
```bash
curl -s -X POST \
  http://localhost:8080/realms/banking-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=banking-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "scope=developer" \
  | python3 -m json.tool
```

You should receive a JSON response with an `access_token`. Decode it at [jwt.io](https://jwt.io) and verify:
- `iss` = `http://localhost:8080/realms/banking-realm`
- `scope` includes `developer`

---

## 9. Step 3 — Start All Servers (Correct Order)

> **Order matters.** Eureka must be running before any service tries to register. The Gateway must know about Eureka before it can route to services. The domain services can start in any order after Eureka.

### Startup Order

```
1. Eureka Discovery Server  (must be FIRST)
2. API Gateway              (must be SECOND — needs Eureka)
3. Account Service          (any order after Eureka)
4. Customer Service         (any order after Eureka)
5. Transaction Service      (any order after Eureka)
```

### Running with Maven (Development)

Open a separate terminal for each service.

**Terminal 1 — Eureka Server:**
```bash
cd eurekadiscoveryserver
./mvnw spring-boot:run
# Wait until you see: "Started EurekadiscoveryserverApplication"
# Dashboard: http://localhost:8761
```

**Terminal 2 — API Gateway:**
```bash
cd cloudapigateway
./mvnw spring-boot:run
# Wait until you see: "Started CloudapigatewayApplication"
```

**Terminal 3 — Account Service:**
```bash
cd accountservice/accountserviceapi
./mvnw spring-boot:run
# Port 8081
```

**Terminal 4 — Customer Service:**
```bash
cd customerservice/customerserviceapi
./mvnw spring-boot:run
# Port 8082
```

**Terminal 5 — Transaction Service:**
```bash
cd transactionservice/transactionserviceapi
./mvnw spring-boot:run
# Port 8083
```

### Running with a Built JAR

If you prefer to build first:
```bash
# In each service directory
./mvnw clean package -DskipTests

# Then run
java -jar target/*.jar
```

### Passing Credentials via Environment Variables (Recommended)

Instead of editing `application.properties`, override sensitive values at runtime:
```bash
java -jar target/*.jar \
  --banking.vault.mysqlusername=bankinguser \
  --banking.vault.mysqlpassword=your_secure_password
```

Or via environment variables:
```bash
export BANKING_VAULT_MYSQLUSERNAME=bankinguser
export BANKING_VAULT_MYSQLPASSWORD=your_secure_password
java -jar target/*.jar
```

---

## 10. Step 4 — Verify Everything is Running

### Check Eureka Dashboard

Open: `http://localhost:8761`

You should see three services registered:

```
ACCOUNTSERVICE    UP  (1)  192.168.x.x:8081
CUSTOMERSERVICE   UP  (1)  192.168.x.x:8082
TRANSACTIONSERVICE UP (1)  192.168.x.x:8083
```

> If a service does not appear within 30–60 seconds, check the service's console for errors. The most common cause is MySQL not running or wrong credentials.

### Check Actuator Health Endpoints

```bash
# Eureka
curl http://localhost:8761/actuator/health

# Gateway
curl http://localhost:8765/actuator/health

# Account Service (direct)
curl http://localhost:8081/actuator/health

# Customer Service (direct)
curl http://localhost:8082/actuator/health

# Transaction Service (direct)
curl http://localhost:8083/actuator/health
```

All should return: `{"status":"UP"}`

### Check Gateway Routes

```bash
curl http://localhost:8765/actuator/gateway/routes
```

You should see the configured routes for `accountservice`, `customerservice`, and `transactionservice`.

---

## 11. Step 5 — Obtain a Token & Call APIs

### Get an Access Token

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/banking-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=banking-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "scope=developer" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### Example API Calls (via Gateway)

**Create a Customer:**
```bash
curl -s -X POST http://localhost:8765/customers/v1.0 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jordan",
    "lastName": "Lee",
    "email": "jordan.lee@bank.com",
    "phone": "+31690000000",
    "address": "10 Bank Avenue, Rotterdam"
  }' | python3 -m json.tool
```

**Create an Account:**
```bash
curl -s -X POST http://localhost:8765/accounts/v1.0 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "NL91ABNA0417164300",
    "customerId": 1,
    "type": "Savings",
    "balance": 5000.00
  }' | python3 -m json.tool
```

**Create a Transaction:**
```bash
curl -s -X POST http://localhost:8765/transactions/v1.0 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "Deposit",
    "amount": 750.00,
    "status": "PENDING"
  }' | python3 -m json.tool
```

**Get All Accounts (no `developer` scope required for GET):**
```bash
curl -s http://localhost:8765/accounts/v1.0 \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

---

## 12. Swagger UI Access

Each service exposes its own Swagger UI. You can also access all docs through the gateway.

| Service | Direct Swagger URL | Via Gateway |
|---|---|---|
| Account Service | `http://localhost:8081/swagger-ui.html` | `http://localhost:8765/accountservice/v3/api-docs` |
| Customer Service | `http://localhost:8082/swagger-ui.html` | `http://localhost:8765/customerservice/v3/api-docs` |
| Transaction Service | `http://localhost:8083/swagger-ui.html` | `http://localhost:8765/transactionservice/v3/api-docs` |

### Authorising in Swagger UI

1. Open a service's Swagger UI.
2. Click the **Authorize** button (padlock icon).
3. In the OAuth2 section, enter:
   - **client_id**: `banking-client`
   - **client_secret**: your client secret from Keycloak
   - **scope**: `openid developer`
4. Click **Authorize**. Swagger will redirect to Keycloak for login, then return with a token.
5. All subsequent Swagger requests will include the token automatically.

---

## 13. Docker Compose (Gateway)

A `docker-compose.yml` exists in `cloudapigateway/`. It assumes the gateway image has been built and a Docker network `bofanetwork` has been created.

```bash
# Create the external network first
docker network create bofanetwork

# Build the gateway image
cd cloudapigateway
docker build -t gatewayapp .

# Start the gateway container
docker-compose up -d
```

> **Note:** The `docker-compose.yml` only covers the gateway. A full Docker Compose file for all 5 services (including Eureka, the 3 domain services, MySQL, and Keycloak) does not exist yet. Creating one is a recommended next step — see [Known Gaps](#16-known-gaps--recommended-next-steps).

---

## 14. Common Pitfalls & Warnings

### ⚠️ WARNING: Never Change Credentials Only in `application.properties`

The credentials (`banking.vault.mysqlusername` / `banking.vault.mysqlpassword`) appear in **three separate `application.properties` files** — one per domain service. If you change the password in only one file, two services will fail to connect to the database on startup with a cryptic `Access denied for user` error.

**Files to update when changing DB credentials:**
```
accountservice/accountserviceapi/src/main/resources/application.properties
customerservice/customerserviceapi/src/main/resources/application.properties
transactionservice/transactionserviceapi/src/main/resources/application.properties
```

The safest approach is to **not store credentials in these files at all** and use environment variables or a secrets manager.

---

### ⚠️ WARNING: Do Not Commit Real Credentials to Git

The `application.properties` files currently contain `pass` as the MySQL password. If you change this to a real password and commit it, your credentials are in your git history forever (even if you later change the file). Use `.gitignore` to exclude local `application-local.properties` files, or use environment variable overrides.

---

### ⚠️ WARNING: Keycloak Realm Name is Hardcoded Everywhere

The string `banking-realm` appears in every service's `application.properties` and `application.yml`. If you rename your realm, update every occurrence:

```
# Search all files for this string:
grep -r "banking-realm" --include="*.properties" --include="*.yml" .
```

---

### ⚠️ WARNING: Start Eureka BEFORE Domain Services

If you start Account Service before Eureka, it will log repeated errors like:
```
Cannot execute request on any known server
```
This is not fatal — the service will keep retrying and register once Eureka is available — but it fills logs with noise. Always start in order: Eureka → Gateway → Domain Services.

---

### ⚠️ WARNING: Gateway Uses Reactive (WebFlux), Not Servlet (MVC)

The Gateway uses Spring Cloud Gateway which is built on **Project Reactor / WebFlux**. Do NOT add `spring-boot-starter-web` to the gateway's pom.xml — it will cause a startup conflict. The pom.xml already has a comment explaining this. The gateway's security config must also use `ServerHttpSecurity` (reactive), not `HttpSecurity` (servlet).

---

### ⚠️ WARNING: `spring-boot-starter-data-rest` in Account Service Pom

This dependency is present but intentional use is unclear. Spring Data REST auto-exposes your `AccountRepository` at `/accounts` as a HAL/HATEOAS endpoint, which can conflict with or shadow your manually defined `AccountController` routes. If you don't need HATEOAS, remove it:

```xml
<!-- Remove this from accountservice/accountserviceapi/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

---

### ⚠️ WARNING: `ddl-auto=update` is Risky in Production

`spring.jpa.hibernate.ddl-auto=update` is convenient for development (tables are auto-created/updated) but dangerous in production — a change to your entity class could alter or drop columns with live data. Before deploying to any non-development environment, switch to `validate` or `none` and use a proper migration tool (Flyway or Liquibase).

---

### ⚠️ WARNING: Keycloak `developer` Scope Must Be Included in Token Request

If you get `403 Forbidden` on write operations, the most likely cause is that your token was issued without the `developer` scope. Always include `scope=developer` in your token request. Confirm by decoding your token at jwt.io and checking the `scope` field.

---

### ⚠️ WARNING: Keycloak Token Expiry

By default, Keycloak access tokens expire after 5 minutes. In Postman or scripts, refresh the token before re-running requests. In Swagger UI, re-authorise when the session expires.

---

## 15. API Reference Summary

### Account Service — `http://localhost:8765/accounts`

| Method | Path | Auth Required | Role Required | Description |
|---|---|---|---|---|
| `POST` | `/v1.0` | ✅ JWT | `developer` scope | Create new account |
| `GET` | `/v1.0` | ✅ JWT | Any authenticated | Get all accounts |
| `GET` | `/v1.0/{id}` | ✅ JWT | Any authenticated | Get account by ID |
| `GET` | `/v1.0/number/{accountNumber}` | ✅ JWT | Any authenticated | Get account by number |
| `PUT` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Update account |
| `DELETE` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Delete account |

### Customer Service — `http://localhost:8765/customers`

| Method | Path | Auth Required | Role Required | Description |
|---|---|---|---|---|
| `POST` | `/v1.0` | ✅ JWT | `developer` scope | Register new customer |
| `GET` | `/v1.0` | ✅ JWT | Any authenticated | Get all customers |
| `GET` | `/v1.0/{id}` | ✅ JWT | Any authenticated | Get customer by ID |
| `GET` | `/v1.0/email/{email}` | ✅ JWT | Any authenticated | Get customer by email |
| `PUT` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Update customer |
| `DELETE` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Delete customer |

### Transaction Service — `http://localhost:8765/transactions`

| Method | Path | Auth Required | Role Required | Description | Business Rule |
|---|---|---|---|---|---|
| `POST` | `/v1.0` | ✅ JWT | `developer` scope | Initiate transaction | Amount > 0 |
| `GET` | `/v1.0` | ✅ JWT | Any authenticated | Get all transactions | — |
| `GET` | `/v1.0/{id}` | ✅ JWT | Any authenticated | Get transaction by ID | — |
| `GET` | `/v1.0/account/{accountId}` | ✅ JWT | Any authenticated | Get transactions by account | — |
| `PUT` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Amend transaction | **PENDING only** |
| `DELETE` | `/v1.0/{id}` | ✅ JWT | `developer` scope | Cancel transaction | **PENDING only** |

### Valid Transaction Types: `Deposit`, `Withdrawal`, `Transfer`
### Valid Transaction Statuses: `PENDING`, `SUCCESS`, `FAILED`

---

## 16. Known Gaps & Recommended Next Steps

### High Priority

**1. Add proper unit and integration tests.** The use case explicitly requires JUnit/Mockito unit tests and Spring Integration tests. The current `contextLoads()` tests are the bare minimum and do not validate any business logic.

```java
// Example: Unit test for AccountService
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock AccountRepository accountRepository;
    @Mock AccountMapper accountMapper;
    @InjectMocks AccountService accountService;

    @Test
    void addAccount_duplicateAccountNumber_throwsException() {
        when(accountRepository.findByAccountNumber("NL91ABNA..."))
            .thenReturn(Optional.of(new Account()));
        assertThrows(AccountAlreadyExistsException.class,
            () -> accountService.addAccount(dto));
    }
}
```

**2. TransactionService should verify account existence.** Before creating a transaction, the Transaction Service should call the Account Service via a `FeignClient` or `WebClient` to confirm the `accountId` is valid. Currently, you can create transactions for account IDs that don't exist.

**3. Create a complete `docker-compose.yml` at the project root** covering all 5 services plus MySQL and Keycloak. This would make the entire system startable with a single `docker-compose up`.

### Medium Priority

**4. Replace hardcoded credentials with environment variables** or a proper secrets manager. At minimum, extract credentials from `application.properties` into environment-specific configuration.

**5. Add `@Operation` / `@ApiResponse` Swagger annotations** to `AccountController` and `CustomerController` to match the quality of `TransactionController`.

**6. Align Spring Boot and Spring Cloud versions** across all services to the same release train.

**7. Fix package naming.** Rename `com.cognizant.hospitalmgmt` in the Eureka server and `com.siemens` in the Gateway to `com.sainik.banking.*`.

**8. Remove `spring-boot-starter-data-rest`** from the Account Service unless HATEOAS is intentionally required.

### Future Enhancements (from the use case)

**9. Implement Kafka event streaming.** The dependency and exclusion are already set up. Define topics (e.g., `account-events`, `transaction-events`) and add producers in the write operations and consumers for async side effects (notifications, audit log).

**10. Implement a Spring Cloud Config Server** for centralised configuration management, so all `application.properties` are served from a single Git-backed config repository.

**11. Add credit scoring / fraud detection** as separate microservices that consume events from Kafka — the architecture already supports it.

**12. Separate databases per service** (`accountdb`, `customerdb`, `transactiondb`) for true domain isolation.

---

*Generated based on analysis of the banking-microservices project — February 2026.*
