package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.Instant;

@Entity
@DiscriminatorValue("Employee")
public class Employee extends Person {

    private String employeeCode;
    private String department;
    private Instant hireDate;

    public Employee() {}
    public Employee(String firstName, String lastName, String email,
                    String employeeCode, String department, Instant hireDate) {
        super(firstName, lastName, email);
        this.employeeCode = employeeCode;
        this.department = department;
        this.hireDate = hireDate;
    }
}
