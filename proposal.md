# Bug Injection Proposal for SBwithJunit5

## Repository Map

### Core Subsystems

#### 1. **Application Bootstrap & Configuration**
- `SbJunit5Application.java` - Custom dotenv loading, Spring context initialization
- `application.yml` (main) - MySQL config with env var placeholders
- `application.yml` (test) - H2 in-memory config with deferred initialization
- `data.sql` (test) - Test data seeding

#### 2. **REST API Layer (Controllers)**
- `EmployeeController.java` - 7 endpoints: CRUD, pagination, search (with/without pagination)
- `HealthCheckController.java` - Health check endpoint

#### 3. **Business Logic Layer (Services)**
- `EmployeeService.java` - Interface with 8 methods
- `EmployeeServiceImpl.java` - DTO→Entity conversion, string parsing (Long, Double)

#### 4. **Data Access Layer (DAO)**
- `EmployeeDao.java` - Interface abstracting repository access
- `EmployeeDaoImpl.java` - Exception wrapping, repository delegation

#### 5. **Persistence Layer (Repositories)**
- `EmployeeRepository.java` - JPA repository with custom search methods

#### 6. **Data Models**
- `Employee.java` - JPA entity with auto-increment ID, date formatting
- `EmployeeDTO.java` - Bean validation with complex regex patterns

#### 7. **Exception Handling**
- `EmployeeNotFound.java` - Custom runtime exception
- `GlobalExceptionalHandler.java` - @RestControllerAdvice with 3 handlers

#### 8. **Test Suite**
- Controller tests: MockMvc with @WebMvcTest
- Service tests: @SpringBootTest with H2
- DAO tests: @SpringBootTest with mocked repository
- Repository tests: @DataJpaTest
- DTO validation tests: Manual validator with @Nested/@ParameterizedTest

### Key Data Flows
1. **Create/Update**: Controller → Service (DTO→Entity + parse) → DAO → Repository → DB
2. **Read**: Controller → Service → DAO (exception wrap) → Repository → DB
3. **Search**: Controller (pagination logic) → Service → DAO → Repository (custom query) → DB
4. **Validation**: Controller (@Valid) → Bean Validator → GlobalExceptionalHandler

---

## Bug Candidates (50 Total)

### **B01: Date Parsing Timezone Inconsistency**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, lines 22-29
- **Bug Type**: Timezone handling / Date precision
- **Proposed Change**: When converting EmployeeDTO date to Employee entity, the date is copied directly without timezone normalization. Add implicit timezone conversion that differs between save and update operations.
- **Trigger Conditions**: Users in different timezones saving employees with DOB; manifests when comparing saved vs retrieved dates across timezone boundaries
- **Expected Symptom**: DOB appears off by 1 day for some users; inconsistent between save and update operations
- **Why It's Hard**: Only visible to users in specific timezones; dates look "close enough"; no error thrown
- **Suggested Detection**: Property-based test with multiple timezone contexts; integration test comparing save→retrieve date equality
- **Exercise Value**: High - teaches timezone awareness
- **Stealth**: High - intermittent, location-dependent

---

### **B02: Pagination Boundary Underflow**
- **Location**: `EmployeeController.java`, `getEmployees()` method, lines 44-53
- **Bug Type**: Boundary condition / Off-by-one
- **Proposed Change**: Change default page from 0 to -1 when page parameter is explicitly null (not missing). Causes underflow in PageRequest.
- **Trigger Conditions**: Request to `/employee/pages?page=null` or when page param explicitly set to null in client code
- **Expected Symptom**: Returns empty result or throws obscure exception about negative page index
- **Why It's Hard**: Rare edge case; most clients omit param rather than sending null; error message doesn't point to source
- **Suggested Detection**: Boundary value test with null, negative, and zero page values
- **Exercise Value**: Medium - edge case handling
- **Stealth**: High - rare trigger

---

### **B03: Contact Number Overflow Silent Truncation**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, line 25
- **Bug Type**: Numeric overflow / Data integrity
- **Proposed Change**: Parse contact number as Integer instead of Long, causing overflow for numbers > 2.1B. Wrap in try-catch that silently sets to 0 on overflow.
- **Trigger Conditions**: Contact numbers starting with digits > 2 (common in many countries)
- **Expected Symptom**: Contact number saved as 0 or negative value; no validation error shown to user
- **Why It's Hard**: Passes validation (10-digit string check); only fails during parsing; exception swallowed
- **Suggested Detection**: Test with international phone numbers; property test with various 10-digit combinations
- **Exercise Value**: High - data integrity + silent failure
- **Stealth**: Very High - data-dependent, silent

---

### **B04: Search Case Sensitivity Regression**
- **Location**: `EmployeeController.java`, `searchEmployees()` method, line 32
- **Bug Type**: API contract drift / Regression
- **Proposed Change**: Add `.toLowerCase()` to search name parameter before passing to service, but repository already does case-insensitive search. This creates double-lowercasing that breaks searches for names with mixed case stored data.
- **Trigger Conditions**: Searching for employees when DB has names with uppercase letters
- **Expected Symptom**: Search returns no results even though employee exists; works for all-lowercase searches
- **Why It's Hard**: Appears to work in most test cases (lowercase test data); subtle interaction between layers
- **Suggested Detection**: Test with mixed-case employee names and various search patterns
- **Exercise Value**: Medium - layer interaction bugs
- **Stealth**: Medium - data-dependent

---

### **B05: Exception Handler Priority Inversion**
- **Location**: `GlobalExceptionalHandler.java`, handler methods, lines 18-43
- **Bug Type**: Exception handling / Control flow
- **Proposed Change**: Move RuntimeException handler before EmployeeNotFound handler (or make it catch Exception instead). Since EmployeeNotFound extends RuntimeException, it gets caught by wrong handler.
- **Trigger Conditions**: Any EmployeeNotFound exception thrown
- **Expected Symptom**: Returns 500 INTERNAL_SERVER_ERROR instead of 404 NOT_FOUND; wrong error message format
- **Why It's Hard**: Both handlers return similar JSON; status code difference might be overlooked; works in unit tests that don't check status
- **Suggested Detection**: Integration test verifying exact HTTP status codes for each exception type
- **Exercise Value**: High - exception hierarchy understanding
- **Stealth**: Medium - status code difference subtle

---

### **B06: Salary Precision Loss in Rounding**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, line 26
- **Bug Type**: Numeric precision / Floating point
- **Proposed Change**: After parsing salary as Double, round to 1 decimal place instead of 2. Use `Math.round(salary * 10) / 10.0` instead of keeping full precision.
- **Trigger Conditions**: Salaries with 2 decimal places (e.g., 50000.99)
- **Expected Symptom**: Salary saved as 50001.0 instead of 50000.99; small discrepancies accumulate
- **Why It's Hard**: Validation passes (regex checks format); rounding seems reasonable; small differences overlooked
- **Suggested Detection**: Exact equality test for salary after save→retrieve; property test with various decimal values
- **Exercise Value**: High - floating point precision awareness
- **Stealth**: High - subtle data corruption

---

### **B07: Pagination Page Size Integer Overflow**
- **Location**: `EmployeeController.java`, `searchEmployees()` method, lines 35-36
- **Bug Type**: Resource exhaustion / Integer overflow
- **Proposed Change**: Change pageSize default from 10 to Integer.MAX_VALUE when size is null. Causes massive memory allocation.
- **Trigger Conditions**: Search request without size parameter
- **Expected Symptom**: OutOfMemoryError or extreme slowness when large dataset; works fine with small test data
- **Why It's Hard**: Tests use small datasets; appears as performance issue not logic bug; intermittent based on data size
- **Suggested Detection**: Load test with large dataset; integration test with size limits
- **Exercise Value**: High - resource limits and DoS prevention
- **Stealth**: Very High - data-size dependent

---

### **B08: Email Validation Regex Catastrophic Backtracking**
- **Location**: `EmployeeDTO.java`, empEmail field, line 38
- **Bug Type**: Performance regression / ReDoS
- **Proposed Change**: Change email regex to `^[a-z0-9+.]*@[a-z]+[(.)*]+[a-z]+$` (add * quantifier creating nested quantifiers). Causes exponential backtracking on invalid emails.
- **Trigger Conditions**: Invalid email with many dots/plus signs (e.g., "a++++++++++++++++@test")
- **Expected Symptom**: Request hangs for seconds/minutes; CPU spike; timeout
- **Why It's Hard**: Valid emails process instantly; only malicious/malformed input triggers; looks like network issue
- **Suggested Detection**: Performance test with pathological inputs; timeout monitoring
- **Exercise Value**: Very High - ReDoS vulnerability awareness
- **Stealth**: Very High - input-dependent, appears as performance issue

---

### **B09: Delete Without Existence Check**
- **Location**: `EmployeeDaoImpl.java`, `deleteEmployeeById()` method, lines 36-38
- **Bug Type**: Error handling gap / Silent failure
- **Proposed Change**: Repository.deleteById() doesn't throw exception if ID doesn't exist. Remove any existence check, making delete always return success even for non-existent IDs.
- **Trigger Conditions**: Attempting to delete employee that doesn't exist
- **Expected Symptom**: Returns "Employee Deleted Successfully" even when nothing was deleted; idempotent but misleading
- **Why It's Hard**: Appears to work; no error thrown; audit logs would show discrepancy; tests might not verify actual deletion
- **Suggested Detection**: Test that verifies count before/after delete; test deleting non-existent ID expecting error
- **Exercise Value**: Medium - idempotency vs correctness
- **Stealth**: High - silent success

---

### **B10: Update Partial Field Mutation**
- **Location**: `EmployeeServiceImpl.java`, `updateData()` method, lines 33-42
- **Bug Type**: Data integrity / State mutation
- **Proposed Change**: Comment out the line that sets empContactNumber during update (line 37). Field retains old value instead of updating.
- **Trigger Conditions**: Updating employee with new contact number
- **Expected Symptom**: Contact number doesn't update while other fields do; partial update success
- **Why It's Hard**: Other fields update correctly; might assume it's a client issue; no error thrown
- **Suggested Detection**: Test that verifies all fields updated; integration test comparing before/after state
- **Exercise Value**: Medium - partial update bugs
- **Stealth**: Medium - field-specific

---

### **B11: Dotenv Loading Race Condition**
- **Location**: `SbJunit5Application.java`, `main()` method, lines 21-33
- **Bug Type**: Race condition / Initialization order
- **Proposed Change**: Load dotenv in a separate thread without joining. Environment properties might not be available when Spring context initializes.
- **Trigger Conditions**: Fast startup on multi-core systems; timing-dependent
- **Expected Symptom**: Intermittent startup failures with "Could not resolve placeholder" errors; works most of the time
- **Why It's Hard**: Non-deterministic; works in debug mode (slower); hard to reproduce consistently
- **Suggested Detection**: Stress test with repeated rapid restarts; integration test with timing assertions
- **Exercise Value**: Very High - concurrency and initialization order
- **Stealth**: Very High - timing-dependent, intermittent

---

### **B12: JSON Date Deserialization Locale Dependency**
- **Location**: `Employee.java`, empDOB field, line 32
- **Bug Type**: Locale/Internationalization
- **Proposed Change**: Add `@JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")` hardcoding timezone. Breaks for users in other timezones.
- **Trigger Conditions**: API requests from clients in non-IST timezones
- **Expected Symptom**: Dates off by timezone offset; inconsistent date handling across users
- **Why It's Hard**: Works fine for developers in IST; tests might use same timezone; appears as "user error"
- **Suggested Detection**: Test with multiple timezone contexts; property test with various locales
- **Exercise Value**: High - internationalization awareness
- **Stealth**: High - location-dependent

---

### **B13: Validation Error Message Information Leak**
- **Location**: `GlobalExceptionalHandler.java`, `handleBeanValidation()` method, lines 27-33
- **Bug Type**: Security / Information disclosure
- **Proposed Change**: Add stack trace to validation error response: `map.put("stackTrace", Arrays.toString(exception.getStackTrace()))`. Leaks internal structure.
- **Trigger Conditions**: Any validation failure
- **Expected Symptom**: Detailed stack traces in API responses revealing internal package structure, file paths
- **Why It's Hard**: Helps debugging so seems useful; security implication not obvious; common in dev environments
- **Suggested Detection**: Security audit of error responses; test that validates response structure
- **Exercise Value**: Medium - security awareness
- **Stealth**: Low - visible but seems helpful

---

### **B14: Search Empty String Edge Case**
- **Location**: `EmployeeServiceImpl.java`, `searchEmployees()` method, line 67
- **Bug Type**: Edge case / Unexpected behavior
- **Proposed Change**: Add check: if name is empty string, return empty list instead of delegating to DAO (which returns all employees).
- **Trigger Conditions**: Search with empty string parameter
- **Expected Symptom**: Returns no results instead of all results; inconsistent with "contains" semantics
- **Why It's Hard**: Empty string "contains" all strings is mathematically correct but unintuitive; might seem like a feature
- **Suggested Detection**: Test with empty string, whitespace, null; document expected behavior
- **Exercise Value**: Medium - edge case semantics
- **Stealth**: Medium - interpretation-dependent

---

### **B15: Page Number Type Coercion**
- **Location**: `EmployeeController.java`, `getEmployees()` method, line 49
- **Bug Type**: Type handling / Implicit conversion
- **Proposed Change**: Change page parameter type from Integer to String, parse manually with Integer.parseInt(). Doesn't handle non-numeric strings gracefully.
- **Trigger Conditions**: Request with non-numeric page parameter (e.g., `/employee/pages?page=abc`)
- **Expected Symptom**: NumberFormatException instead of validation error; 500 instead of 400 status
- **Why It's Hard**: Spring normally handles conversion; manual parsing bypasses framework; wrong error type
- **Suggested Detection**: Test with various invalid page values; verify 400 status for bad input
- **Exercise Value**: Medium - framework integration understanding
- **Stealth**: Medium - error type difference

---

### **B16: Repository Method Name Typo**
- **Location**: `EmployeeRepository.java`, custom method, line 14
- **Bug Type**: Typo / Method naming
- **Proposed Change**: Rename method to `findByEmpNameContainingIgnorCase` (missing 'e'). Spring Data JPA won't recognize pattern, falls back to full table scan or fails.
- **Trigger Conditions**: Any search operation
- **Expected Symptom**: Search extremely slow on large datasets or fails at startup with "No property 'ignorCase' found"
- **Why It's Hard**: Might work but slowly; error at startup is clear but typo not obvious; similar to correct spelling
- **Suggested Detection**: Startup integration test; performance test comparing search speed
- **Exercise Value**: Low - simple typo
- **Stealth**: Low - likely fails at startup

---

### **B17: Salary String Parsing Locale Dependency**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, line 26
- **Bug Type**: Locale dependency / Parsing
- **Proposed Change**: Use `Double.parseDouble()` which respects default locale. In some locales, comma is decimal separator, breaking parsing of "50000.00".
- **Trigger Conditions**: Server running with non-US locale (e.g., European locales using comma)
- **Expected Symptom**: NumberFormatException when parsing salary; works in US locale
- **Why It's Hard**: Works in dev environment (US locale); only fails in production with different locale; appears environment-specific
- **Suggested Detection**: Test with multiple locales; explicit locale handling in parsing
- **Exercise Value**: High - locale awareness
- **Stealth**: Very High - environment-dependent

---

### **B18: Concurrent Modification During Pagination**
- **Location**: `EmployeeController.java`, `getEmployees()` method, lines 44-53
- **Bug Type**: Concurrency / Consistency
- **Proposed Change**: No change to code, but bug manifests: pagination doesn't use snapshot isolation. If data changes between page requests, results can be duplicated or skipped.
- **Trigger Conditions**: Multiple users modifying employees while another user paginating through results
- **Expected Symptom**: Same employee appears on multiple pages or missing employees; inconsistent pagination
- **Why It's Hard**: Requires concurrent operations; appears as data inconsistency; hard to reproduce in tests
- **Suggested Detection**: Concurrent integration test with writes during pagination; consistency checks
- **Exercise Value**: Very High - concurrency and isolation levels
- **Stealth**: Very High - timing-dependent, requires load

---

### **B19: Health Check False Positive**
- **Location**: `HealthCheckController.java`, `healthCheck()` method, lines 13-18
- **Bug Type**: Observability / False positive
- **Proposed Change**: Always return "UP" without checking database connectivity or any actual health indicators.
- **Trigger Conditions**: Database down or connection pool exhausted
- **Expected Symptom**: Health check returns UP while application can't serve requests; misleading monitoring
- **Why It's Hard**: Health endpoint works; actual endpoints fail; monitoring shows green while users see errors
- **Suggested Detection**: Integration test that verifies health check fails when DB is down
- **Exercise Value**: High - proper health check implementation
- **Stealth**: High - monitoring blind spot

---

### **B20: DTO Validation Bypass via Null**
- **Location**: `EmployeeController.java`, `saveData()` method, line 61
- **Bug Type**: Validation bypass / Null handling
- **Proposed Change**: Add null check before validation: `if (employeeDTO == null) return ResponseEntity.ok(null);`. Bypasses @Valid annotation.
- **Trigger Conditions**: POST request with null body or empty JSON
- **Expected Symptom**: Accepts invalid data; null pointer exceptions in service layer; inconsistent validation
- **Why It's Hard**: Seems like defensive programming; bypasses framework validation; downstream errors confusing
- **Suggested Detection**: Test with null, empty, and malformed JSON bodies
- **Exercise Value**: Medium - validation framework understanding
- **Stealth**: Medium - appears defensive

---

### **B21: Update Method ID Mismatch**
- **Location**: `EmployeeController.java`, `updateData()` method, line 66
- **Bug Type**: Logic error / ID mismatch
- **Proposed Change**: Use DTO's empId (if it exists) instead of path variable empId. Allows updating wrong employee.
- **Trigger Conditions**: Update request where path ID differs from body ID
- **Expected Symptom**: Updates wrong employee; security issue allowing unauthorized updates
- **Why It's Hard**: Works when IDs match; subtle security vulnerability; might seem like feature to allow ID changes
- **Suggested Detection**: Test with mismatched IDs; security test for unauthorized access
- **Exercise Value**: High - security and API design
- **Stealth**: High - works in normal cases

---

### **B22: Exception Message Injection**
- **Location**: `EmployeeDaoImpl.java`, `getDataById()` method, line 27
- **Bug Type**: Security / Injection
- **Proposed Change**: Change exception message to include empId: `"Employee Not Found: " + empId`. If empId comes from user input, could inject malicious content into logs.
- **Trigger Conditions**: Request with malicious ID containing newlines or control characters
- **Expected Symptom**: Log injection; fake log entries; log parsing breaks
- **Why It's Hard**: Seems like helpful debugging info; log injection often overlooked; requires log analysis to detect
- **Suggested Detection**: Security test with malicious IDs; log format validation
- **Exercise Value**: Medium - log injection awareness
- **Stealth**: High - requires log analysis

---

### **B23: Pagination Total Count Inaccuracy**
- **Location**: `EmployeeController.java`, `getEmployees()` method, lines 44-53
- **Bug Type**: Correctness / Metadata
- **Proposed Change**: Return Page object but manually construct with wrong total count. Set total to current page size * 2.
- **Trigger Conditions**: Any paginated request
- **Expected Symptom**: Page metadata shows wrong total elements; pagination UI shows incorrect page count
- **Why It's Hard**: Data is correct, only metadata wrong; might not be checked; appears as UI bug
- **Suggested Detection**: Test that validates page metadata; compare total with actual count
- **Exercise Value**: Medium - metadata correctness
- **Stealth**: Medium - data correct, metadata wrong

---

### **B24: Search Null Parameter NullPointerException**
- **Location**: `EmployeeController.java`, `searchEmployees()` method, line 26
- **Bug Type**: Null handling / NPE
- **Proposed Change**: Remove @RequestParam required check, allow null name. Call `.toLowerCase()` on null causing NPE.
- **Trigger Conditions**: Search request without name parameter
- **Expected Symptom**: NullPointerException; 500 error instead of 400 validation error
- **Why It's Hard**: Framework usually prevents this; manual handling bypasses protection; wrong error type
- **Suggested Detection**: Test with missing required parameters; verify 400 status
- **Exercise Value**: Low - basic null check
- **Stealth**: Low - obvious NPE

---

### **B25: Double Precision Comparison Bug**
- **Location**: `EmployeeServiceImpl.java`, `updateData()` method, line 38
- **Bug Type**: Floating point comparison
- **Proposed Change**: Add check: only update salary if different using `==` comparison. Due to floating point precision, might always update or never update.
- **Trigger Conditions**: Updating employee with same salary value
- **Expected Symptom**: Unnecessary updates or missed updates; performance impact; audit log noise
- **Why It's Hard**: Floating point equality is tricky; might work for simple values; intermittent based on precision
- **Suggested Detection**: Test with various salary values; epsilon-based comparison
- **Exercise Value**: High - floating point comparison
- **Stealth**: High - precision-dependent

---

### **B26: Repository Transaction Isolation Issue**
- **Location**: `EmployeeDaoImpl.java`, class level
- **Bug Type**: Concurrency / Transaction isolation
- **Proposed Change**: Add `@Transactional(isolation = Isolation.READ_UNCOMMITTED)` to class. Allows dirty reads.
- **Trigger Conditions**: Concurrent transactions; one reading while another writing
- **Expected Symptom**: Reading uncommitted data; phantom reads; data inconsistency
- **Why It's Hard**: Works in single-threaded tests; requires concurrent load; appears as rare data inconsistency
- **Suggested Detection**: Concurrent integration test; transaction isolation test
- **Exercise Value**: Very High - transaction isolation levels
- **Stealth**: Very High - concurrency-dependent

---

### **B27: Email Validation Regex Allows Invalid Domains**
- **Location**: `EmployeeDTO.java`, empEmail field, line 38
- **Bug Type**: Validation weakness
- **Proposed Change**: Change regex to allow single-letter domains: `^[a-z0-9+.]+@[a-z]+[.]+[a-z]+$`. Accepts invalid emails like "test@a.b".
- **Trigger Conditions**: Submitting employee with single-letter domain email
- **Expected Symptom**: Accepts invalid emails; email delivery fails; data quality issue
- **Why It's Hard**: Technically valid format; edge case; might work for some TLDs; seems overly restrictive to reject
- **Suggested Detection**: Test with various invalid email formats; email format validation
- **Exercise Value**: Medium - validation thoroughness
- **Stealth**: Medium - edge case validity

---

### **B28: Delete Operation Cascade Failure**
- **Location**: `EmployeeDaoImpl.java`, `deleteEmployeeById()` method, line 37
- **Bug Type**: Data integrity / Cascade
- **Proposed Change**: If Employee had relationships (future feature), delete doesn't cascade. Orphaned records remain.
- **Trigger Conditions**: Deleting employee with related records (not currently present)
- **Expected Symptom**: Foreign key constraint violation or orphaned data
- **Why It's Hard**: Works now (no relationships); breaks when relationships added; forward compatibility issue
- **Suggested Detection**: Integration test with relationships; referential integrity checks
- **Exercise Value**: Medium - cascade awareness
- **Stealth**: High - future-breaking change

---

### **B29: Date Format Ambiguity (dd-MM vs MM-dd)**
- **Location**: `Employee.java` and `EmployeeDTO.java`, empDOB field
- **Bug Type**: Date format ambiguity
- **Proposed Change**: Change format in Employee to "MM-dd-yyyy" but keep DTO as "dd-MM-yyyy". Inconsistent parsing.
- **Trigger Conditions**: Dates where day and month are both ≤12 (e.g., 05-03-2000)
- **Expected Symptom**: Dates swapped; May 3rd becomes March 5th; only obvious when day >12
- **Why It's Hard**: Works for most dates; only breaks on specific values; appears as user input error
- **Suggested Detection**: Test with dates where day >12; consistency test across layers
- **Exercise Value**: High - format consistency
- **Stealth**: Very High - data-dependent

---

### **B30: Search Performance Degradation**
- **Location**: `EmployeeRepository.java`, search method, line 14
- **Bug Type**: Performance regression / N+1
- **Proposed Change**: Add `@EntityGraph` or lazy loading that causes N+1 query problem when fetching search results.
- **Trigger Conditions**: Search returning multiple results
- **Expected Symptom**: Slow search performance; database query explosion; works fast with 1 result
- **Why It's Hard**: Works correctly; only performance impact; scales poorly; not caught by functional tests
- **Suggested Detection**: Performance test with large result sets; query count monitoring
- **Exercise Value**: High - N+1 query awareness
- **Stealth**: High - performance not correctness

---

### **B31: Validation Order Dependency**
- **Location**: `EmployeeDTO.java`, field validation annotations
- **Bug Type**: Validation order / Short-circuit
- **Proposed Change**: Reorder annotations so @Pattern runs before @Size. For invalid input, wrong error message shown first.
- **Trigger Conditions**: Input that violates multiple constraints
- **Expected Symptom**: Confusing error messages; pattern error for size issue; poor UX
- **Why It's Hard**: All validations eventually fail; order seems arbitrary; UX issue not logic bug
- **Suggested Detection**: Test that validates error message priority; UX testing
- **Exercise Value**: Low - validation order
- **Stealth**: Low - UX issue

---

### **B32: Controller Response Status Inconsistency**
- **Location**: `EmployeeController.java`, `updateData()` method, line 67
- **Bug Type**: API contract / HTTP semantics
- **Proposed Change**: Return HttpStatus.CREATED (201) for updates. Should be OK (200) or NO_CONTENT (204).
- **Trigger Conditions**: Any update operation
- **Expected Symptom**: Wrong HTTP status; confuses REST clients; breaks API contracts
- **Why It's Hard**: Response body correct; status code overlooked; works functionally; semantic issue
- **Suggested Detection**: API contract test; HTTP status validation
- **Exercise Value**: Medium - REST semantics
- **Stealth**: Medium - functional but semantically wrong

---

### **B33: Salary Validation Regex Decimal Places**
- **Location**: `EmployeeDTO.java`, empSalary field, line 31
- **Bug Type**: Validation logic / Edge case
- **Proposed Change**: Change regex to require exactly 2 decimal places but allow leading zeros: `^[0-9]+[.][0-9]{2}$`. Rejects valid salaries like "50000.50" with leading zeros in integer part.
- **Trigger Conditions**: Salaries with certain patterns
- **Expected Symptom**: Valid salaries rejected; inconsistent validation; user frustration
- **Why It's Hard**: Works for most inputs; edge case; regex complexity; seems correct at glance
- **Suggested Detection**: Property test with various salary formats; boundary value testing
- **Exercise Value**: Medium - regex correctness
- **Stealth**: Medium - pattern-dependent

---

### **B34: Exception Handler Logging Side Effect**
- **Location**: `GlobalExceptionalHandler.java`, `handleAnyException()` method, line 41
- **Bug Type**: Performance / Side effect
- **Proposed Change**: Log full stack trace array as objects causing massive log output. Use `log.info("Stack Trace", (Object[]) runtimeException.getStackTrace())`.
- **Trigger Conditions**: Any runtime exception
- **Expected Symptom**: Massive log files; disk space exhaustion; log aggregation costs; performance impact
- **Why It's Hard**: Seems like good debugging practice; impact only visible at scale; gradual degradation
- **Suggested Detection**: Log volume monitoring; performance test with exceptions
- **Exercise Value**: Medium - logging best practices
- **Stealth**: High - gradual impact

---

### **B35: Contact Number Leading Zero Loss**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, line 25
- **Bug Type**: Data integrity / Numeric conversion
- **Proposed Change**: Parse as Long which drops leading zeros. Phone numbers like "0123456789" become "123456789".
- **Trigger Conditions**: Contact numbers starting with 0 (common in many countries)
- **Expected Symptom**: Leading zeros lost; phone numbers invalid; data corruption
- **Why It's Hard**: Validation passes (10 digits); only visible after save; appears as display issue; country-specific
- **Suggested Detection**: Test with leading zero numbers; round-trip equality test
- **Exercise Value**: High - numeric vs string data
- **Stealth**: High - data-dependent, country-specific

---

### **B36: Pagination Size Limit Bypass**
- **Location**: `EmployeeController.java`, `getEmployees()` method, line 50
- **Bug Type**: Resource exhaustion / Validation bypass
- **Proposed Change**: No maximum size validation. User can request size=1000000 causing memory exhaustion.
- **Trigger Conditions**: Request with very large size parameter
- **Expected Symptom**: OutOfMemoryError; application crash; DoS vulnerability
- **Why It's Hard**: Works with reasonable sizes; no built-in limit; appears as performance issue; security implication
- **Suggested Detection**: Load test with large sizes; resource limit validation
- **Exercise Value**: High - DoS prevention
- **Stealth**: High - requires malicious/accidental large input

---

### **B37: Update Optimistic Locking Missing**
- **Location**: `Employee.java`, entity class
- **Bug Type**: Concurrency / Lost update
- **Proposed Change**: No @Version field for optimistic locking. Concurrent updates cause lost updates (last write wins).
- **Trigger Conditions**: Two users updating same employee simultaneously
- **Expected Symptom**: One update silently lost; no conflict detection; data inconsistency
- **Why It's Hard**: Works in single-user scenarios; requires concurrent updates; appears as user error; no error thrown
- **Suggested Detection**: Concurrent update test; optimistic locking test
- **Exercise Value**: Very High - optimistic locking
- **Stealth**: Very High - concurrency-dependent

---

### **B38: Search Injection via Name Parameter**
- **Location**: `EmployeeRepository.java`, search method
- **Bug Type**: Security / Query injection
- **Proposed Change**: If using @Query with string concatenation instead of method naming: `@Query("SELECT e FROM Employee e WHERE e.empName LIKE '%" + name + "%'")`. Allows JPQL injection.
- **Trigger Conditions**: Search with malicious name parameter containing JPQL syntax
- **Expected Symptom**: Unauthorized data access; query manipulation; security breach
- **Why It's Hard**: Works for normal input; security issue not obvious; requires security testing; rare in Spring Data
- **Suggested Detection**: Security test with injection payloads; parameterized query validation
- **Exercise Value**: High - injection awareness
- **Stealth**: High - requires security knowledge

---

### **B39: DTO to Entity Null Field Propagation**
- **Location**: `EmployeeServiceImpl.java`, `saveData()` method, lines 22-29
- **Bug Type**: Null handling / Data integrity
- **Proposed Change**: Don't validate DTO fields before conversion. Null fields in DTO propagate to entity causing constraint violations later.
- **Trigger Conditions**: DTO with null fields that pass @Valid (e.g., optional fields)
- **Expected Symptom**: Database constraint violation; cryptic error messages; wrong error layer
- **Why It's Hard**: Validation should prevent this; edge cases slip through; error at wrong layer; confusing stack trace
- **Suggested Detection**: Test with null/missing fields; constraint violation handling
- **Exercise Value**: Medium - validation coverage
- **Stealth**: Medium - validation gap

---

### **B40: Dotenv File Missing Graceful Degradation**
- **Location**: `SbJunit5Application.java`, `main()` method, line 21
- **Bug Type**: Error handling / Startup failure
- **Proposed Change**: Dotenv.load() throws exception if .env missing. No try-catch, application fails to start even with env vars set elsewhere.
- **Trigger Conditions**: Running without .env file (e.g., production with system env vars)
- **Expected Symptom**: Application won't start; works in dev (has .env); fails in production; misleading error
- **Why It's Hard**: Works in dev environment; production uses different config method; appears as deployment issue
- **Suggested Detection**: Test startup without .env file; graceful degradation test
- **Exercise Value**: High - configuration flexibility
- **Stealth**: High - environment-dependent

---

### **B41: JSON Serialization Infinite Recursion**
- **Location**: `Employee.java`, entity class
- **Bug Type**: Serialization / Stack overflow
- **Proposed Change**: If Employee had bidirectional relationship (e.g., with Department), missing @JsonManagedReference/@JsonBackReference causes infinite recursion during serialization.
- **Trigger Conditions**: GET request for employee with relationships
- **Expected Symptom**: StackOverflowError; request timeout; 500 error
- **Why It's Hard**: Works without relationships; breaks when relationships added; common mistake; cryptic error
- **Suggested Detection**: Integration test with entity relationships; serialization test
- **Exercise Value**: High - JPA relationship serialization
- **Stealth**: High - relationship-dependent

---

### **B42: Validation Group Missing**
- **Location**: `EmployeeDTO.java`, validation annotations
- **Bug Type**: Validation logic / Context-dependent
- **Proposed Change**: Use validation groups but don't specify group in controller @Valid. Some validations skipped.
- **Trigger Conditions**: Create vs update operations with different validation requirements
- **Expected Symptom**: Wrong validations applied; accepts invalid data in some contexts; inconsistent behavior
- **Why It's Hard**: Some validations work; context-dependent; appears as validation bug; group concept not obvious
- **Suggested Detection**: Test create vs update with different validation requirements
- **Exercise Value**: Medium - validation groups
- **Stealth**: High - context-dependent

---

### **B43: Repository Method Return Type Mismatch**
- **Location**: `EmployeeRepository.java`, custom methods
- **Bug Type**: Type safety / Null handling
- **Proposed Change**: Change return type from `List<Employee>` to `Employee` for search method. Returns null or throws exception when multiple results.
- **Trigger Conditions**: Search returning multiple results
- **Expected Symptom**: Exception or null instead of list; breaks API contract; type confusion
- **Why It's Hard**: Works when single result; breaks with multiple; type system should prevent but Spring Data allows
- **Suggested Detection**: Test with queries returning 0, 1, and multiple results
- **Exercise Value**: Medium - return type semantics
- **Stealth**: Medium - data-dependent

---

### **B44: Health Check Endpoint Security**
- **Location**: `HealthCheckController.java`, class level
- **Bug Type**: Security / Information disclosure
- **Proposed Change**: Add detailed system info to health check: JVM version, memory stats, database connection string. Information leak.
- **Trigger Conditions**: Any health check request
- **Expected Symptom**: Sensitive system information exposed; reconnaissance for attackers
- **Why It's Hard**: Seems useful for debugging; common in dev; security implication overlooked; helpful information
- **Suggested Detection**: Security audit; test that validates response doesn't leak sensitive data
- **Exercise Value**: Medium - security awareness
- **Stealth**: Medium - seems helpful

---

### **B45: Pagination Offset Calculation Error**
- **Location**: `EmployeeController.java`, `getEmployees()` method, line 51
- **Bug Type**: Off-by-one / Calculation error
- **Proposed Change**: Manually calculate offset as `page * size + 1` instead of using PageRequest. Off-by-one causes skipped records.
- **Trigger Conditions**: Any paginated request with page > 0
- **Expected Symptom**: First record of each page skipped; gaps in results; inconsistent pagination
- **Why It's Hard**: First page works; subtle off-by-one; might appear as data issue; hard to notice with small datasets
- **Suggested Detection**: Test that validates no gaps in paginated results; sequential page test
- **Exercise Value**: High - pagination logic
- **Stealth**: High - page-dependent

---

### **B46: Exception Message Localization Missing**
- **Location**: `GlobalExceptionalHandler.java`, all handlers
- **Bug Type**: Internationalization / UX
- **Proposed Change**: Hardcode English error messages. Non-English users get English errors even with Accept-Language header.
- **Trigger Conditions**: Requests with non-English Accept-Language header
- **Expected Symptom**: Wrong language error messages; poor UX for international users; inconsistent localization
- **Why It's Hard**: Works for English users; i18n often overlooked; appears as missing feature not bug; low priority
- **Suggested Detection**: Test with various Accept-Language headers; i18n coverage test
- **Exercise Value**: Low - i18n awareness
- **Stealth**: Medium - language-dependent

---

### **B47: DTO Validation Regex Performance**
- **Location**: `EmployeeDTO.java`, empAddress field, line 24
- **Bug Type**: Performance regression / ReDoS
- **Proposed Change**: Change address regex to have nested quantifiers: `^[a-zA-Z0-9+.,:=\\s]*[a-zA-Z0-9+.,:=\\s]*$`. Causes slow validation.
- **Trigger Conditions**: Long addresses with many special characters
- **Expected Symptom**: Slow validation; request timeout; CPU spike
- **Why It's Hard**: Short addresses fast; only long/complex addresses slow; appears as performance issue; regex complexity
- **Suggested Detection**: Performance test with various address lengths; timeout monitoring
- **Exercise Value**: High - ReDoS awareness
- **Stealth**: High - length-dependent

---

### **B48: Delete Soft Delete Missing**
- **Location**: `EmployeeDaoImpl.java`, `deleteEmployeeById()` method, line 37
- **Bug Type**: Data integrity / Audit trail
- **Proposed Change**: Hard delete instead of soft delete (set deleted flag). No audit trail, can't recover.
- **Trigger Conditions**: Any delete operation
- **Expected Symptom**: Data permanently lost; no recovery possible; audit compliance issue
- **Why It's Hard**: Works as coded; business requirement issue; might be intentional; compliance problem not obvious
- **Suggested Detection**: Audit requirement test; data recovery test; compliance check
- **Exercise Value**: Medium - soft delete pattern
- **Stealth**: High - requirement interpretation

---

### **B49: Search Trim Missing**
- **Location**: `EmployeeController.java`, `searchEmployees()` method, line 26
- **Bug Type**: UX / Edge case
- **Proposed Change**: Don't trim search parameter. Leading/trailing spaces cause no results even for valid names.
- **Trigger Conditions**: Search with leading/trailing whitespace
- **Expected Symptom**: No results for valid search; user frustration; appears as data issue
- **Why It's Hard**: Works without spaces; common user error; seems like validation issue; UX problem
- **Suggested Detection**: Test with whitespace variations; input sanitization test
- **Exercise Value**: Low - input sanitization
- **Stealth**: Medium - input-dependent

---

### **B50: Repository Query Timeout Missing**
- **Location**: `EmployeeRepository.java`, custom methods
- **Bug Type**: Performance / Resource exhaustion
- **Proposed Change**: No query timeout configured. Long-running queries block threads indefinitely.
- **Trigger Conditions**: Large dataset or slow database; complex queries
- **Expected Symptom**: Thread exhaustion; application hangs; cascading failures
- **Why It's Hard**: Works with small data; scales poorly; appears as performance issue; no error until resources exhausted
- **Suggested Detection**: Load test with large dataset; timeout configuration test; thread pool monitoring
- **Exercise Value**: High - timeout and resource management
- **Stealth**: Very High - data-size and load-dependent

---

## Top 10 Recommended Bugs (Ranked by Exercise Value + Stealth)

1. **B11** - Dotenv loading race condition: Very high educational value on concurrency and initialization order; extremely stealthy (timing-dependent)

2. **B08** - Email validation ReDoS: Critical security awareness; very stealthy (input-dependent performance issue)

3. **B37** - Optimistic locking missing: Essential concurrency pattern; very stealthy (requires concurrent load)

4. **B18** - Concurrent pagination consistency: Deep understanding of isolation levels; very stealthy (timing-dependent)

5. **B03** - Contact number overflow silent truncation: Data integrity + silent failure; very stealthy (data-dependent, international)

6. **B35** - Contact number leading zero loss: Numeric vs string data understanding; very stealthy (country-specific)

7. **B17** - Salary parsing locale dependency: Internationalization awareness; very stealthy (environment-dependent)

8. **B06** - Salary precision loss: Floating point precision awareness; high stealth (subtle data corruption)

9. **B29** - Date format ambiguity: Format consistency across layers; very stealthy (data-dependent)

10. **B40** - Dotenv missing graceful degradation: Configuration flexibility; high stealth (environment-dependent)

---

## Implementation Notes

When implementing selected bugs:
- Each bug is in a different file/module to ensure independence
- No bug causes compilation errors
- Tests that directly reveal bugs will be identified for removal
- Changes remain minimal and plausible as engineering mistakes
- All bugs are in core 20% functionality (Employee CRUD flow)
- Diverse bug types: concurrency (B11, B18, B37), data integrity (B03, B06, B35), security (B08), i18n (B17, B29), configuration (B40)
