# Ambulance Service Provider

A web-based system for requesting and dispatching ambulance services built with Spring Boot.

## Technologies

- Java 17
- Spring Boot 3.5.3
- Spring Data JPA
- PostgreSQL
- HTML/CSS/JavaScript
- Maven

## Prerequisites

1. Java JDK 17 or higher
2. PostgreSQL 12 or higher
3. Maven 3.6+ (or use included mvnw)

## Running Tests

To run all tests:

```bash
./mvnw test
```

To run a specific test class:

```bash
./mvnw test -Dtest=ServiceHistoryServiceTest
```

To run a specific test method:

```bash
./mvnw test -Dtest=ServiceHistoryServiceTest#testUpdateServiceStatus_ValidTransition
```

## Setup Instructions

### 1. Database Setup

1. Install PostgreSQL if not already installed
2. Create the database:
   ```sql
   CREATE DATABASE ambulance_service;
   ```

### 2. Build and Run the Application

1. Navigate to the project directory
2. Run `mvn clean install`
3. Run `mvn spring-boot:run` to start the application

### 3. Access the Application

1. Open your browser and navigate to `http://localhost:8080/`