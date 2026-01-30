package com.csi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee", indexes = {
    @Index(name = "idx_emp_email", columnList = "empEmail", unique = true)
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int empId;

    private String empName;

    private String empAddress;

    private long empContactNumber;

    private double empSalary;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date empDOB;

    private String empEmail;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "employee", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<EmployeeAudit> auditHistory;
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class EmployeeAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int auditId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    @JsonIgnore
    private Employee employee;
    
    private String action;
    private Date timestamp;
}
