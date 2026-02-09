# GitHub Copilot CLI Debugging Guide for Spring Boot Projects

Real-world debugging sessions demonstrating how to use GitHub Copilot CLI to troubleshoot common Spring Boot issues.

---

## Table of Contents
1. [Spring Security 403 Forbidden Error](#1-spring-security-403-forbidden-error)
2. [JVM Target Version Mismatch](#2-jvm-target-version-mismatch)
3. [PostgreSQL Authentication Failure](#3-postgresql-authentication-failure)
4. [Windows PowerShell Limitations](#4-windows-powershell-limitations)

---

## 1. Spring Security 403 Forbidden Error

### Problem
**Symptom:** POST request to `/api/auth/register` returns `403 Forbidden` even though the endpoint should be publicly accessible.

**Error Message:**
```
Status: 403 Forbidden
```

### Debugging Session with Copilot CLI

**User:** "i tested this register post api and it output 403"

**Copilot Actions:**
1. **Investigated Security Configuration**
   ```bash
   # Copilot examined SecurityConfig.java
   view src/main/java/com/hackathon/app/security/SecurityConfig.java
   ```

2. **Checked Controller Mapping**
   ```bash
   # Verified the endpoint path
   view src/main/java/com/hackathon/app/controller/AuthController.java
   ```

3. **Analyzed JWT Filter Behavior**
   ```bash
   # Checked if filter was blocking requests
   view src/main/java/com/hackathon/app/security/JwtAuthenticationFilter.java
   ```

### Root Cause
Spring Security was applying **default form login** and **HTTP Basic authentication** to all endpoints, which interfered with stateless JWT authentication.

### Solution Applied

**Before:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**After:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())           // ✅ Explicitly disable form login
            .httpBasic(basic -> basic.disable())         // ✅ Explicitly disable HTTP Basic
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()  // ✅ Allow H2 console
                    .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))  // ✅ For H2 console
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

### Key Changes
1. **Disabled Form Login:** `.formLogin(form -> form.disable())`
2. **Disabled HTTP Basic:** `.httpBasic(basic -> basic.disable())`
3. **Disabled CORS:** `.cors(cors -> cors.disable())` - for development/testing
4. **Allowed H2 Console:** Added `/h2-console/**` to permitAll
5. **Disabled Frame Options:** For H2 console iframe support

### Testing
```bash
# Postman Request
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}

# Response: 201 Created ✅
```

### Lessons Learned
- Spring Security 6+ requires **explicit disabling** of default authentication mechanisms
- JWT-based APIs should disable form login and HTTP Basic auth
- Always test public endpoints without authentication headers first
- Check application logs for security filter chain initialization

---

## 2. JVM Target Version Mismatch

### Problem
**Symptom:** Maven build fails with compilation errors about invalid target release.

**Error Message:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
[ERROR] Fatal error compiling: error: invalid target release: 17
```

### Debugging Session with Copilot CLI

**User:** "mvn clean install fails with target release error"

**Copilot Actions:**
1. **Check Java Version**
   ```bash
   java -version
   ```
   Output: `java version "11.0.20"`

2. **Check pom.xml Configuration**
   ```bash
   view pom.xml
   ```
   Found: `<java.version>17</java.version>`

3. **Verify Maven Compiler Plugin**
   ```bash
   grep -n "maven-compiler-plugin" pom.xml
   ```

### Root Cause
**Mismatch:** Project configured for Java 17, but system has Java 11 installed.

### Solution Options

#### Option 1: Install Java 17 (Recommended)
```bash
# Download from https://adoptium.net/
# Or use package manager

# Windows (Chocolatey)
choco install temurin17

# Verify installation
java -version
# Output: openjdk version "17.0.9"
```

#### Option 2: Downgrade Project to Java 11
```xml
<!-- pom.xml -->
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

**Note:** Spring Boot 3.x requires Java 17+. If using Spring Boot 3.2.0, **must use Option 1**.

### Verification
```bash
# Clean and rebuild
./mvnw clean install

# Should now compile successfully
[INFO] BUILD SUCCESS
```

### Lessons Learned
- Always match Java version with Spring Boot version requirements
- Spring Boot 3.x requires Java 17 or higher
- Check both `JAVA_HOME` and `PATH` environment variables
- Use `./mvnw -version` to see Maven's Java version

---

## 3. PostgreSQL Authentication Failure

### Problem
**Symptom:** Application fails to start with database connection error.

**Error Message:**
```
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "postgres"
Caused by: org.postgresql.util.PSQLException: FATAL: no pg_hba.conf entry for host "127.0.0.1"
```

### Debugging Session with Copilot CLI

**User:** "app won't start, postgres connection error"

**Copilot Actions:**
1. **Check Application Properties**
   ```bash
   view src/main/resources/application.properties
   ```

2. **Verify PostgreSQL Service**
   ```bash
   # Windows
   Get-Service postgresql*
   
   # Or check if port 5432 is listening
   netstat -an | findstr 5432
   ```

3. **Test Connection Manually**
   ```bash
   # Using psql
   psql -h localhost -U postgres -d hackathon_db
   ```

### Common Root Causes

#### Cause 1: Wrong Credentials
**application.properties:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hackathon_db
spring.datasource.username=postgres
spring.datasource.password=wrong_password  # ❌ Incorrect
```

**Solution:**
```properties
spring.datasource.username=postgres
spring.datasource.password=your_actual_password  # ✅ Correct password
```

#### Cause 2: Database Doesn't Exist
**Error:** `database "hackathon_db" does not exist`

**Solution:**
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE hackathon_db;

-- Verify
\l
```

#### Cause 3: PostgreSQL Service Not Running
**Windows:**
```powershell
# Check service status
Get-Service postgresql-x64-14

# Start service
Start-Service postgresql-x64-14
```

**Linux/Mac:**
```bash
# Check status
sudo systemctl status postgresql

# Start service
sudo systemctl start postgresql
```

#### Cause 4: pg_hba.conf Restrictions
**Error:** `no pg_hba.conf entry for host`

**Solution:**
1. Locate `pg_hba.conf` (usually in PostgreSQL data directory)
2. Add/modify entry:
   ```conf
   # IPv4 local connections:
   host    all             all             127.0.0.1/32            md5
   ```
3. Reload PostgreSQL:
   ```bash
   # Windows
   pg_ctl reload -D "C:\Program Files\PostgreSQL\14\data"
   
   # Linux
   sudo systemctl reload postgresql
   ```

### Alternative: Use H2 for Development

**Quick Fix:** Switch to H2 in-memory database for local development.

```properties
# Comment out PostgreSQL
#spring.datasource.url=jdbc:postgresql://localhost:5432/hackathon_db
#spring.datasource.username=postgres
#spring.datasource.password=postgres

# Use H2 instead
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Enable H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Access H2 Console:** `http://localhost:8081/h2-console`

### Verification
```bash
# Start application
./mvnw spring-boot:run

# Should see in logs:
HikariPool-1 - Start completed.
Started CopilotHackathonApplication in 3.456 seconds
```

### Lessons Learned
- Test database connection independently before running the app
- Use H2 for quick local development/testing
- Keep production credentials in environment variables, not properties files
- Check PostgreSQL logs: `/var/log/postgresql/` or Windows Event Viewer

---

## 4. Windows PowerShell Limitations

### Problem
**Symptom:** GitHub Copilot CLI commands fail on Windows with PowerShell Core not installed.

**Error Message:**
```
PowerShell 6+ (pwsh) is not available. Please install it from https://aka.ms/powershell
Error: Command failed: pwsh.exe --version
'pwsh.exe' is not recognized as an internal or external command
```

### Debugging Session with Copilot CLI

**User:** "copilot commands not working on windows"

**Copilot Attempted:**
```bash
# Copilot tried to run:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Failed with PowerShell error
```

### Root Cause
GitHub Copilot CLI requires **PowerShell Core (PowerShell 6+)**, but Windows comes with **Windows PowerShell 5.1** by default.

### Solution Options

#### Option 1: Install PowerShell Core (Recommended)

**Method A - MSI Installer:**
1. Download from: https://aka.ms/powershell
2. Run installer: `PowerShell-7.4.x-win-x64.msi`
3. Restart terminal

**Method B - Windows Package Manager:**
```powershell
# Using winget
winget install Microsoft.PowerShell

# Using Chocolatey
choco install powershell-core
```

**Verification:**
```powershell
pwsh --version
# Output: PowerShell 7.4.1
```

#### Option 2: Use Alternative Command Methods

**Use Windows Command Prompt (cmd):**
```cmd
# Instead of PowerShell, open cmd.exe
mvnw.cmd spring-boot:run
mvnw.cmd clean install
mvnw.cmd test
```

**Use Git Bash:**
```bash
# If Git for Windows is installed
./mvnw spring-boot:run
./mvnw clean install
```

**Use IDE Terminal:**
- IntelliJ IDEA: Built-in terminal handles Maven wrapper
- VS Code: Use integrated terminal with cmd.exe or bash

### Windows-Specific Maven Wrapper Usage

**With Windows PowerShell 5.1:**
```powershell
# Requires .\ prefix
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean install
```

**With PowerShell Core (pwsh):**
```powershell
# Works with ./
./mvnw spring-boot:run
./mvnw clean install
```

**With Command Prompt (cmd):**
```cmd
mvnw spring-boot:run
mvnw clean install
```

### Common Windows Path Issues

**Problem:** Maven wrapper not found
```powershell
./mvnw : The term './mvnw' is not recognized
```

**Solution:**
```powershell
# Windows PowerShell 5.1 - use backslash
.\mvnw.cmd clean install

# Or use full path
C:\Users\username\project\mvnw.cmd spring-boot:run
```

### Copilot CLI Workarounds for Windows

**1. Use Python Scripts:**
```python
# create_dirs.py
import os
import pathlib

dirs = [
    "src/test/java/com/hackathon/app/controller",
    "src/test/java/com/hackathon/app/service"
]

for dir_path in dirs:
    pathlib.Path(dir_path).mkdir(parents=True, exist_ok=True)
    print(f"Created: {dir_path}")
```

**2. Use Batch Scripts:**
```batch
@echo off
REM create_dirs.bat

mkdir "src\test\java\com\hackathon\app\controller"
mkdir "src\test\java\com\hackathon\app\service"

echo Directories created successfully
```

**3. Use Java Process API:**
```java
// For complex operations, use Java itself
ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "dir");
Process p = pb.start();
```

### IDE Integration (Best Practice)

**IntelliJ IDEA:**
- Use built-in Maven tool window
- Run configurations handle OS differences automatically
- Terminal uses system default shell

**VS Code:**
- Configure terminal profile in settings:
  ```json
  {
    "terminal.integrated.profiles.windows": {
      "PowerShell Core": {
        "path": "pwsh.exe"
      },
      "Command Prompt": {
        "path": "cmd.exe"
      }
    }
  }
  ```

### Verification Checklist

```powershell
# 1. Check PowerShell version
$PSVersionTable.PSVersion

# 2. Check Java version
java -version

# 3. Check Maven wrapper
.\mvnw.cmd --version

# 4. Test build
.\mvnw.cmd clean install -DskipTests

# 5. Test run
.\mvnw.cmd spring-boot:run
```

### Lessons Learned
- Windows PowerShell 5.1 ≠ PowerShell Core 7.x
- Always use `mvnw.cmd` on Windows for compatibility
- IDEs abstract away OS-specific shell differences
- For CI/CD, use GitHub Actions (Linux) or configure Windows runners properly
- Python/batch scripts are more portable than complex shell commands on Windows

---

## General Debugging Best Practices with Copilot CLI

### 1. Provide Context
**Good:** "POST /api/auth/register returns 403 in Postman"  
**Better:** "POST /api/auth/register returns 403 in Postman, using Spring Boot 3.2.0 with JWT auth, endpoint is configured as permitAll in SecurityConfig"

### 2. Share Error Messages
Always copy full stack traces from:
- Application logs (console output)
- Browser developer console
- API client (Postman, curl) responses

### 3. Check Application State
```bash
# Before asking for help, verify:
- Is the application running?
- Which port? (check application.properties)
- Which profile? (dev, test, prod)
- Database connected?
```

### 4. Incremental Testing
Test components individually:
1. ✅ Application starts
2. ✅ Database connects
3. ✅ Public endpoints work (no auth)
4. ✅ JWT generation works (login)
5. ✅ Protected endpoints work (with JWT)

### 5. Use Copilot for Investigation
```bash
# Let Copilot examine files
view src/main/java/com/hackathon/app/security/SecurityConfig.java

# Let Copilot search for patterns
grep -n "permitAll" src/main/java/**/*.java

# Let Copilot run tests
./mvnw test -Dtest=AuthControllerTest
```

### 6. Understand the Fix
Don't just apply fixes blindly. Ask Copilot:
- "Why did formLogin cause the 403 error?"
- "What does .cors(cors -> cors.disable()) do?"
- "When should I use H2 vs PostgreSQL?"

---

## Quick Reference: Common Spring Boot Issues

| Error | Likely Cause | Quick Fix |
|-------|-------------|-----------|
| 403 Forbidden | Security config blocking | Disable form login, check permitAll |
| 500 Internal Server Error | NullPointerException | Check service/repo injection |
| Connection refused | Database not running | Start PostgreSQL/use H2 |
| Port already in use | Another app on same port | Change server.port |
| ClassNotFoundException | Missing dependency | Update pom.xml, run mvn install |
| Target release invalid | JDK version mismatch | Match Java version to Spring Boot |
| Bean creation failed | Circular dependency | Refactor @Autowired usage |

---

## Additional Resources

- **Spring Boot Docs:** https://docs.spring.io/spring-boot/docs/current/reference/html/
- **Spring Security:** https://docs.spring.io/spring-security/reference/
- **PostgreSQL Setup:** https://www.postgresql.org/docs/
- **PowerShell Core:** https://aka.ms/powershell
- **H2 Database:** https://www.h2database.com/

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-09  
**Maintained by:** GitHub Copilot CLI User Community
