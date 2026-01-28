# Spring Boot JUnit 5 Project Guidelines

## Architecture Overview
This is a Spring Boot 3.x application with layered architecture: Controller → Service → DAO → Repository → JPA Entity.
- **Controllers** handle REST endpoints with pagination support
- **Services** contain business logic, converting DTOs to Entities
- **DAOs** wrap repositories with custom exception handling
- **DTOs** use Bean Validation annotations for input validation
- **Entities** use Lombok for boilerplate reduction

## Key Patterns
- **DTO Conversion**: Services manually convert String fields from DTOs to primitives (e.g., `Long.parseLong(dto.getEmpContactNumber())`)
- **Validation**: Use `@Valid` in controllers, custom regex patterns in DTOs (e.g., salary format: `^[0-9]{1,9}+[.]{1}+[0-9]{2}+$`)
- **Pagination**: Controllers accept `page` and `size` params, defaulting to page 0, size 10
- **Search**: Case-insensitive name search via repository methods like `findByEmpNameContainingIgnoreCase`
- **Exceptions**: Custom exceptions (e.g., `EmployeeNotFound`) thrown from DAO layer

## Configuration
- **Environment Variables**: Loaded from `.env` file via java-dotenv in main class
- **Database**: MySQL for prod, H2 in-memory for tests
- **Test Setup**: `.env.test` in `src/test/resources/`, test data in `data.sql`

## Development Workflow
- **Build**: `mvn clean compile` (use `mvnw` wrapper)
- **Run**: `mvn spring-boot:run` (requires `.env` with DB credentials)
- **Test**: `mvn test` (runs JUnit 5 with H2, includes integration tests)
- **Package**: `mvn package` produces executable WAR
- **API Docs**: Swagger UI at `/swagger-ui.html` after startup

## Testing Conventions
- Use `@SpringBootTest` for integration tests with full context
- AssertJ for fluent assertions (e.g., `assertThat(list).hasSize(3)`)
- Test data pre-loaded via `data.sql` for consistent state
- Mock external dependencies if added later

## Code Style
- Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` on models/DTOs
- Field injection with `@Autowired` (not constructor injection)
- Entity IDs use `GenerationType.IDENTITY`
- REST responses wrapped in `ResponseEntity<?>`

## Deployment
- **WAR Packaging**: Deploy to Tomcat or run with `java -jar target/*.war`
- **Docker**: Multi-stage build with Eclipse Temurin JDK 21 and Maven Daemon
- **CI/CD**: Jenkins pipeline builds, tests, then deploys WAR to Tomcat</content>
