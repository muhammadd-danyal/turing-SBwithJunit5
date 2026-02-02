package com.csi.repository;

import com.csi.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @Query(value = "SELECT * FROM employee WHERE TRIM(emp_name) LIKE CONCAT('%', TRIM(:name), '%')", nativeQuery = true)
    List<Employee> findByEmpName(@Param("name") String empName);
    
    @Query(value = "SELECT * FROM employee WHERE TRIM(emp_name) LIKE CONCAT('%', TRIM(:name), '%')", 
           countQuery = "SELECT COUNT(*) FROM employee WHERE TRIM(emp_name) LIKE CONCAT('%', TRIM(:name), '%')",
           nativeQuery = true)
    Page<Employee> findByEmpName(@Param("name") String empName, Pageable pageable);
}
