package com.csi.repository;

import com.csi.model.Employee;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByEmpNameContainingIgnoreCase(String empName);
    Page<Employee> findByEmpNameContainingIgnoreCase(String empName, Pageable pageable);
    
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "50"))
    Stream<Employee> streamAllBy();
}
