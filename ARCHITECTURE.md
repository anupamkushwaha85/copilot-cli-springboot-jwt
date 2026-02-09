# Spring Boot Backend Architecture
## JWT Authentication & Task Management System

This document provides a comprehensive overview of the architectural design, layered structure, and component interactions in the Spring Boot backend application.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Layered Architecture](#layered-architecture)
3. [Security Architecture](#security-architecture)
4. [Database Architecture](#database-architecture)
5. [Request Flow](#request-flow)
6. [Component Details](#component-details)
7. [Design Patterns](#design-patterns)
8. [Configuration Management](#configuration-management)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│                  (Postman, Frontend, Mobile)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/HTTPS
                             │ JSON
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                     │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │              Security Filter Chain (JWT)                     │ │
│ │  - JwtAuthenticationFilter                                   │ │
│ │  - Spring Security Configuration                             │ │
│ └────────────────────────┬────────────────────────────────────┘ │
│                          │                                        │
│ ┌────────────────────────▼────────────────────────────────────┐ │
│ │                  Controller Layer                            │ │
│ │  - AuthController                                            │ │
│ │  - TaskController                                            │ │
│ │  (REST Endpoints, Request Validation, Response Mapping)      │ │
│ └────────────────────────┬────────────────────────────────────┘ │
│                          │                                        │
│ ┌────────────────────────▼────────────────────────────────────┐ │
│ │                   Service Layer                              │ │
│ │  - AuthService                                               │ │
│ │  - TaskService                                               │ │
│ │  (Business Logic, Validation, Transaction Management)        │ │
│ └────────────────────────┬────────────────────────────────────┘ │
│                          │                                        │
│ ┌────────────────────────▼────────────────────────────────────┐ │
│ │                  Repository Layer                            │ │
│ │  - UserRepository (JPA)                                      │ │
│ │  - TaskRepository (JPA)                                      │ │
│ │  (Data Access, Query Methods)                                │ │
│ └────────────────────────┬────────────────────────────────────┘ │
└──────────────────────────┼──────────────────────────────────────┘
                           │
                           │ JPA/Hibernate
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Database Layer                              │
│  - PostgreSQL (Production)                                       │
│  - H2 (Development/Testing)                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Key Characteristics

- **Layered Architecture:** Clear separation of concerns
- **Stateless Authentication:** JWT-based, no server-side sessions
- **RESTful API Design:** Resource-oriented endpoints
- **Dependency Injection:** Spring's IoC container
- **Environment-Based Configuration:** Profiles for dev, test, prod

---

## Layered Architecture

### 1. Controller Layer (Presentation)

**Responsibility:** HTTP request handling, response generation, input validation

**Location:** `src/main/java/com/hackathon/app/controller/`

**Components:**
- `AuthController` - Authentication endpoints
- `TaskController` - Task management endpoints

**Key Annotations:**
```java
@RestController      // Marks class as REST controller
@RequestMapping      // Base URL path
@PostMapping         // HTTP POST mapping
@GetMapping          // HTTP GET mapping
@Valid               // Triggers validation
@AuthenticationPrincipal  // Injects authenticated user
```

**Example Structure:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 1. Receive HTTP request
        // 2. Validate input (@Valid triggers DTO validation)
        // 3. Delegate to service layer
        AuthResponse response = authService.register(request);
        // 4. Return HTTP response
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**Responsibilities:**
- ✅ HTTP protocol handling
- ✅ Request/Response serialization (JSON ↔ Java)
- ✅ Input validation
- ✅ HTTP status code management
- ✅ Delegating to service layer
- ❌ Business logic (delegated to service)
- ❌ Data access (delegated to repository)

---

### 2. Service Layer (Business Logic)

**Responsibility:** Core business logic, transaction management, orchestration

**Location:** `src/main/java/com/hackathon/app/service/`

**Components:**
- `AuthService` - User registration, login, token generation
- `TaskService` - Task CRUD operations, user authorization

**Key Annotations:**
```java
@Service             // Marks as service component
@Transactional       // Transaction boundary
@Autowired           // Dependency injection
```

**Example Structure:**
```java
@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    public AuthResponse register(RegisterRequest request) {
        // 1. Business validation
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        
        // 2. Business logic
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 3. Persist data
        user = userRepository.save(user);
        
        // 4. Generate JWT token
        String token = tokenProvider.generateToken(user);
        
        // 5. Build response
        return new AuthResponse(token, "Bearer", user.getUsername(), user.getEmail());
    }
}
```

**Responsibilities:**
- ✅ Business logic implementation
- ✅ Business rule validation
- ✅ Transaction management
- ✅ Orchestrating multiple repository calls
- ✅ Entity ↔ DTO conversion
- ✅ Exception throwing for business errors
- ❌ HTTP concerns (delegated to controller)
- ❌ Direct database queries (delegated to repository)

---

### 3. Repository Layer (Data Access)

**Responsibility:** Database operations, query execution

**Location:** `src/main/java/com/hackathon/app/repository/`

**Components:**
- `UserRepository` - User data access
- `TaskRepository` - Task data access

**Key Annotations:**
```java
@Repository          // Marks as repository component
```

**Example Structure:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Method name query - Spring Data JPA generates SQL
    Optional<User> findByUsername(String username);
    
    // Boolean return for existence check
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Find all tasks for a user
    List<Task> findByUserId(Long userId);
    
    // Find tasks by user and completion status
    List<Task> findByUserIdAndCompleted(Long userId, Boolean completed);
}
```

**Spring Data JPA Magic:**
```java
// No implementation needed! Spring generates at runtime:

findByUsername(String username)
→ SELECT * FROM users WHERE username = ?

findByUserIdAndCompleted(Long userId, Boolean completed)
→ SELECT * FROM tasks WHERE user_id = ? AND completed = ?

existsByEmail(String email)
→ SELECT COUNT(*) > 0 FROM users WHERE email = ?
```

**Inherited Methods from JpaRepository:**
```java
save(entity)           // INSERT or UPDATE
findById(id)           // SELECT by primary key
findAll()              // SELECT all
deleteById(id)         // DELETE by primary key
count()                // SELECT COUNT(*)
existsById(id)         // Check existence
```

**Responsibilities:**
- ✅ CRUD operations
- ✅ Custom query methods
- ✅ Database transaction execution
- ✅ Entity state management (JPA lifecycle)
- ❌ Business logic (delegated to service)
- ❌ Transaction boundaries (managed by service)

---

### 4. Entity Layer (Domain Model)

**Responsibility:** Data model, database mapping, relationships

**Location:** `src/main/java/com/hackathon/app/entity/`

**Components:**
- `User` - User entity with Spring Security integration
- `Task` - Task entity with user relationship

**Key Annotations:**
```java
@Entity              // JPA entity
@Table               // Database table mapping
@Id                  // Primary key
@GeneratedValue      // Auto-increment strategy
@Column              // Column mapping
@OneToMany           // One-to-many relationship
@ManyToOne           // Many-to-one relationship
@JoinColumn          // Foreign key
```

**User Entity:**
```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
    
    // UserDetails implementation for Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return true; }
}
```

**Task Entity:**
```java
@Entity
@Table(name = "tasks")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private Boolean completed = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**Entity Relationships:**
```
User (1) ←→ (N) Task

User.tasks (List<Task>)
  ↓ @OneToMany(mappedBy = "user")
  
Task.user (User)
  ↓ @ManyToOne
  ↓ @JoinColumn(name = "user_id")
  
Database:
  tasks.user_id → users.id (foreign key)
```

**Responsibilities:**
- ✅ Database schema definition
- ✅ Entity relationships
- ✅ Data validation constraints
- ✅ JPA lifecycle callbacks
- ❌ Business logic (delegated to service)

---

### 5. DTO Layer (Data Transfer Objects)

**Responsibility:** API contract definition, input/output models

**Location:** `src/main/java/com/hackathon/app/dto/`

**Components:**
- Request DTOs: `RegisterRequest`, `LoginRequest`, `CreateTaskRequest`, `UpdateTaskRequest`
- Response DTOs: `AuthResponse`, `TaskResponse`

**Why DTOs?**
```
❌ BAD: Expose entities directly in API
@PostMapping("/register")
public User register(@RequestBody User user) {
    // Problems:
    // - Exposes internal structure
    // - Can't control what fields are sent
    // - Security risk (mass assignment)
    // - Tight coupling
}

✅ GOOD: Use DTOs
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // Benefits:
    // - Controlled input
    // - API versioning friendly
    // - Security (only expected fields)
    // - Validation at API boundary
}
```

**Request DTO Example:**
```java
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    // Getters and setters
}
```

**Response DTO Example:**
```java
public class AuthResponse {
    private String token;
    private String type;
    private String username;
    private String email;
    
    // Constructor, getters, setters
    
    // Note: Password is NEVER included in response
}
```

**Validation Annotations:**
```java
@NotNull         // Field must not be null
@NotBlank        // String must not be null, empty, or whitespace
@NotEmpty        // Collection/array must not be empty
@Size(min, max)  // String/collection size constraints
@Email           // Valid email format
@Min / @Max      // Numeric range
@Pattern         // Regex validation
@Past / @Future  // Date validation
```

---

### 6. Exception Layer

**Responsibility:** Error handling, consistent error responses

**Location:** `src/main/java/com/hackathon/app/exception/`

**Components:**
- Custom Exceptions: `ResourceNotFoundException`, `BadRequestException`
- Error Response DTO: `ErrorResponse`
- Global Handler: `GlobalExceptionHandler`

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An error occurred: " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

**Error Response Format:**
```json
{
  "status": 404,
  "message": "Task not found with id: 123",
  "timestamp": "2026-02-09T13:45:00"
}
```

**Benefits:**
- ✅ Consistent error format across all endpoints
- ✅ Centralized error handling
- ✅ Clean controller code (no try-catch blocks)
- ✅ Automatic validation error aggregation

---

## Security Architecture

### JWT Authentication Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /api/auth/login
       │    { username, password }
       ▼
┌─────────────────────────────────────────┐
│     AuthController                       │
│     ├─> AuthService.login()              │
│         ├─> AuthenticationManager        │
│         │   (validates credentials)      │
│         ├─> JwtTokenProvider             │
│         │   (generates token)            │
│         └─> return AuthResponse          │
│             { token, username, email }   │
└──────┬──────────────────────────────────┘
       │
       │ 2. Response with JWT
       ▼
┌─────────────┐
│   Client    │
│ Stores JWT  │
└──────┬──────┘
       │
       │ 3. GET /api/tasks
       │    Header: Authorization: Bearer <JWT>
       ▼
┌─────────────────────────────────────────┐
│  JwtAuthenticationFilter                │
│  ├─> Extract JWT from header            │
│  ├─> Validate JWT (signature, expiry)   │
│  ├─> Extract username from JWT          │
│  ├─> Load UserDetails from DB           │
│  ├─> Set Authentication in SecurityContext
│  └─> Continue filter chain              │
└──────┬──────────────────────────────────┘
       │
       │ 4. Request with authenticated user
       ▼
┌─────────────────────────────────────────┐
│     TaskController                       │
│     @AuthenticationPrincipal UserDetails │
│     ├─> TaskService.getUserTasks()       │
│     └─> Return tasks                     │
└──────┬──────────────────────────────────┘
       │
       │ 5. Response with task list
       ▼
┌─────────────┐
│   Client    │
└─────────────┘
```

### Security Components

#### 1. JwtTokenProvider

**Responsibility:** JWT creation and validation

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    // Generate JWT token
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    // Extract username from token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    // Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**JWT Structure:**
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcwNzQ4MjQwMCwiZXhwIjoxNzA3NTY4ODAwfQ.signature

Header (Base64):
{
  "alg": "HS512"
}

Payload (Base64):
{
  "sub": "testuser",           // username
  "iat": 1707482400,            // issued at
  "exp": 1707568800             // expiration (24 hours)
}

Signature:
HMACSHA512(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

#### 2. JwtAuthenticationFilter

**Responsibility:** Intercept requests, validate JWT, set authentication

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            // Extract JWT from Authorization header
            String jwt = getJwtFromRequest(request);
            
            // Validate and authenticate
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication", ex);
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### 3. CustomUserDetailsService

**Responsibility:** Load user details for authentication

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> 
                    new UsernameNotFoundException("User not found: " + username)
                );
    }
}
```

**Note:** `User` entity implements `UserDetails`, so it can be returned directly.

#### 4. SecurityConfig

**Responsibility:** Security filter chain configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable default authentication
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()    // Public
                .requestMatchers("/h2-console/**").permitAll()  // Dev only
                .anyRequest().authenticated()                    // Protected
            )
            
            // H2 console support
            .headers(headers -> 
                headers.frameOptions(frame -> frame.disable())
            )
            
            // Custom authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT filter before Spring Security's default filter
            .addFilterBefore(
                jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
}
```

**Security Filter Chain Order:**
```
Request → JwtAuthenticationFilter (custom)
       → UsernamePasswordAuthenticationFilter (Spring default)
       → AuthorizationFilter (checks .authenticated())
       → Controller
```

---

## Database Architecture

### Multi-Environment Database Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                    Application Start                          │
│                           │                                   │
│          Active Profile? ─┼─ dev  → H2 (in-memory)          │
│                           ├─ test → H2 (in-memory)          │
│                           └─ prod → PostgreSQL (persistent)  │
└──────────────────────────────────────────────────────────────┘
```

### Database Configuration

#### Default (application.properties)
```properties
# Server
server.port=8081

# JPA Common Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=yourSecretKeyMustBeAtLeast256BitsLongForHS256AlgorithmToWorkProperlyWithJWTTokens
jwt.expiration=86400000

# H2 Database (Development Default)
spring.datasource.url=jdbc:h2:mem:copilotdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

#### Development Profile (application-dev.properties)
```properties
# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:devdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Enable SQL logging
spring.jpa.show-sql=true
logging.level.com.hackathon.app=DEBUG
logging.level.org.springframework.security=DEBUG

# H2 Console
spring.h2.console.enabled=true
```

#### Test Profile (application-test.properties)
```properties
# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Recreate schema for each test
spring.jpa.hibernate.ddl-auto=create-drop

# Disable SQL logging in tests
spring.jpa.show-sql=false
```

#### Production Profile (application-prod.properties)
```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/hackathon_db
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

# Production JPA settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Disable H2 console
spring.h2.console.enabled=false

# Production logging
logging.level.com.hackathon.app=INFO
```

### Database Schema

**Generated by JPA:**

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Tasks table
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes (auto-generated by JPA for unique constraints)
CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
```

### JPA Configuration Options

**hibernate.ddl-auto values:**
```
create       - Drop existing tables and create new ones (data loss!)
create-drop  - Create on start, drop on shutdown (testing only)
update       - Modify schema to match entities (development)
validate     - Only validate schema matches entities (production)
none         - No schema management (use migration tools)
```

**Recommended by Environment:**
- Development: `update` (auto-apply entity changes)
- Testing: `create-drop` (clean state for each test)
- Production: `validate` or `none` (use Flyway/Liquibase)

---

## Request Flow

### Complete Request Flow (Protected Endpoint)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CLIENT REQUEST                                                │
│    POST http://localhost:8081/api/tasks                          │
│    Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...                  │
│    Content-Type: application/json                                │
│    {                                                              │
│      "title": "New Task",                                        │
│      "description": "Task description",                          │
│      "completed": false                                          │
│    }                                                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. SECURITY FILTER CHAIN                                         │
│    ┌──────────────────────────────────────────┐                 │
│    │ JwtAuthenticationFilter                  │                 │
│    │ ├─> Extract JWT from header              │                 │
│    │ ├─> Validate JWT (signature, expiry)     │                 │
│    │ ├─> Extract username: "testuser"         │                 │
│    │ ├─> Load UserDetails from database       │                 │
│    │ └─> Set SecurityContextHolder             │                 │
│    │     .setAuthentication(userDetails)      │                 │
│    └──────────────────────────────────────────┘                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. CONTROLLER LAYER                                              │
│    TaskController.createTask()                                   │
│    ┌──────────────────────────────────────────┐                 │
│    │ @PostMapping                              │                 │
│    │ public ResponseEntity<TaskResponse>       │                 │
│    │ createTask(                               │                 │
│    │   @Valid @RequestBody CreateTaskRequest  │ ← Deserialize   │
│    │   @AuthenticationPrincipal UserDetails   │ ← From Security │
│    │ )                                         │                 │
│    │                                           │                 │
│    │ 1. Input validation (@Valid)             │                 │
│    │ 2. Extract username from UserDetails     │                 │
│    │ 3. Delegate to service                   │                 │
│    └──────────────────────────────────────────┘                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. SERVICE LAYER                                                 │
│    TaskService.createTask(request, username)                     │
│    ┌──────────────────────────────────────────┐                 │
│    │ @Service                                  │                 │
│    │ @Transactional                            │                 │
│    │                                           │                 │
│    │ 1. Load user from database                │                 │
│    │    user = userRepository                  │                 │
│    │           .findByUsername(username)       │                 │
│    │                                           │                 │
│    │ 2. Create Task entity                     │                 │
│    │    task = new Task()                      │                 │
│    │    task.setTitle(request.getTitle())      │                 │
│    │    task.setUser(user)                     │                 │
│    │                                           │                 │
│    │ 3. Save to database                       │                 │
│    │    task = taskRepository.save(task)       │                 │
│    │                                           │                 │
│    │ 4. Convert to DTO                         │                 │
│    │    return new TaskResponse(task)          │                 │
│    └──────────────────────────────────────────┘                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. REPOSITORY LAYER                                              │
│    UserRepository.findByUsername(username)                       │
│    TaskRepository.save(task)                                     │
│    ┌──────────────────────────────────────────┐                 │
│    │ JPA generates SQL:                        │                 │
│    │                                           │                 │
│    │ SELECT * FROM users                       │                 │
│    │ WHERE username = 'testuser'               │                 │
│    │                                           │                 │
│    │ INSERT INTO tasks                         │                 │
│    │ (title, description, completed,           │                 │
│    │  user_id, created_at)                     │                 │
│    │ VALUES (?, ?, ?, ?, ?)                    │                 │
│    └──────────────────────────────────────────┘                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. DATABASE                                                      │
│    PostgreSQL / H2                                               │
│    ┌──────────────────────────────────────────┐                 │
│    │ Execute queries                           │                 │
│    │ Return result sets                        │                 │
│    │ Commit transaction                        │                 │
│    └──────────────────────────────────────────┘                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼ (Response flows back up)
┌─────────────────────────────────────────────────────────────────┐
│ 7. RESPONSE                                                      │
│    HTTP 201 Created                                              │
│    Content-Type: application/json                                │
│    {                                                              │
│      "id": 1,                                                    │
│      "title": "New Task",                                        │
│      "description": "Task description",                          │
│      "completed": false,                                         │
│      "createdAt": "2026-02-09T13:45:00"                          │
│    }                                                              │
└─────────────────────────────────────────────────────────────────┘
```

### Unauthenticated Request Flow

```
Request without JWT Token
       │
       ▼
JwtAuthenticationFilter
       ├─> No token found in Authorization header
       ├─> Skip authentication
       └─> Continue filter chain
       │
       ▼
Spring Security Authorization
       ├─> Check endpoint permissions
       ├─> /api/auth/** → permitAll() → ALLOW
       └─> /api/tasks/** → authenticated() → DENY
       │
       ▼
403 Forbidden Response
```

---

## Design Patterns

### 1. Layered Architecture Pattern

**Separation of Concerns:**
- Presentation → Controller
- Business Logic → Service
- Data Access → Repository
- Domain Model → Entity

**Benefits:**
- ✅ Independent testing of each layer
- ✅ Technology changes isolated to specific layers
- ✅ Clear responsibilities
- ✅ Reusable business logic

### 2. Repository Pattern

**Abstraction over data access:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// Service doesn't know about SQL, JPA, or database details
userRepository.findByUsername("testuser");
```

**Benefits:**
- ✅ Swappable data sources
- ✅ Centralized query logic
- ✅ Testable with mocks

### 3. DTO Pattern

**Separate API model from domain model:**
```java
// Client sends:
RegisterRequest → Controller

// Controller converts:
RegisterRequest → User entity → Service

// Service returns:
User entity → AuthResponse → Controller

// Client receives:
AuthResponse
```

**Benefits:**
- ✅ API stability (internal changes don't break API)
- ✅ Security (control exposed fields)
- ✅ Validation at boundary

### 4. Dependency Injection

**Spring IoC Container manages dependencies:**
```java
@Service
public class TaskService {
    
    @Autowired  // Spring injects implementation
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
}
```

**Benefits:**
- ✅ Loose coupling
- ✅ Easy to mock for testing
- ✅ Centralized configuration

### 5. Filter Chain Pattern

**Security as composable filters:**
```
Request
  → Filter 1 (JWT validation)
  → Filter 2 (Authorization)
  → Filter 3 (...)
  → Controller
```

**Benefits:**
- ✅ Cross-cutting concerns separated
- ✅ Configurable order
- ✅ Reusable filters

### 6. Service Layer Pattern

**Business logic orchestration:**
```java
@Service
public class TaskService {
    // Orchestrates:
    // - Multiple repository calls
    // - Business validation
    // - Transaction management
    // - DTO conversion
}
```

**Benefits:**
- ✅ Reusable business logic
- ✅ Transaction boundaries
- ✅ Testable without HTTP layer

---

## Configuration Management

### Profile-Based Configuration

**Activate Profile:**
```bash
# Command line
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# application.properties
spring.profiles.active=dev

# IntelliJ Run Configuration
Active Profiles: dev
```

**Configuration Hierarchy:**
```
application.properties (base)
  ├─ Common settings (port, jwt config)
  │
  ├─> application-dev.properties (overrides)
  │   └─ Development settings (H2, debug logging)
  │
  ├─> application-test.properties (overrides)
  │   └─ Test settings (H2, create-drop)
  │
  └─> application-prod.properties (overrides)
      └─ Production settings (PostgreSQL, validate)
```

### Environment Variables

**Externalize sensitive configuration:**
```properties
# application-prod.properties
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
jwt.secret=${JWT_SECRET}

# Syntax: ${ENV_VAR:default_value}
```

**Set environment variables:**
```bash
# Linux/Mac
export DB_USERNAME=myuser
export DB_PASSWORD=mypassword
export JWT_SECRET=your-secret-key

# Windows
set DB_USERNAME=myuser
set DB_PASSWORD=mypassword

# Docker
docker run -e DB_USERNAME=myuser -e DB_PASSWORD=mypassword app
```

### Configuration Best Practices

1. **Never commit secrets**
   ```
   ✅ Use environment variables
   ✅ Use external config servers
   ✅ Use secret management (AWS Secrets Manager, HashiCorp Vault)
   ❌ Hardcode passwords in properties files
   ```

2. **Profile-specific settings**
   ```
   Dev:  H2, verbose logging, H2 console
   Test: H2, create-drop, minimal logging
   Prod: PostgreSQL, validate, INFO logging
   ```

3. **Reasonable defaults**
   ```properties
   ${DB_USERNAME:postgres}  # Defaults to 'postgres' if not set
   ```

---

## API Endpoints Summary

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT |

### Protected Endpoints (JWT Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create new task |
| GET | `/api/tasks` | Get all user's tasks |
| GET | `/api/tasks/{id}` | Get specific task |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |

---

## Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.0 | Application framework |
| Java | 17 | Programming language |
| Maven | 3.6+ | Build tool |

### Spring Ecosystem

| Library | Purpose |
|---------|---------|
| spring-boot-starter-web | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | Database access, ORM |
| spring-boot-starter-security | Authentication, authorization |
| spring-boot-starter-validation | Bean validation |

### Security

| Library | Purpose |
|---------|---------|
| jjwt-api | JWT token creation |
| jjwt-impl | JWT implementation |
| jjwt-jackson | JSON serialization for JWT |

### Database

| Database | Purpose |
|----------|---------|
| PostgreSQL | Production database |
| H2 | Development/testing database |

### Testing

| Library | Purpose |
|---------|---------|
| spring-boot-starter-test | JUnit 5, Mockito, AssertJ |
| spring-security-test | Security testing utilities |

---

## Performance Considerations

### 1. Database Connection Pooling

**HikariCP (default in Spring Boot):**
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### 2. Lazy Loading

**Entities use lazy loading by default:**
```java
@ManyToOne(fetch = FetchType.LAZY)  // Only load when accessed
private User user;
```

**Avoid N+1 queries:**
```java
// Bad: N+1 queries
List<Task> tasks = taskRepository.findAll();
tasks.forEach(task -> System.out.println(task.getUser().getUsername()));

// Good: Join fetch
@Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.id = :userId")
List<Task> findByUserIdWithUser(@Param("userId") Long userId);
```

### 3. DTO Projection

**Load only needed fields:**
```java
public interface TaskSummary {
    Long getId();
    String getTitle();
    Boolean getCompleted();
}

List<TaskSummary> findAllProjectedBy();
```

### 4. Stateless Authentication

**No server-side session storage:**
- ✅ Horizontally scalable
- ✅ No session replication needed
- ✅ Reduced memory usage

---

## Security Best Practices

### 1. Password Security
```java
// Never store plain text passwords
String hashedPassword = passwordEncoder.encode(plainPassword);

// Use BCrypt (adaptive hashing)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // Automatic salt, configurable strength
}
```

### 2. JWT Security
```properties
# Use strong, long secret (256+ bits)
jwt.secret=yourSecretKeyMustBeAtLeast256BitsLongForHS256AlgorithmToWorkProperlyWithJWTTokens

# Reasonable expiration
jwt.expiration=86400000  # 24 hours
```

### 3. Input Validation
```java
@NotBlank(message = "Username is required")
@Size(min = 3, max = 20)
private String username;
```

### 4. Authorization Checks
```java
// Verify user owns resource
public TaskResponse getTask(Long taskId, String username) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    
    if (!task.getUser().getUsername().equals(username)) {
        throw new ForbiddenException("Access denied");
    }
    
    return new TaskResponse(task);
}
```

---

## Conclusion

This architecture provides:

✅ **Clear Separation of Concerns** - Each layer has defined responsibilities  
✅ **Security by Design** - JWT authentication, password hashing, authorization  
✅ **Scalability** - Stateless design, connection pooling, lazy loading  
✅ **Maintainability** - Consistent patterns, dependency injection, clean code  
✅ **Testability** - Mockable dependencies, layered testing  
✅ **Flexibility** - Profile-based configuration, multiple database support  

The layered architecture, combined with Spring Boot's conventions and best practices, creates a robust foundation for building production-ready REST APIs.

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-09  
**Project:** Spring Boot Backend Architecture  
**Technology Stack:** Spring Boot 3.2.0, Java 17, PostgreSQL, JWT
