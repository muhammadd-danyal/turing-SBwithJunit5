package com.csi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionalHandler {

    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(EmployeeNotFound.class)
    public ResponseEntity<?> handleNotFound(EmployeeNotFound employeeNotFound) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> map = new HashMap<>();
        map.put("Message", messageSource.getMessage("error.employee.notfound", null, "Employee Not Found", locale));
        map.put("Status", HttpStatus.NOT_FOUND);
        map.put("Code", HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleBeanValidation(MethodArgumentNotValidException exception) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> map = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error ->
                map.put(((FieldError) error).getField(), messageSource.getMessage("error.validation.failed", 
                        new Object[]{error.getDefaultMessage()}, "Validation failed: " + error.getDefaultMessage(), locale)));
        return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleAnyException(RuntimeException runtimeException) {
        Map<String, Object> map = new HashMap<>();
        map.put("Message", runtimeException.getMessage());
        map.put("Status", HttpStatus.INTERNAL_SERVER_ERROR);
        map.put("Code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.info("Stack Trace", (Object[]) runtimeException.getStackTrace());
        return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}