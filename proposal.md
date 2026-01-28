# Codebase Bug Proposal

## Repo Map

### Subsystems and Key Files

- **Application Entry Point**: Handles Spring Boot startup and environment variable loading from .env files.
  - `SbJunit5Application.java`: Main class with custom property source for dotenv integration.

- **Web Layer (Controllers)**: REST API endpoints for employee CRUD operations, including pagination and search.
  - `EmployeeController.java`: REST controller with GET, POST, PUT, DELETE mappings; handles request parameters for pagination (page, size defaults to 0, 10).

- **Business Logic (Services)**: Converts DTOs to entities, performs business rules, delegates to DAOs.
  - `EmployeeService.java`: Interface defining CRUD and search methods.
  - `EmployeeServiceImpl.java`: Implementation with manual string-to-primitive conversions (e.g., Long.parseLong for contact number).

- **Data Access Layer (DAOs)**: Wraps JPA repositories, adds custom exception handling.
  - `EmployeeDao.java`: Interface for data operations.
  - `EmployeeDaoImpl.java`: Implementation using repository, throws EmployeeNotFound for missing entities.

- **Data Persistence (Repository)**: JPA repository with custom query methods.
  - `EmployeeRepository.java`: Extends JpaRepository, includes findByEmpNameContainingIgnoreCase for search.

- **Data Models**: JPA entities and DTOs with validation.
  - `Employee.java`: Entity with Lombok annotations, GenerationType.IDENTITY for ID.
  - `EmployeeDTO.java`: Input DTO with Bean Validation (@NotBlank, @Pattern, etc.).

- **Exception Handling**: Custom exceptions for error cases.
  - `EmployeeNotFound.java`: Custom exception for missing employees.

- **Configuration**: YAML configs and environment handling.
  - `application.yml`: Prod config for MySQL, JPA settings.
  - Test `application.yml`: H2 in-memory for tests, with data.sql initialization.

- **Testing**: Integration tests with Spring Boot context, H2 database.
  - Various test classes (e.g., EmployeeServiceImplTest.java): Use @SpringBootTest, AssertJ assertions, pre-loaded test data.

- **Build and Deployment**: Maven build, Docker, CI/CD.
  - `pom.xml`: Dependencies (Spring Boot 3.5.6, Java 21, MySQL, H2, Lombok, etc.).
  - `Dockerfile`: Multi-stage with Eclipse Temurin JDK 21 and Maven Daemon.
  - `Jenkinsfile`: Pipeline for build, test, deploy to Tomcat.

- **Other**: Health check controller, but minimal.

Core 20% focuses on Controllers, Services, DAOs, Repository, Models/DTOs, and main app for business logic and data flow.

## Bug Candidates

B01  
Location: EmployeeController.java, searchEmployees method, lines ~25-35  
Bug type: Incorrect default value handling  
Proposed change: Change default page size from 10 to 0 when size is null but page is provided.  
Trigger conditions: When searching with page=1 but no size specified.  
Expected symptom: Returns empty page instead of default 10 items.  
Why it’s hard: Logic seems correct at glance; only fails with specific param combinations not in tests.  
Suggested detection: Integration test with page=1, no size, assert non-empty results.  

B02  
Location: EmployeeServiceImpl.java, saveData method, lines ~20-30  
Bug type: Numeric precision loss  
Proposed change: Use Double.valueOf instead of Double.parseDouble for salary, but truncate to 2 decimals incorrectly.  
Trigger conditions: Salary input with more than 2 decimal places.  
Expected symptom: Salary stored with wrong precision, e.g., 123.456 becomes 123.45 but calculation errors.  
Why it’s hard: Tests use exact values; real inputs vary.  
Suggested detection: Property-based test with random salary values.  

B03  
Location: EmployeeDaoImpl.java, getDataById method, lines ~25-30  
Bug type: Error swallowed  
Proposed change: Catch EmployeeNotFound and return null instead of throwing.  
Trigger conditions: Request for non-existent employee ID.  
Expected symptom: Null returned silently, leading to NPE downstream.  
Why it’s hard: Tests expect exception; but if caller doesn't handle, subtle.  
Suggested detection: Unit test asserting exception is thrown.  

B04  
Location: EmployeeRepository.java, findByEmpNameContainingIgnoreCase, lines ~10-15  
Bug type: Case sensitivity inconsistency  
Proposed change: Remove IgnoreCase, making search case-sensitive.  
Trigger conditions: Search with different case than stored data.  
Expected symptom: No results for case-mismatched names.  
Why it’s hard: Test data matches case; user inputs vary.  
Suggested detection: Test with mixed-case inputs.  

B05  
Location: EmployeeDTO.java, empSalary pattern, lines ~25-30  
Bug type: Regex boundary issue  
Proposed change: Change regex to allow negative salaries or extra decimals.  
Trigger conditions: Input like "-100.00" or "100.123".  
Expected symptom: Invalid salaries accepted.  
Why it’s hard: Validation passes for edge cases not tested.  
Suggested detection: Fuzz testing on DTO validation.  

B06  
Location: SbJunit5Application.java, main method, lines ~20-30  
Bug type: Environment variable loading failure  
Proposed change: Skip loading if .env file missing, use defaults.  
Trigger conditions: .env file absent in prod.  
Expected symptom: App starts with wrong DB config.  
Why it’s hard: Works in dev; prod deployment issue.  
Suggested detection: Integration test without .env.  

B07  
Location: EmployeeController.java, getEmployees method, lines ~40-50  
Bug type: Off-by-one in pagination  
Proposed change: Use page-1 in PageRequest.of.  
Trigger conditions: page=1, expects first page.  
Expected symptom: Skips first page, shows second.  
Why it’s hard: page=0 works; tests may not cover page=1.  
Suggested detection: Test pagination with page=1.  

B08  
Location: EmployeeServiceImpl.java, updateData method, lines ~35-45  
Bug type: Partial update failure  
Proposed change: Only update name, skip other fields.  
Trigger conditions: Update request with multiple fields.  
Expected symptom: Only name changes, others ignored.  
Why it’s hard: Tests update all fields; partial updates rare.  
Suggested detection: Test partial updates.  

B09  
Location: EmployeeDaoImpl.java, searchEmployees method, lines ~45-50  
Bug type: Ordering inconsistency  
Proposed change: Return results in reverse order.  
Trigger conditions: Search query.  
Expected symptom: Results in wrong order, confusing UI.  
Why it’s hard: No order specified; tests don't check order.  
Suggested detection: Test asserting result order.  

B10  
Location: Employee.java, empId field, lines ~15-20  
Bug type: ID generation race  
Proposed change: Change to TABLE strategy, but misconfigure.  
Trigger conditions: Concurrent saves.  
Expected symptom: Duplicate IDs in high load.  
Why it’s hard: Single-threaded tests; concurrency rare.  
Suggested detection: Load test with concurrent inserts.  

B11  
Location: EmployeeDTO.java, empContactNumber pattern, lines ~20-25  
Bug type: Length validation gap  
Proposed change: Allow 9 digits instead of exactly 10.  
Trigger conditions: 9-digit number input.  
Expected symptom: Invalid contact accepted.  
Why it’s hard: Tests use 10 digits; edge lengths not covered.  
Suggested detection: Boundary value tests for length.  

B12  
Location: EmployeeController.java, POST /employee, lines ~55-65  
Bug type: Response status mismatch  
Proposed change: Return 200 instead of 201 for create.  
Trigger conditions: Successful creation.  
Expected symptom: Wrong HTTP status, API clients confused.  
Why it’s hard: Functional test checks data, not status.  
Suggested detection: API contract test for status codes.  

B13  
Location: EmployeeServiceImpl.java, getAllData pageable, lines ~50-60  
Bug type: Page size limit ignored  
Proposed change: Always return all data regardless of pageable.  
Trigger conditions: Large page size requested.  
Expected symptom: Performance issue with big data.  
Why it’s hard: Tests use small data; pagination not fully tested.  
Suggested detection: Performance test with large pages.  

B14  
Location: EmployeeRepository.java, custom query, lines ~10-15  
Bug type: Query injection vulnerability  
Proposed change: Use string concatenation instead of parameter binding.  
Trigger conditions: Malicious search input.  
Expected symptom: SQL injection possible.  
Why it’s hard: Tests use safe inputs; security scan needed.  
Suggested detection: Security audit or injection tests.  

B15  
Location: EmployeeDaoImpl.java, deleteEmployeeById, lines ~35-40  
Bug type: Soft delete missing  
Proposed change: Don't delete, just mark inactive (but no field).  
Trigger conditions: Delete request.  
Expected symptom: Data not removed, privacy issue.  
Why it’s hard: Tests check absence; but data lingers.  
Suggested detection: DB integrity check after delete.  

B16  
Location: SbJunit5Application.java, property source, lines ~25-35  
Bug type: Property override failure  
Proposed change: Add properties with wrong precedence.  
Trigger conditions: Conflicting env vars.  
Expected symptom: Wrong config used.  
Why it’s hard: Single env setup in tests.  
Suggested detection: Config test with overrides.  

B17  
Location: EmployeeController.java, PUT /employee/{id}, lines ~70-80  
Bug type: ID mismatch  
Proposed change: Use wrong ID from path.  
Trigger conditions: Update with path ID.  
Expected symptom: Updates wrong entity.  
Why it’s hard: Tests use correct IDs; path parsing assumed.  
Suggested detection: Test with mismatched IDs.  

B18  
Location: EmployeeServiceImpl.java, searchEmployees, lines ~65-75  
Bug type: Empty result handling  
Proposed change: Return null instead of empty list.  
Trigger conditions: No matches.  
Expected symptom: NPE in controller.  
Why it’s hard: Tests may not check empty cases.  
Suggested detection: Test empty search results.  

B19  
Location: EmployeeDTO.java, empEmail pattern, lines ~30-35  
Bug type: Email domain restriction  
Proposed change: Restrict to specific domain, but regex wrong.  
Trigger conditions: Email from other domains.  
Expected symptom: Valid emails rejected.  
Why it’s hard: Test emails match; real variety.  
Suggested detection: Email validation tests.  

B20  
Location: EmployeeDaoImpl.java, saveData, lines ~20-25  
Bug type: Duplicate key handling  
Proposed change: Ignore save if exists.  
Trigger conditions: Save existing ID.  
Expected symptom: Silent failure.  
Why it’s hard: Tests save new; updates separate.  
Suggested detection: Test duplicate saves.  

B21  
Location: EmployeeController.java, GET /employee/{id}, lines ~45-55  
Bug type: Caching header missing  
Proposed change: Add wrong cache headers.  
Trigger conditions: Repeated requests.  
Expected symptom: Stale data served.  
Why it’s hard: No cache in tests; performance issue.  
Suggested detection: Cache testing.  

B22  
Location: EmployeeServiceImpl.java, Long.parseLong, lines ~25-30  
Bug type: Number format exception swallowed  
Proposed change: Catch and set to 0.  
Trigger conditions: Invalid number string.  
Expected symptom: Contact set to 0.  
Why it’s hard: Validation prevents; but if bypassed.  
Suggested detection: Invalid input tests.  

B23  
Location: EmployeeRepository.java, findAll pageable, lines ~12-15  
Bug type: Page count off  
Proposed change: Return wrong total count.  
Trigger conditions: Paginated query.  
Expected symptom: Incorrect pagination UI.  
Why it’s hard: Tests check data, not metadata.  
Suggested detection: Pagination metadata test.  

B24  
Location: Employee.java, empSalary field, lines ~25-30  
Bug type: Type mismatch  
Proposed change: Store as String instead of double.  
Trigger conditions: Save operation.  
Expected symptom: DB type error.  
Why it’s hard: H2 flexible; MySQL strict.  
Suggested detection: DB schema test.  

B25  
Location: EmployeeController.java, search with page, lines ~30-40  
Bug type: Parameter precedence  
Proposed change: Ignore size if page present.  
Trigger conditions: Both params.  
Expected symptom: Wrong page size.  
Why it’s hard: Default logic; specific combos.  
Suggested detection: Param combination tests.  

B26  
Location: EmployeeServiceImpl.java, updateData, lines ~40-50  
Bug type: Field not updated  
Proposed change: Skip empEmail update.  
Trigger conditions: Update with email.  
Expected symptom: Email unchanged.  
Why it’s hard: Tests update all; partial rare.  
Suggested detection: Field-specific update tests.  

B27  
Location: EmployeeDaoImpl.java, getAllData, lines ~30-35  
Bug type: Lazy loading issue  
Proposed change: Fetch without joins.  
Trigger conditions: Access related data.  
Expected symptom: Lazy init exception.  
Why it’s hard: Simple model; no relations.  
Suggested detection: Entity graph tests.  

B28  
Location: SbJunit5Application.java, Dotenv load, lines ~20-25  
Bug type: File path wrong  
Proposed change: Hardcode path.  
Trigger conditions: Different working dir.  
Expected symptom: .env not loaded.  
Why it’s hard: Dev works; prod fails.  
Suggested detection: Environment test.  

B29  
Location: EmployeeDTO.java, empName pattern, lines ~15-20  
Bug type: Special char allowed  
Proposed change: Allow numbers in name.  
Trigger conditions: Name with digits.  
Expected symptom: Invalid names accepted.  
Why it’s hard: Regex strict; but if changed.  
Suggested detection: Regex boundary tests.  

B30  
Location: EmployeeController.java, ResponseEntity, lines ~50-60  
Bug type: Wrong content type  
Proposed change: Set to text/plain.  
Trigger conditions: API calls.  
Expected symptom: Client parsing fails.  
Why it’s hard: JSON assumed; tests pass.  
Suggested detection: Content type assertion.  

B31  
Location: EmployeeServiceImpl.java, Double.parseDouble, lines ~30-35  
Bug type: Locale issue  
Proposed change: Use US locale explicitly wrong.  
Trigger conditions: Non-US system.  
Expected symptom: Parse failure.  
Why it’s hard: Dev env matches.  
Suggested detection: Locale tests.  

B32  
Location: EmployeeRepository.java, ContainingIgnoreCase, lines ~10-15  
Bug type: Partial match bug  
Proposed change: Use exact match.  
Trigger conditions: Substring search.  
Expected symptom: No partial results.  
Why it’s hard: Test names exact.  
Suggested detection: Substring search tests.  

B33  
Location: EmployeeDaoImpl.java, PageRequest, lines ~50-55  
Bug type: Sort order wrong  
Proposed change: Sort descending.  
Trigger conditions: Paginated list.  
Expected symptom: Reverse order.  
Why it’s hard: No sort specified.  
Suggested detection: Sort assertion tests.  

B34  
Location: Employee.java, @Entity, lines ~10-15  
Bug type: Table name wrong  
Proposed change: @Table(name="wrong")  
Trigger conditions: DB queries.  
Expected symptom: Table not found.  
Why it’s hard: H2 auto-creates.  
Suggested detection: Schema validation.  

B35  
Location: EmployeeController.java, @RequestParam, lines ~25-30  
Bug type: Required param missing  
Proposed change: Make page required.  
Trigger conditions: No page param.  
Expected symptom: 400 error.  
Why it’s hard: Defaults set; but if changed.  
Suggested detection: Missing param tests.  

B36  
Location: EmployeeServiceImpl.java, new Employee(), lines ~20-25  
Bug type: Field initialization  
Proposed change: Set empId manually.  
Trigger conditions: Save new.  
Expected symptom: ID conflict.  
Why it’s hard: Auto-gen; but if set.  
Suggested detection: ID generation tests.  

B37  
Location: EmployeeDaoImpl.java, repository.save, lines ~20-25  
Bug type: Save failure silent  
Proposed change: Catch exception and log.  
Trigger conditions: DB error.  
Expected symptom: No error thrown.  
Why it’s hard: Tests succeed.  
Suggested detection: DB failure simulation.  

B38  
Location: EmployeeDTO.java, @Size, lines ~18-22  
Bug type: Min max swap  
Proposed change: min=18, max=4  
Trigger conditions: Name length 5.  
Expected symptom: Invalid accepted.  
Why it’s hard: Test lengths within.  
Suggested detection: Boundary size tests.  

B39  
Location: SbJunit5Application.java, SpringApplicationBuilder, lines ~30-35  
Bug type: Profile wrong  
Proposed change: Set wrong profile.  
Trigger conditions: Startup.  
Expected symptom: Wrong config.  
Why it’s hard: No profiles used.  
Suggested detection: Profile tests.  

B40  
Location: EmployeeController.java, Page<Employee>, lines ~40-45  
Bug type: Page content wrong  
Proposed change: Return wrong page.  
Trigger conditions: Page 2.  
Expected symptom: Duplicate data.  
Why it’s hard: Small data.  
Suggested detection: Page content tests.  

B41  
Location: EmployeeServiceImpl.java, employeeDao.saveData, lines ~35-40  
Bug type: Return wrong object  
Proposed change: Return old instead of updated.  
Trigger conditions: Update.  
Expected symptom: No change apparent.  
Why it’s hard: Data same.  
Suggested detection: Return value check.  

B42  
Location: EmployeeRepository.java, JpaRepository, lines ~8-12  
Bug type: Method name typo  
Proposed change: findByEmpNameContaining (no IgnoreCase)  
Trigger conditions: Case search.  
Expected symptom: No results.  
Why it’s hard: Test data case matches.  
Suggested detection: Case variation tests.  

B43  
Location: EmployeeDaoImpl.java, EmployeeNotFound, lines ~25-30  
Bug type: Wrong exception  
Proposed change: Throw RuntimeException.  
Trigger conditions: Not found.  
Expected symptom: Different error.  
Why it’s hard: Tests expect specific.  
Suggested detection: Exception type tests.  

B44  
Location: Employee.java, @GeneratedValue, lines ~15-18  
Bug type: Strategy wrong  
Proposed change: AUTO instead of IDENTITY.  
Trigger conditions: MySQL.  
Expected symptom: Sequence issues.  
Why it’s hard: H2 works.  
Suggested detection: DB-specific tests.  

B45  
Location: EmployeeController.java, @GetMapping, lines ~20-25  
Bug type: Path wrong  
Proposed change: /search2  
Trigger conditions: Call /search.  
Expected symptom: 404.  
Why it’s hard: Tests use correct path.  
Suggested detection: Endpoint tests.  

B46  
Location: EmployeeServiceImpl.java, Long.parseLong, lines ~25-30  
Bug type: Overflow handling  
Proposed change: No check for long max.  
Trigger conditions: Very large number.  
Expected symptom: Overflow.  
Why it’s hard: Test values small.  
Suggested detection: Large number tests.  

B47  
Location: EmployeeDaoImpl.java, repository.findById, lines ~25-30  
Bug type: Optional handling  
Proposed change: .get() instead of orElseThrow.  
Trigger conditions: Not found.  
Expected symptom: NoSuchElementException.  
Why it’s hard: Tests expect custom.  
Suggested detection: Exception tests.  

B48  
Location: EmployeeDTO.java, @Pattern, lines ~25-30  
Bug type: Regex escape  
Proposed change: Missing escape in regex.  
Trigger conditions: Special chars.  
Expected symptom: Invalid validation.  
Why it’s hard: Test inputs safe.  
Suggested detection: Regex fuzz tests.  

B49  
Location: SbJunit5Application.java, MapPropertySource, lines ~25-30  
Bug type: Key case wrong  
Proposed change: Uppercase keys.  
Trigger conditions: Env vars lowercase.  
Expected symptom: Not loaded.  
Why it’s hard: Case mismatch.  
Suggested detection: Case sensitivity tests.  

B50  
Location: EmployeeController.java, ResponseEntity.ok, lines ~35-40  
Bug type: Status code wrong  
Proposed change: Return 204 for data.  
Trigger conditions: Successful search.  
Expected symptom: No content header.  
Why it’s hard: Data present.  
Suggested detection: Status code tests.  

## Ranking
Ranked by exercise value (educational + non-trivial) and stealth (subtle):  
1. B14 (security, high value)  
2. B03 (error handling, stealth)  
3. B10 (concurrency, non-trivial)  
4. B23 (pagination, common issue)  
5. B02 (precision, subtle)  
6. B07 (off-by-one, classic)  
7. B18 (null handling, stealth)  
8. B32 (query logic, hard to spot)  
9. B46 (overflow, edge case)  
10. B49 (config, environment dependent)  
... (remaining in similar order, prioritizing diversity and difficulty)

## Top 10 Recommended Set
B14: Security vulnerability in query, educational for injection risks.  
B03: Error swallowing, teaches exception handling importance.  
B10: Concurrency issue, non-trivial in simple app.  
B23: Pagination metadata bug, common in APIs.  
B02: Numeric precision, subtle data integrity.  
B07: Off-by-one, classic mistake.  
B18: Null return, stealth NPE risk.  
B32: Search logic change, affects functionality.  
B46: Overflow handling, edge case.  
B49: Config loading, environment-specific.</content>
