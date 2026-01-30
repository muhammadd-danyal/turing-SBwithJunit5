# Bug Injection Proposal - SBwithJunit5 Codebase

## Repository Map

### Core Subsystems

#### 1. **Application Bootstrap & Configuration**
- `SbJunit5Application.java` - Main entry point with custom Dotenv loading
- `application.yml` (main/test) - Dual database configuration (MySQL prod, H2 test)
- `.env` file loading - Custom environment variable injection

#### 2. **REST API Layer (Controllers)**
- `EmployeeController.java` - CRUD endpoints with dual-mode pagination
- `HealthCheckController.java` - Service health monitoring endpoint

#### 3. **Business Logic Layer (Services)**
- `EmployeeService.java` - Service interface
- `EmployeeServiceImpl.java` - DTO→Entity conversion, string parsing logic

#### 4. **Data Access Layer (DAOs)**
- `EmployeeDao.java` - DAO interface
- `EmployeeDaoImpl.java` - Exception wrapping, repository delegation

#### 5. **Persistence Layer (Repositories)**
- `EmployeeRepository.java` - Spring Data JPA with custom query methods

#### 6. **Data Models**
- `Employee.java` - JPA entity with auto-generated ID
- `EmployeeDTO.java` - Validation-heavy DTO with regex patterns

#### 7. **Exception Handling**
- `EmployeeNotFound.java` - Custom business exception
- `GlobalExceptionalHandler.java` - Centralized error handling with custom response format

#### 8. **Test Infrastructure**
- Unit tests with `@WebMvcTest`, `@SpringBootTest`, `@DataJpaTest`
- H2 in-memory database with `data.sql` seeding
- MockMvc for controller testing, Mockito for service mocking

### Data Flow
```
HTTP Request → Controller (validation) → Service (DTO→Entity conversion) 
→ DAO (exception wrapping) → Repository (JPA) → Database
```

---

## Bug Candidates (50 Total)

### **B01: Pagination Page Number Underflow**
- **Location**: `EmployeeController.java`, `getEmployees()`, lines 48-52
- **Bug Type**: Boundary condition error
- **Proposed Change**: Remove validation that prevents negative page numbers from being passed to `PageRequest.of()`
- **Trigger Conditions**: Client sends `?page=-1&size=10`
- **Expected Symptom**: `IllegalArgumentException` from Spring Data instead of graceful handling, or unexpected first page return
- **Why Hard**: Negative page numbers might wrap around or throw exceptions deep in Spring Data layer, not at controller level
- **Detection**: Boundary value testing with negative pagination parameters
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B02: Employee ID Collision on Concurrent Saves**
- **Location**: `EmployeeDaoImpl.java`, `saveData()`, line 22
- **Bug Type**: Race condition / concurrency issue
- **Proposed Change**: Add check-then-act pattern: check if ID exists, then save (without synchronization)
- **Trigger Conditions**: Two concurrent POST requests saving new employees simultaneously
- **Expected Symptom**: Duplicate IDs or lost updates due to TOCTOU (Time-Of-Check-Time-Of-Use) bug
- **Why Hard**: Only manifests under high concurrency; single-threaded tests pass fine
- **Detection**: Concurrent integration test with parallel save operations
- **Stealth**: ★★★★★ | **Value**: ★★★★★

### **B03: Response Entity Status Code Mismatch**
- **Location**: `EmployeeController.java`, `updateData()`, line 67
- **Bug Type**: HTTP semantics violation
- **Proposed Change**: Return `HttpStatus.CREATED (201)` for updates instead of `HttpStatus.OK (200)`
- **Trigger Conditions**: Any PUT request to update existing employee
- **Expected Symptom**: Clients expecting 200 for updates get 201, breaking REST conventions and client logic
- **Why Hard**: Both are success codes; clients might not validate specific status codes
- **Detection**: Assert HTTP 200 (not 201) for successful updates
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B04: JPA Entity Detachment After Delete**
- **Location**: `EmployeeServiceImpl.java`, `deleteEmployeeById()`, line 55
- **Bug Type**: State management error
- **Proposed Change**: Call `getDataById()` after delete to "verify" deletion, causing EntityNotFoundException
- **Trigger Conditions**: Delete employee then immediately query in same transaction
- **Expected Symptom**: Unexpected exception or stale entity returned from persistence context
- **Why Hard**: Delete succeeds but subsequent operations in same context behave unexpectedly
- **Detection**: Test delete followed by get in same transaction
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B05: Pagination Size Zero Division**
- **Location**: `EmployeeController.java`, `getEmployees()`, line 50
- **Bug Type**: Division by zero / invalid state
- **Proposed Change**: Remove validation preventing `size=0` from reaching `PageRequest.of()`
- **Trigger Conditions**: Client sends `?page=0&size=0`
- **Expected Symptom**: `IllegalArgumentException` from Spring Data or infinite loop in pagination logic
- **Why Hard**: Edge case rarely tested; size=0 is semantically invalid but not explicitly handled
- **Detection**: Boundary testing with size=0, negative sizes
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B06: Validation Annotation Order Dependency**
- **Location**: `EmployeeDTO.java`, `empName` field, lines 18-20
- **Bug Type**: Validation execution order bug
- **Proposed Change**: Place `@Pattern` before `@NotBlank`, causing pattern to validate null/empty as valid
- **Trigger Conditions**: Submit employee with null or empty name
- **Expected Symptom**: Null/empty names pass validation when they shouldn't
- **Why Hard**: Annotation order shouldn't matter per spec, but implementation may short-circuit
- **Detection**: Test with null and empty name values
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B07: Update Method Missing Null Check**
- **Location**: `EmployeeServiceImpl.java`, `updateData()`, line 34
- **Bug Type**: Null pointer exception
- **Proposed Change**: Remove null check before accessing `employeeDAO.getDataById()` result
- **Trigger Conditions**: Update request for non-existent employee ID
- **Expected Symptom**: `NullPointerException` instead of proper `EmployeeNotFound` exception
- **Why Hard**: Exception handling exists but NPE occurs before custom exception can be thrown
- **Detection**: Integration test updating non-existent employee
- **Stealth**: ★★☆☆☆ | **Value**: ★★★☆☆

### **B08: Delete Without Existence Check**
- **Location**: `EmployeeDaoImpl.java`, `deleteEmployeeById()`, lines 36-38
- **Bug Type**: Silent failure
- **Proposed Change**: Call `repository.deleteById()` without checking if entity exists first
- **Trigger Conditions**: Delete request for non-existent employee ID
- **Expected Symptom**: Returns success (200 OK) even when nothing was deleted
- **Why Hard**: Spring Data's `deleteById()` doesn't throw exception for missing entities
- **Detection**: Assert that delete of non-existent ID returns 404 or error
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B09: Missing @Transactional on Multi-Step Update**
- **Location**: `EmployeeServiceImpl.java`, `updateData()`, line 33
- **Bug Type**: Transaction boundary missing
- **Proposed Change**: Remove `@Transactional` annotation (if present) from update method
- **Trigger Conditions**: Update fails after fetch but before save
- **Expected Symptom**: Partial updates, data inconsistency if exception occurs mid-operation
- **Why Hard**: Works fine in happy path; only fails under error conditions
- **Detection**: Test update with forced exception between fetch and save
- **Stealth**: ★★★★☆ | **Value**: ★★★★★

### **B10: Concurrent Modification During Search**
- **Location**: `EmployeeServiceImpl.java`, `searchEmployees()`, line 67
- **Bug Type**: Race condition
- **Proposed Change**: Add logic that modifies collection while iterating (if caching added)
- **Trigger Conditions**: Concurrent search and update operations
- **Expected Symptom**: `ConcurrentModificationException` or stale search results
- **Why Hard**: Only manifests under concurrent load; timing-dependent
- **Detection**: Load test with concurrent searches and updates
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B11: Exception Message Information Leak**
- **Location**: `GlobalExceptionalHandler.java`, `handleAnyException()`, line 41
- **Bug Type**: Security - information disclosure
- **Proposed Change**: Include full stack trace in response body instead of just logging
- **Trigger Conditions**: Any unhandled runtime exception
- **Expected Symptom**: Sensitive internal paths and implementation details exposed to clients
- **Why Hard**: Helpful during development, security issue in production
- **Detection**: Security audit of error responses
- **Stealth**: ★★★☆☆ | **Value**: ★★★★☆

### **B12: Query Result Stream Not Closed**
- **Location**: `EmployeeDaoImpl.java`, `getAllData()`, line 32
- **Bug Type**: Resource leak
- **Proposed Change**: Use `repository.findAll().stream()` without try-with-resources or explicit close
- **Trigger Conditions**: Multiple calls to getAllData() over time
- **Expected Symptom**: Memory leak, connection pool exhaustion under load
- **Why Hard**: Small leak per call; only noticeable after many requests
- **Detection**: Load test monitoring connection pool and memory usage
- **Stealth**: ★★★★★ | **Value**: ★★★★☆

### **B13: Jackson Deserialization Without Validation**
- **Location**: `EmployeeController.java`, save/update endpoints, lines 61, 66
- **Bug Type**: Validation bypass
- **Proposed Change**: Accept `Employee` entity directly instead of `EmployeeDTO`, bypassing validation
- **Trigger Conditions**: POST/PUT with invalid data that would fail DTO validation
- **Expected Symptom**: Invalid data persisted to database, validation rules bypassed
- **Why Hard**: Looks cleaner to use entity directly; validation silently skipped
- **Detection**: Test with invalid data that should fail validation
- **Stealth**: ★★★★☆ | **Value**: ★★★★★

### **B14: Lazy Loading Exception Outside Transaction**
- **Location**: `EmployeeController.java`, `getEmployee()`, line 72
- **Bug Type**: LazyInitializationException
- **Proposed Change**: Add lazy-loaded collection to Employee entity, access outside transaction
- **Trigger Conditions**: GET request for employee with lazy-loaded associations
- **Expected Symptom**: `LazyInitializationException` when accessing lazy fields in controller
- **Why Hard**: Works if associations are eager; fails only with lazy loading
- **Detection**: Test with lazy-loaded associations accessed in response
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B15: Search Empty String Returns Nothing**
- **Location**: `EmployeeController.java`, `searchEmployees()`, line 32
- **Bug Type**: Edge case mishandling
- **Proposed Change**: Add check that returns empty list if search name is empty/blank
- **Trigger Conditions**: Search with empty string or whitespace-only string
- **Expected Symptom**: Returns no results instead of all employees
- **Why Hard**: Semantically ambiguous what empty search should return
- **Detection**: Test search with "", " ", null
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B16: Email Validation Allows Invalid TLDs**
- **Location**: `EmployeeDTO.java`, `empEmail` pattern, line 38
- **Bug Type**: Validation gap
- **Proposed Change**: Regex allows single-character TLDs: `^[a-z0-9+.]+@[a-z]+[.]+[a-z]+$`
- **Trigger Conditions**: Email like "test@domain.x" (single char TLD)
- **Expected Symptom**: Invalid emails pass validation
- **Why Hard**: Regex looks correct at glance; single-char TLDs are rare but invalid
- **Detection**: Test with invalid TLD formats
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B17: Update Ignores Partial DTO Fields**
- **Location**: `EmployeeServiceImpl.java`, `updateData()`, lines 35-40
- **Bug Type**: Data loss
- **Proposed Change**: Only update non-null DTO fields, but treat null as "don't update"
- **Trigger Conditions**: Partial update request with some null fields
- **Expected Symptom**: Null fields in DTO erase existing data instead of being ignored
- **Why Hard**: Ambiguous whether null means "set to null" or "don't change"
- **Detection**: Test partial updates with null fields
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B18: Health Check Doesn't Verify Database**
- **Location**: `HealthCheckController.java`, `healthCheck()`, lines 13-17
- **Bug Type**: Observability gap
- **Proposed Change**: Return "UP" even when database is unreachable
- **Trigger Conditions**: Database connection lost but application still running
- **Expected Symptom**: Health endpoint reports UP while actual operations fail
- **Why Hard**: Health check doesn't test dependencies, only application process
- **Detection**: Integration test with database stopped
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B19: Contact Number Leading Zeros Lost**
- **Location**: `EmployeeServiceImpl.java`, `saveData()`, line 25
- **Bug Type**: Data corruption
- **Proposed Change**: Parse string to long loses leading zeros (already happens, but make it worse by trimming)
- **Trigger Conditions**: Contact numbers starting with 0 (like international format)
- **Expected Symptom**: "0123456789" becomes "123456789" (9 digits)
- **Why Hard**: Validation checks 10 chars in string, but long storage loses leading zeros
- **Detection**: Test with contact numbers starting with 0
- **Stealth**: ★★★★☆ | **Value**: ★★★☆☆

### **B20: Content-Type Not Validated**
- **Location**: `EmployeeController.java`, POST/PUT endpoints, lines 60, 65
- **Bug Type**: Input validation gap
- **Proposed Change**: Remove `consumes = MediaType.APPLICATION_JSON` from `@PostMapping/@PutMapping`
- **Trigger Conditions**: POST/PUT with wrong Content-Type (text/plain, form-data)
- **Expected Symptom**: Accepts non-JSON payloads, deserialization fails with cryptic errors
- **Why Hard**: Most clients send correct Content-Type; edge case with misconfigured clients
- **Detection**: Test with various Content-Type headers
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B21: Exception Handler Swallows Cause**
- **Location**: `GlobalExceptionalHandler.java`, `handleAnyException()`, line 38
- **Bug Type**: Observability loss
- **Proposed Change**: Log only message, not the exception object (lose stack trace)
- **Trigger Conditions**: Any unhandled exception
- **Expected Symptom**: Logs show error message but no stack trace for debugging
- **Why Hard**: Error is logged, but missing critical debugging information
- **Detection**: Code review of logging practices
- **Stealth**: ★★★☆☆ | **Value**: ★★★★☆

### **B22: Search Pagination Ignores Page Parameter**
- **Location**: `EmployeeController.java`, `searchEmployees()`, line 38
- **Bug Type**: Logic error
- **Proposed Change**: Always use page 0 regardless of requested page number
- **Trigger Conditions**: Search with pagination, request page > 0
- **Expected Symptom**: Always returns first page of search results
- **Why Hard**: First page works fine; only noticed when trying to navigate
- **Detection**: Test search pagination with page=1,2,3
- **Stealth**: ★★★★☆ | **Value**: ★★★☆☆

### **B23: Salary Validation Allows Negative**
- **Location**: `EmployeeDTO.java`, `empSalary` pattern, line 31
- **Bug Type**: Validation gap
- **Proposed Change**: Regex allows negative sign: `^-?[0-9]{1,9}+[.]{1}+[0-9]{2}+$`
- **Trigger Conditions**: Salary like "-50000.00"
- **Expected Symptom**: Negative salaries pass validation and stored
- **Why Hard**: Regex validates format but not business rule (salary must be positive)
- **Detection**: Test with negative salary values
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B24: Date Parsing Accepts Invalid Dates**
- **Location**: `EmployeeDTO.java`, `empDOB` field, line 35
- **Bug Type**: Validation gap
- **Proposed Change**: `@JsonFormat` accepts dates like "32-13-2020" (invalid day/month)
- **Trigger Conditions**: DOB with day > 31 or month > 12
- **Expected Symptom**: Invalid dates either wrap around or throw exception during parsing
- **Why Hard**: Date validation is lenient by default
- **Detection**: Test with invalid dates (32nd day, 13th month)
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B25: Concurrent Save Race Condition**
- **Location**: `EmployeeDaoImpl.java`, `saveData()`, line 22
- **Bug Type**: Race condition
- **Proposed Change**: Check if employee exists, then save (TOCTOU bug)
- **Trigger Conditions**: Two concurrent saves of same employee
- **Expected Symptom**: Duplicate entries or lost updates
- **Why Hard**: Only manifests under concurrent load
- **Detection**: Concurrent integration test with parallel saves
- **Stealth**: ★★★★★ | **Value**: ★★★★☆

### **B26: Name Validation Allows Only Spaces**
- **Location**: `EmployeeDTO.java`, `empName` pattern, line 20
- **Bug Type**: Validation gap
- **Proposed Change**: Regex `^[a-zA-Z\\s]+$` allows "    " (only spaces)
- **Trigger Conditions**: Name with only whitespace characters
- **Expected Symptom**: Employees with blank names stored
- **Why Hard**: Passes regex but fails business logic (name must have letters)
- **Detection**: Test with whitespace-only names
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B27: Delete Returns Success for Invalid ID**
- **Location**: `EmployeeController.java`, `deleteEmployeeById()`, lines 76-78
- **Bug Type**: Silent failure
- **Proposed Change**: Return 200 OK even if employee doesn't exist
- **Trigger Conditions**: Delete non-existent employee ID
- **Expected Symptom**: Success message but nothing deleted
- **Why Hard**: No error thrown, appears successful
- **Detection**: Assert 404 when deleting non-existent ID
- **Stealth**: ★★★★☆ | **Value**: ★★★☆☆

### **B28: Address Validation Too Permissive**
- **Location**: `EmployeeDTO.java`, `empAddress` pattern, line 24
- **Bug Type**: Validation gap
- **Proposed Change**: Regex allows addresses with only special characters "+++===..."
- **Trigger Conditions**: Address like "++++++++++++++++++"
- **Expected Symptom**: Meaningless addresses pass validation
- **Why Hard**: Technically matches pattern but semantically invalid
- **Detection**: Test with special-character-only addresses
- **Stealth**: ★★★☆☆ | **Value**: ★★☆☆☆

### **B29: Search Returns Duplicate Results**
- **Location**: `EmployeeDaoImpl.java`, `searchEmployees()`, line 47
- **Bug Type**: Data integrity issue
- **Proposed Change**: Don't use DISTINCT in query, return duplicates if joins added later
- **Trigger Conditions**: Search query that could produce duplicates
- **Expected Symptom**: Same employee appears multiple times in results
- **Why Hard**: Only manifests if data model changes or joins added
- **Detection**: Test for duplicate IDs in search results
- **Stealth**: ★★★☆☆ | **Value**: ★★☆☆☆

### **B30: Update Doesn't Validate ID Mismatch**
- **Location**: `EmployeeController.java`, `updateData()`, line 66
- **Bug Type**: Logic error
- **Proposed Change**: Accept empId in path and DTO body, don't validate they match
- **Trigger Conditions**: Update request with mismatched IDs (path: 5, body: 10)
- **Expected Symptom**: Updates wrong employee or confusing behavior
- **Why Hard**: Both IDs present, unclear which should take precedence
- **Detection**: Test with mismatched path and body IDs
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B31: GetById Returns Stale Cache**
- **Location**: `EmployeeServiceImpl.java`, `getDataById()`, line 45
- **Bug Type**: Caching staleness
- **Proposed Change**: Add `@Cacheable` without proper invalidation on updates
- **Trigger Conditions**: Get employee, update it, get again
- **Expected Symptom**: Second get returns old data
- **Why Hard**: Caching improves performance but causes staleness
- **Detection**: Test get-update-get sequence
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B32: HTTP Method Not Restricted**
- **Location**: `EmployeeController.java`, `deleteEmployeeById()`, line 75
- **Bug Type**: HTTP semantics violation
- **Proposed Change**: Use `@RequestMapping` instead of `@DeleteMapping`, accepting all HTTP methods
- **Trigger Conditions**: GET/POST to delete endpoint instead of DELETE
- **Expected Symptom**: Deletion via GET request (CSRF risk, caching issues, accidental deletes)
- **Why Hard**: Functionally works but violates REST principles and creates security risks
- **Detection**: Test DELETE endpoint with GET/POST methods
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B33: Empty List Pagination Metadata Wrong**
- **Location**: `EmployeeDaoImpl.java`, `getAllData(Pageable)`, line 42
- **Bug Type**: Edge case error
- **Proposed Change**: Return page with wrong metadata when result set is empty
- **Trigger Conditions**: Paginated request when no employees exist
- **Expected Symptom**: Page shows hasNext=true but no content
- **Why Hard**: Empty state not well tested
- **Detection**: Test pagination with empty database
- **Stealth**: ★★★☆☆ | **Value**: ★★☆☆☆

### **B34: Contact Number Validation Allows Letters**
- **Location**: `EmployeeDTO.java`, `empContactNumber` pattern, line 28
- **Bug Type**: Validation gap
- **Proposed Change**: Regex becomes `^[0-9a-z]+$` allowing letters
- **Trigger Conditions**: Contact number like "12345abcde"
- **Expected Symptom**: Validation passes but parsing fails with NumberFormatException
- **Why Hard**: Validation and parsing are separate; inconsistent
- **Detection**: Test with alphanumeric contact numbers
- **Stealth**: ★★☆☆☆ | **Value**: ★★☆☆☆

### **B35: CORS Configuration Missing**
- **Location**: `EmployeeController.java` or global config
- **Bug Type**: Security/functionality gap
- **Proposed Change**: No `@CrossOrigin` annotation or CORS configuration
- **Trigger Conditions**: Frontend app on different origin tries to call API
- **Expected Symptom**: Browser blocks requests with CORS errors
- **Why Hard**: Works in same-origin testing; fails only with real frontend deployment
- **Detection**: Test API calls from different origin (localhost:3000 → localhost:8080)
- **Stealth**: ★★★☆☆ | **Value**: ★★★★☆

### **B36: Update Timestamp Not Updated**
- **Location**: `Employee.java`, entity class
- **Bug Type**: Audit trail gap
- **Proposed Change**: No `@LastModifiedDate` field, updates not tracked
- **Trigger Conditions**: Any update operation
- **Expected Symptom**: Can't determine when employee was last modified
- **Why Hard**: Feature gap, not a bug per se, but missing important metadata
- **Detection**: Audit requirements review
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B37: Dotenv File Path Hardcoded**
- **Location**: `SbJunit5Application.java`, `main()`, line 21
- **Bug Type**: Configuration brittleness
- **Proposed Change**: Hardcode `.env` path without fallback mechanism
- **Trigger Conditions**: Running from different working directory
- **Expected Symptom**: Application fails to start, can't find .env file
- **Why Hard**: Works in IDE/standard setup, fails in containers or CI
- **Detection**: Test startup from different working directories
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B38: Exception Handler Order Wrong**
- **Location**: `GlobalExceptionalHandler.java`, exception handlers
- **Bug Type**: Exception handling precedence
- **Proposed Change**: Put `RuntimeException` handler before specific handlers
- **Trigger Conditions**: Any specific exception (EmployeeNotFound, validation)
- **Expected Symptom**: Generic handler catches all, specific handlers never invoked
- **Why Hard**: All exceptions handled, but with wrong response format
- **Detection**: Test that specific exceptions return specific formats
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B39: Save Doesn't Return Generated ID**
- **Location**: `EmployeeController.java`, `saveData()`, line 62
- **Bug Type**: API contract issue
- **Proposed Change**: Return employee without ID populated
- **Trigger Conditions**: Any save operation
- **Expected Symptom**: Client can't reference newly created employee
- **Why Hard**: Save succeeds, but client missing critical information
- **Detection**: Assert response contains valid ID after save
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B40: Pagination Size Exceeds Max**
- **Location**: `EmployeeController.java`, `getEmployees()`, line 50
- **Bug Type**: Resource exhaustion
- **Proposed Change**: Allow unlimited page size (no max validation)
- **Trigger Conditions**: Request with size=1000000
- **Expected Symptom**: OutOfMemoryError or severe performance degradation
- **Why Hard**: Works fine with reasonable sizes, DoS with large sizes
- **Detection**: Load test with extreme page sizes
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B41: Exception Loses Original Cause Chain**
- **Location**: `GlobalExceptionalHandler.java`, `handleAnyException()`, line 36
- **Bug Type**: Observability loss
- **Proposed Change**: Create new exception without passing original as cause: `new RuntimeException(message)`
- **Trigger Conditions**: Any unhandled exception
- **Expected Symptom**: Stack trace shows handler location, not original error location
- **Why Hard**: Error is logged but root cause chain is lost, making debugging difficult
- **Detection**: Verify exception cause chain is preserved in logs
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B42: Repository Save Returns Detached Entity**
- **Location**: `EmployeeDaoImpl.java`, `saveData()`, line 22
- **Bug Type**: JPA state management
- **Proposed Change**: Return input entity instead of repository.save() result
- **Trigger Conditions**: Save new employee, then access generated ID
- **Expected Symptom**: ID is null/0 because returned entity is detached, not managed
- **Why Hard**: Looks correct; both are same object reference but different JPA state
- **Detection**: Assert returned entity has generated ID populated
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B43: Validation Error Response Inconsistent**
- **Location**: `GlobalExceptionalHandler.java`, `handleBeanValidation()`, lines 28-32
- **Bug Type**: API inconsistency
- **Proposed Change**: Return different response structure than other errors
- **Trigger Conditions**: Validation failure
- **Expected Symptom**: Client can't parse error response consistently
- **Why Hard**: Both formats valid JSON, but structure differs
- **Detection**: Assert all error responses follow same schema
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B44: Repository Method Name Typo**
- **Location**: `EmployeeRepository.java`, custom query method
- **Bug Type**: Query generation error
- **Proposed Change**: Rename to `findByEmpNameContainingIgnorCase` (missing 'e')
- **Trigger Conditions**: Application startup
- **Expected Symptom**: Spring Data can't parse method name, startup failure
- **Why Hard**: Compile-time success, runtime failure
- **Detection**: Integration test that exercises search
- **Stealth**: ★☆☆☆☆ | **Value**: ★★☆☆☆

### **B45: Transaction Boundary Missing**
- **Location**: `EmployeeServiceImpl.java`, `updateData()`, line 33
- **Bug Type**: Data consistency issue
- **Proposed Change**: Remove `@Transactional` annotation (if present) or don't add it
- **Trigger Conditions**: Update fails partway through
- **Expected Symptom**: Partial updates committed, inconsistent state
- **Why Hard**: Usually succeeds, only fails under error conditions
- **Detection**: Test update with forced exception mid-operation
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B46: Health Check Caches Result**
- **Location**: `HealthCheckController.java`, `healthCheck()`, line 13
- **Bug Type**: Observability staleness
- **Proposed Change**: Cache health check result for 5 minutes
- **Trigger Conditions**: Service degrades after health check
- **Expected Symptom**: Reports UP for 5 minutes after actual failure
- **Why Hard**: Reduces load on health endpoint, but hides real-time status
- **Detection**: Test health check immediately after inducing failure
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

### **B47: Email Validation Case Sensitive**
- **Location**: `EmployeeDTO.java`, `empEmail` pattern, line 38
- **Bug Type**: Validation too strict
- **Proposed Change**: Regex requires lowercase, rejects "John@Example.com"
- **Trigger Conditions**: Email with uppercase letters
- **Expected Symptom**: Valid emails rejected
- **Why Hard**: RFC allows mixed case, but regex enforces lowercase
- **Detection**: Test with mixed-case email addresses
- **Stealth**: ★★★☆☆ | **Value**: ★★☆☆☆

### **B48: Salary Overflow on Large Values**
- **Location**: `EmployeeServiceImpl.java`, `saveData()`, line 26
- **Bug Type**: Numeric overflow
- **Proposed Change**: Allow salary regex to accept 15 digits before decimal
- **Trigger Conditions**: Salary > Double.MAX_VALUE (very large)
- **Expected Symptom**: Infinity or overflow in double storage
- **Why Hard**: Validation passes but storage overflows
- **Detection**: Test with extremely large salary values
- **Stealth**: ★★★★☆ | **Value**: ★★★☆☆

### **B49: Delete Cascade Not Configured**
- **Location**: `Employee.java`, entity relationships (if any added)
- **Bug Type**: Data integrity issue
- **Proposed Change**: Add relationships without cascade delete
- **Trigger Conditions**: Delete employee with related records
- **Expected Symptom**: Foreign key constraint violation
- **Why Hard**: Works until relationships added
- **Detection**: Test delete with related entities
- **Stealth**: ★★★☆☆ | **Value**: ★★★☆☆

### **B50: Pagination Sort Order Unstable**
- **Location**: `EmployeeController.java`, `getEmployees()`, line 51
- **Bug Type**: Non-deterministic ordering
- **Proposed Change**: Use `PageRequest.of(page, size)` without explicit sort
- **Trigger Conditions**: Multiple pages with duplicate sort values
- **Expected Symptom**: Same employee appears on different pages across requests
- **Why Hard**: Database returns rows in arbitrary order without explicit sort
- **Detection**: Test pagination consistency with repeated requests
- **Stealth**: ★★★★☆ | **Value**: ★★★★☆

---

## Top 10 Recommended Bugs (Ranked by Educational Value + Stealth)

1. **B02** - Employee ID collision on concurrent saves: Race condition with TOCTOU bug
2. **B13** - Jackson deserialization bypasses validation: Security vulnerability from accepting wrong type
3. **B09** - Missing @Transactional on multi-step update: Data consistency under failure
4. **B12** - Query result stream not closed: Resource leak causing memory/connection exhaustion
5. **B31** - Stale cache after update: Classic caching invalidation problem
6. **B08** - Delete without existence check: Silent failure pattern
7. **B40** - Pagination size exceeds max: Resource exhaustion / DoS vulnerability
8. **B45** - Missing transaction boundary: Data consistency under failure conditions
9. **B14** - Lazy loading exception outside transaction: JPA state management issue
10. **B42** - Repository save returns detached entity: Subtle JPA entity state bug

---

## Bug Categories Distribution

- **Validation Gaps**: B06, B16, B23, B24, B26, B28, B34 (7 bugs)
- **Numeric Issues**: B05, B19, B48 (3 bugs)
- **Pagination Errors**: B01, B22, B33, B40, B50 (5 bugs)
- **Concurrency/Race Conditions**: B02, B10, B25 (3 bugs)
- **Security Issues**: B11, B13, B32, B35 (4 bugs)
- **Data Integrity**: B08, B17, B27, B29, B45, B49 (6 bugs)
- **JPA/Transaction Issues**: B04, B09, B14, B42 (4 bugs)
- **Resource Leaks**: B12 (1 bug)
- **Caching/Staleness**: B31, B46 (2 bugs)
- **Observability**: B18, B21, B41, B43 (4 bugs)
- **API Contract/HTTP**: B03, B15, B20, B30, B39, B47 (6 bugs)
- **Configuration**: B37, B38, B44 (3 bugs)
- **Edge Cases**: B07, B36 (2 bugs)

**Total**: 50 unique, diverse bugs across core codebase functionality

---

## Implementation Notes

When implementing selected bugs:
1. Each bug should be in a separate file/module to maintain independence
2. Changes should be minimal (1-5 lines typically)
3. Tests that directly verify the buggy behavior should be removed/modified
4. Build and existing tests must continue passing
5. Bugs should be realistic "oops" moments, not obvious sabotage
6. Document each bug location for later verification/removal
