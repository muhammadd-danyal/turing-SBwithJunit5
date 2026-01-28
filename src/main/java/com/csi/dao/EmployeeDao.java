package com.csi.dao;

import com.csi.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeDao {

    Employee saveData(Employee employee);

    Employee getDataById(int empId);

    List<Employee> getAllData();

    void deleteEmployeeById(int empId);

    Page<Employee> getAllData(Pageable pageable);

    List<Employee> searchEmployees(String name);

    Page<Employee> searchEmployees(String name, Pageable pageable);
}