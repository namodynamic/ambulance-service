# Ambulance Service Provider

A web-based system for requesting and dispatching ambulance services built with Spring Boot.

## 🚨 Project Goal

### Primary Objective
To develop a comprehensive web-based system that enables users to request ambulance services while providing administrators with robust tools to manage ambulance dispatch, patient records, and service history.

### Problem Solved
- Streamlines the entire process of requesting and dispatching ambulances
- Reduces emergency response times through efficient dispatch management
- Ensures organized and accessible patient information for better care coordination
- Provides real-time tracking and status updates for all service requests

### Value Proposition
- **For Patients**: Faster emergency response and improved care coordination
- **For Medical Staff**: Centralized patient information and streamlined dispatch process
- **For Administrators**: Comprehensive tools for resource management and service optimization
- **For Healthcare System**: Enhanced efficiency in emergency medical services and resource allocation

## Live Demo

- **Frontend Application**: [Ambulance Service Provider UI](https://ambulance-service-provider-ui.vercel.app/)
- **API Documentation**: Available at `/swagger-ui.html` when running locally

## Technologies

- Java 17
- Spring Boot 3.5.3
- Spring Data JPA
- Spring Security
- JWT Authentication
- PostgreSQL
- Maven

## Features

- Public API endpoints for accessing ambulance information
- Secure authentication and authorization
- RESTful API design
- Swagger/OpenAPI documentation
- Clean, responsive frontend (separate repository)

## Prerequisites

1. Java JDK 17 or higher
2. PostgreSQL 12 or higher
3. Maven 3.6+ (or use included mvnw)
4. Node.js 16+ (for frontend development, if needed)

## Project Structure

```
src/
├── main/
│   ├── java/com/ambulance/ambulance_service/
│   │   ├── config/       # Configuration classes
│   │   ├── controller/   # REST controllers
│   │   ├── model/        # Entity classes
│   │   ├── repository/   # Data repositories
│   │   ├── security/     # Security configuration
│   │   └── service/      # Business logic
│   └── resources/
│       ├── static/       # Static resources (minimal)
│       └── application.properties
└── test/                 # Test classes
```

## Getting Started

### 1. Database Setup

1. Install PostgreSQL if not already installed
2. Create the database:
   ```sql
   CREATE DATABASE ambulance_service;
   ```

### 2. Configuration

Update `application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ambulance_service
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build and Run the Application

```bash
# Build the application
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## API Documentation

When the application is running, you can access:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs (JSON): `http://localhost:8080/v3/api-docs`

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

## Frontend

The frontend is a separate React application. You can access the live version at [Ambulance Service Provider UI](https://ambulance-service-provider-ui.vercel.app/).

## API Endpoints

### Public Endpoints
- `GET /api/ambulances` - Get list of all ambulances
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Authenticate and get JWT token

### Secured Endpoints
(Requires valid JWT token)
- `GET /api/requests` - Get all service requests (ADMIN only)
- `POST /api/requests` - Create a new service request
- `GET /api/requests/{id}` - Get request details

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.