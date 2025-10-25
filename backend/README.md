# Carbon Credit Marketplace - Backend Services

This directory contains all Java Spring Boot microservices for the Carbon Credit Marketplace platform.

## Microservices Architecture

### Core Services

1. **user-service** - User authentication, registration, KYC management
2. **vehicle-service** - Vehicle registration and OEM API integration
3. **trip-service** - Trip data management and synchronization
4. **carbon-credit-service** - CO2 calculation and carbon credit management
5. **verification-service** - CVA verification workflow
6. **marketplace-service** - Listing management and search
7. **transaction-service** - Transaction orchestration and escrow
8. **payment-service** - Payment gateway integration
9. **certificate-service** - Certificate generation and verification
10. **notification-service** - Multi-channel notifications
11. **admin-service** - Admin dashboard and management
12. **analytics-service** - Analytics and reporting

### Support Modules

- **common** - Shared libraries, utilities, and DTOs
- **config-server** - Spring Cloud Config Server
- **eureka-server** - Service Discovery (Netflix Eureka)
- **api-gateway** - Spring Cloud Gateway

## Technology Stack

- Java 17+
- Spring Boot 3.x
- Spring Cloud
- Maven/Gradle
- PostgreSQL
- Redis
- RabbitMQ
- Docker

## Getting Started

Each service is a separate Spring Boot application with its own README.
