package com.csi.repository;

import com.csi.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByEmpNameContainingIgnoreCase(String empName);
    Page<Employee> findByEmpNameContainingIgnoreCase(String empName, Pageable pageable);
}
