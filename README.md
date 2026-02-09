# copilot-cli-springboot-jwt

A secure, production-ready Spring Boot REST API featuring JWT authentication and a layered architecture, with PostgreSQL support for production use. Built entirely using **GitHub Copilot CLI** as the primary development and debugging assistant—from initial scaffolding through security implementation to systematic troubleshooting of Spring Security configurations.

## 🤖 Built with GitHub Copilot CLI

GitHub Copilot CLI served as the primary development assistant throughout the entire lifecycle of this project. It generated the complete layered architecture with controller, service, repository, entity, and DTO layers following Spring Boot best practices. The CLI implemented JWT security with custom filter chains, handled Spring Security configuration challenges, and provided systematic debugging support when authentication endpoints encountered issues. All code adheres to industry standards, with proper separation of concerns, bean validation, and exception handling generated through iterative prompts and refinements.

## Key Features

- **JWT Authentication** - Stateless token-based auth with HS512 signing and BCrypt password hashing
- **RESTful API** - Clean endpoints with proper HTTP semantics and validation
- **Layered Architecture** - Controller, service, repository, entity, and DTO separation
- **Spring Security** - Custom JWT filter chain with role-based access control
- **Multi-Database Support** - PostgreSQL for production, H2 for development
- **Global Exception Handling** - Consistent error responses across all endpoints
- **Bean Validation** - Input validation with custom error messages

## Technologies

- Spring Boot 3.2.0
- Spring Security with JWT (jjwt 0.12.3)
- Spring Data JPA
- PostgreSQL / H2
- Maven
- Java 17

## Running Locally

**Prerequisites:** Java 17 or higher

**Quick Start:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts at `http://localhost:8081` with H2 console at `/h2-console`.

**Endpoints:**
- `POST /api/auth/register` - Create new user
- `POST /api/auth/login` - Authenticate and receive JWT token
- `GET /api/tasks` - Access protected resources (requires JWT)

## Authentication (Postman)

### Register a User

- **Method:** POST
- **URL:** `http://localhost:8080/api/auth/register`
- **Headers:**
    - `Content-Type: application/json`
- **Body (raw → JSON):**
## Authentication (Postman)

### Register a User

- **Method:** POST
- **URL:** `http://localhost:8080/api/auth/register`
- **Headers:**
    - `Content-Type: application/json`
- **Body (raw → JSON):**
```json
{
  "username": "testuser",
  "email": "testuser@example.com",
  "password": "password123"
}

```

For PostgreSQL setup, see `application.properties` configuration.

## Copilot CLI Documentation

Comprehensive documentation on how GitHub Copilot CLI was used throughout development:

- **[COPILOT_WORKFLOW.md](COPILOT_WORKFLOW.md)** - Complete development journey from initial prompt to deployment-ready code, including generation process, iterative refinement, and productivity analysis
- **[COPILOT_DEBUGGING.md](COPILOT_DEBUGGING.md)** - Real debugging sessions with systematic troubleshooting of Spring Security 403 errors and environment-specific adaptations
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Deep dive into layered architecture, JWT authentication flow, security configurations, and design patterns

### 📊 Productivity Metrics

| Task | Without Copilot | With Copilot | Time Saved |
|------|----------------|--------------|------------|
| Project setup | 45 min | 5 min | **89%** |
| Entity/Repository | 30 min | 10 min | **67%** |
| JWT implementation | 90 min | 15 min | **83%** |
| Security config | 60 min | 15 min | **75%** |
| Service/Controller | 60 min | 20 min | **67%** |
| Debugging 403 error | 120 min | 20 min | **83%** |
| Testing setup | 45 min | 20 min | **56%** |
| Documentation | 60 min | 15 min | **75%** |
| **Total** | **8.5 hours** | **2 hours** | **76%** |

## License

MIT
