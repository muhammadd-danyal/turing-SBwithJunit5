package com.csi.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmployeeDTO Validation Tests")
class EmployeeDTOValidationTest {

    private Validator validator;
    private SimpleDateFormat dateFormat;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    }

    @Nested
    @DisplayName("Employee Name Validation Tests")
    class NameValidationTests {

        @Test
        @DisplayName("Should pass validation with valid name")
        void shouldPassWithValidName() {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpName("John Doe");

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "abc", "a", "ab", "123"})
        @DisplayName("Should fail when name is invalid")
        void shouldFailWithInvalidName(String name) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpName(name);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"John123", "John@Doe", "John#Smith"})
        @DisplayName("Should fail when name contains special characters or numbers")
        void shouldFailWithSpecialCharacters(String name) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpName(name);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Special character & number are not allowed");
        }

        @Test
        @DisplayName("Should fail when name exceeds 18 characters")
        void shouldFailWhenNameTooLong() {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpName("This is a very long name");

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Name should contain 4 to 18 characters");
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "john@example.com",
                "jane.smith@company.co",
                "test123@test.org"
        })
        @DisplayName("Should pass with valid email")
        void shouldPassWithValidEmail(String email) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpEmail(email);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "invalid-email",
                "@example.com",
                "test@",
                "test@@example.com",
                "test@example"
        })
        @DisplayName("Should fail with invalid email")
        void shouldFailWithInvalidEmail(String email) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpEmail(email);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Contact Number Validation Tests")
    class ContactNumberValidationTests {

        @Test
        @DisplayName("Should pass with valid 10-digit contact number")
        void shouldPassWithValid10DigitNumber() {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpContactNumber("1234567890");

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "12345678901", "abcdefghij", "123-456-7890"})
        @DisplayName("Should fail with invalid contact number")
        void shouldFailWithInvalidContactNumber(String contact) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpContactNumber(contact);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Salary Validation Tests")
    class SalaryValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"50000.00", "1234.56", "999999999.99"})
        @DisplayName("Should pass with valid salary format")
        void shouldPassWithValidSalary(String salary) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpSalary(salary);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"50000", "50000.5", "50000.567", "abc.de"})
        @DisplayName("Should fail with invalid salary format")
        void shouldFailWithInvalidSalary(String salary) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpSalary(salary);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Please enter salary in valid format (mm.mm eg- 254685.23))");
        }
    }

    @Nested
    @DisplayName("Address Validation Tests")
    class AddressValidationTests {

        @Test
        @DisplayName("Should pass with valid address")
        void shouldPassWithValidAddress() {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpAddress("123 Main Street, Apartment 4B, New York");

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Short", "123", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
        @DisplayName("Should fail with invalid address length")
        void shouldFailWithInvalidAddressLength(String address) {
            EmployeeDTO dto = createValidEmployeeDTO();
            dto.setEmpAddress(address);

            Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    private EmployeeDTO createValidEmployeeDTO() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmpName("John Doe");
        dto.setEmpEmail("john@example.com");
        dto.setEmpContactNumber("1234567890");
        dto.setEmpSalary("50000.00");
        dto.setEmpAddress("123 Main Street, Apartment 4B, New York City");
        try {
            dto.setEmpDOB(dateFormat.parse("15-05-1990"));
        } catch (ParseException e) {
            // Ignore for test
        }
        return dto;
    }
}