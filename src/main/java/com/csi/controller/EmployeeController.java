package com.csi.controller;

import com.csi.dto.EmployeeDTO;
import com.csi.model.Employee;
import com.csi.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;


    @GetMapping("/search")
    public ResponseEntity<?> searchEmployees(
            @RequestParam String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if(page == null && size == null) {
            return ResponseEntity.ok(employeeService.searchEmployees(name));
        }
        else {
            int pageNumber = (page != null) ? page : 0;
            int pageSize = (size != null) ? size : 10;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            return ResponseEntity.ok(employeeService.searchEmployees(name, pageable));
        }

    }


    @GetMapping("/pages")
    public Page<Employee> getEmployees(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        int pageNumber = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return employeeService.getAllData(pageable);
    }

    @GetMapping("/")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllData());
    }

    @PostMapping("/save")
    public ResponseEntity<Employee> saveData(@Valid @RequestBody EmployeeDTO employeeDTO) {
        return new ResponseEntity<>(employeeService.saveData(employeeDTO), HttpStatus.CREATED);
    }

    @PutMapping("/update/{empId}")
    public ResponseEntity<Employee> updateData(@PathVariable int empId, @Valid @RequestBody EmployeeDTO employeeDTO) {
        return new ResponseEntity<>(employeeService.updateData(empId, employeeDTO), HttpStatus.CREATED);
    }

    @GetMapping("/get/{empId}")
    public ResponseEntity<Employee> getEmployee(@PathVariable int empId) {
        return ResponseEntity.ok(employeeService.getDataById(empId));
    }

    @DeleteMapping("/deleteById/{empId}")
    public ResponseEntity<String> deleteEmployeeById(@PathVariable int empId) {
        employeeService.deleteEmployeeById(empId);
        return ResponseEntity.ok("Employee Deleted Successfully");
    }
}