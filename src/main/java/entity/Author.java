package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String nationality;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    protected Author() {
        // required by JPA
    }

    public Author(String name, String email, String nationality) {
        this.name = name;
        this.email = email;
        this.nationality = nationality;
    }


    public void addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
    }

    public List<Book> getBooks() {
        return books;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Author{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", nationality='" + nationality + '\'' +
                ", books=" + books +
                '}';
    }
}

