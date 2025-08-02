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

## 🚀 Deployment

This application is configured for deployment on [Render](https://render.com/). Follow these steps to deploy:

### Prerequisites
- A Render account
- GitHub/GitLab account with access to this repository
- (Optional) A custom domain (if you want to use your own domain)

### Deploy to Render

1. **Fork this repository** to your GitHub/GitLab account

2. **Sign in to Render**
   - Go to [Render Dashboard](https://dashboard.render.com/)
   - Click "New +" and select "Blueprint"
   - Connect your GitHub/GitLab account if you haven't already

3. **Configure your repository**
   - Select the forked repository
   - Give your service a name (e.g., "ambulance-service")
   - Select the branch you want to deploy (usually `main` or `master`)
   - Click "Apply"

4. **Configure environment variables**
   - Go to the "Environment" tab in your Render dashboard
   - Add the following environment variables:
     - `SPRING_PROFILES_ACTIVE=production`
     - `JWT_SECRET` (generate a strong secret key)
     - `JWT_EXPIRATION=86400000` (24 hours in milliseconds)
   - The database credentials will be automatically injected by Render

5. **Start the deployment**
   - Click "Save Changes"
   - Go to the "Manual Deploy" tab and click "Deploy latest commit"

6. **Wait for deployment to complete**
   - The build and deployment process will start automatically
   - You can monitor the progress in the logs

7. **Access your application**
   - Once deployed, you'll get a URL like `https://your-service-name.onrender.com`
   - The API will be available at the root path (e.g., `https://your-service-name.onrender.com/api/ambulances`)

### Environment Variables

For local development, create a `.env` file in the root directory with the following variables:

```
DB_URL=jdbc:postgresql://localhost:5432/ambulance_service
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000
```

### Production Configuration

In production (on Render), the following configurations are automatically applied:
- Database is provisioned and managed by Render
- HTTPS is automatically configured
- Automatic deployments on push to the main branch
- Health checks and auto-restart on failure

### Custom Domain (Optional)

To use a custom domain:
1. Go to your service in the Render dashboard
2. Click on "Settings" > "Custom Domains"
3. Add your domain and follow the DNS configuration instructions