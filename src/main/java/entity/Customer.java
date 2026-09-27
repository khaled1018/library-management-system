package entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
@DiscriminatorValue("Customer")
public class Customer extends Person {

    private String membershipNumber;
    private Instant registrationDate;
    private boolean active;

    public Customer() {}
    public Customer(String firstName, String lastName, String email,
                    String membershipNumber, Instant registrationDate, boolean active) {
        super(firstName, lastName, email);
        this.membershipNumber = membershipNumber;
        this.registrationDate = registrationDate;
        this.active = active;
    }

}
