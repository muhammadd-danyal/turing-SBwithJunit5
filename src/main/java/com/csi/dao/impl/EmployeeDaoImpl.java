package com.csi.dao.impl;

import com.csi.dao.EmployeeDao;
import com.csi.exception.EmployeeNotFound;
import com.csi.model.Employee;
import com.csi.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmployeeDaoImpl implements EmployeeDao {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee saveData(Employee employee) {
        if (employee.getEmpId() != 0) {
            Employee existing = employeeRepository.findById(employee.getEmpId()).orElse(null);
            if (existing != null) {
                employeeRepository.save(employee);
                return employee;
            }
        }
        employeeRepository.save(employee);
        return employee;
    }

    @Override
    public Employee getDataById(int empId) {
        return employeeRepository.findById(empId).orElseThrow(() -> new EmployeeNotFound("Employee Not Found !!!!"));
    }

    @Override
    public List<Employee> getAllData() {
        return employeeRepository.streamAllBy().collect(Collectors.toList());
    }

    @Override
    public void deleteEmployeeById(int empId) {
        Employee employee = employeeRepository.findById(empId)
                .orElseThrow(() -> new EmployeeNotFound("Employee Not Found !!!!"));
        employeeRepository.delete(employee);
    }

    @Override
    public Page<Employee> getAllData(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Override
    public List<Employee> searchEmployees(String name) {
        return employeeRepository.findByEmpNameContainingIgnoreCase(name);
    }

    @Override
    public Page<Employee> searchEmployees(String name, Pageable pageable) {
        return employeeRepository.findByEmpNameContainingIgnoreCase(name, pageable);
    }
}