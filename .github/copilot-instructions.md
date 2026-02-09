# Copilot Instructions - Spring Boot Hackathon Project

Backend-only application using Spring Boot with PostgreSQL and JWT authentication.

## Build, Test, and Lint Commands

### Build
```bash
./mvnw clean install              # Full build with tests
./mvnw clean package              # Build without install
./mvnw clean package -DskipTests  # Build without running tests
```

### Test
```bash
./mvnw test                              # Run all tests
./mvnw test -Dtest=ClassName             # Run specific test class
./mvnw test -Dtest=ClassName#methodName  # Run specific test method
./mvnw verify                            # Run integration tests
```

### Run Application
```bash
./mvnw spring-boot:run                              # Run with default profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # Run with dev profile
java -jar target/application-name.jar               # Run packaged JAR
```

### Lint/Code Quality
```bash
./mvnw checkstyle:check    # Run Checkstyle (if configured)
./mvnw spotless:check      # Check code formatting (if configured)
./mvnw spotless:apply      # Apply code formatting (if configured)
```

### CI/CD
```bash
# GitHub Actions workflow runs on push/PR:
- Build with Maven
- Run tests
- Check code coverage (if configured)
```

## Project Architecture

### Package Structure
- `controller/` - REST API endpoints and request handling
- `service/` - Business logic layer
- `repository/` - JPA repositories for PostgreSQL data access
- `model/` or `entity/` - JPA entities mapped to PostgreSQL tables
- `dto/` - Data Transfer Objects for API requests/responses
- `config/` - Spring configuration classes (security, database)
- `exception/` - Custom exceptions and global exception handlers
- `security/` - JWT filters, authentication providers, and security config

### Key Design Patterns
- **Service Layer Pattern**: Controllers delegate business logic to services
- **Repository Pattern**: Data access abstracted through Spring Data JPA repositories
- **DTO Pattern**: Separate DTOs from entities to control API exposure
- **Dependency Injection**: Use constructor injection (preferred over field injection)

### Configuration
- `application.properties` or `application.yml` - Main configuration
- `application-{profile}.properties` - Profile-specific configs (dev, test, prod)
- PostgreSQL connection configured via `spring.datasource.*` properties
- JWT secret and expiration configured in application properties
- Environment-specific settings should use profiles, not hardcoded values

## Key Conventions

### Naming Conventions
- **Controllers**: Use `@RestController` with `*Controller` suffix (e.g., `UserController`)
- **Services**: Use `@Service` with `*Service` suffix, define interfaces when needed
- **Repositories**: Extend `JpaRepository<Entity, ID>` with `*Repository` suffix
- **DTOs**: Use descriptive names like `CreateUserRequest`, `UserResponse`
- **Entities**: Use `@Entity` annotation, singular nouns (e.g., `User`, not `Users`)

### REST API Conventions
- Base path mapping: Use `@RequestMapping` on controller class
- HTTP methods: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- Path variables: `@PathVariable` for resource IDs in URL
- Request bodies: `@RequestBody` with DTOs, use `@Valid` for validation
- Response: Return `ResponseEntity<T>` for explicit status codes, or DTOs for standard 200 responses

### Exception Handling
- Use `@RestControllerAdvice` for global exception handling
- Define custom exceptions extending `RuntimeException`
- Map exceptions to appropriate HTTP status codes
- Return consistent error response structure

### Testing Conventions
- Unit tests: Use `@ExtendWith(MockitoExtension.class)`, mock dependencies with `@Mock`
- Integration tests: Use `@SpringBootTest` with `@AutoConfigureMockMvc`
- Repository tests: Use `@DataJpaTest` for JPA-specific tests
- Test naming: `methodName_condition_expectedResult` pattern

### Database (PostgreSQL)
- PostgreSQL connection via Spring Data JPA
- Use JPA entity relationships: `@OneToMany`, `@ManyToOne`, `@ManyToMany`
- Cascade operations should be explicit and justified
- Use `@Transactional` on service methods that modify data
- Lazy loading is default; use `fetch = FetchType.EAGER` only when necessary
- Database migrations: Consider Flyway or Liquibase for schema versioning

### Security (JWT Authentication)
- Spring Security with JWT-based authentication
- `JwtAuthenticationFilter` validates JWT tokens on requests
- `JwtTokenProvider` or `JwtUtils` for token generation and validation
- Public endpoints: `/api/auth/login`, `/api/auth/register` (no JWT required)
- Protected endpoints: Require valid JWT in Authorization header (`Bearer <token>`)
- Use `@PreAuthorize` for role-based access control
- Passwords stored with BCrypt encoding (`BCryptPasswordEncoder`)
- User entity should implement `UserDetails` for Spring Security integration

### Dependency Management
- Dependencies are managed in `pom.xml`
- Required starters: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`
- PostgreSQL driver: `org.postgresql:postgresql`
- JWT library: `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- Keep dependencies up to date with security patches

## GitHub Actions CI

Workflow file: `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Build with Maven
      run: ./mvnw clean install
    - name: Run tests
      run: ./mvnw test
```

### CI Best Practices
- Tests run automatically on push and PR
- Use in-memory H2 database for tests (separate from PostgreSQL)
- Configure test profile in `application-test.properties`
