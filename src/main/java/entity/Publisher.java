package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publisher")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String country;

    @OneToMany(mappedBy = "publisher")
    private List<Book> books = new ArrayList<>();

    protected Publisher() {
        // required by JPA
    }

    public Publisher(String name, String address, String country) {
        this.name = name;
        this.address = address;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Publisher{name='" + name + "', country='" + country + "'}";
    }


}
