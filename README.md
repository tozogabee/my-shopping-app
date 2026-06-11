# MyShoppingApp

A microservices-based shopping application built with Spring Boot.

## Services

| Service | Description |
|---|---|
| `booking-service` | Handles bookings |
| `inventory-service` | Manages inventory |
| `billing-service` | Processes billing |

## Requirements

- Java 21
- Maven

## Build

```bash
cd backend
mvn clean install
```

## Run a service

```bash
cd backend/<service-name>
mvn spring-boot:run
```