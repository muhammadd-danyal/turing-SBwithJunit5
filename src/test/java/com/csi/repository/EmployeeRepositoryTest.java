package com.csi.repository;

import com.csi.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void testFindByNameContainingIgnoreCase() {
        List<Employee> results = employeeRepository.findByEmpNameContainingIgnoreCase("becca");
        assertEquals(1, results.size());
        assertEquals("Rebecca V", results.get(0).getEmpName());
    }

    @Test
    void testFindByNameWithPaginationContainingIgnoreCase() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Employee> results = employeeRepository.findByEmpNameContainingIgnoreCase("ohn", pageable);
        assertEquals(1, results.getContent().size());
        assertEquals("John F", results.getContent().get(0).getEmpName());
    }
}

