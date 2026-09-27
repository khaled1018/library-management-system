package entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "isbn")
    private String isbn;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @JoinColumn(name = "author_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Author author;

    @JoinColumn(name = "publisher_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Publisher publisher;

    @ManyToMany
    @JoinTable(name = "book_category", joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    protected Book() {
        // required by JPA
    }

    public Book(String title, String isbn, Integer publicationYear, Publisher publisher, Set<Category> categories) {
        this.title = title;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.publisher = publisher;
        this.categories = categories;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }


    @Override
    public String toString() {
        // Remove Author and Publisher so they wouldn't cause an LazyInitializationException
        return "Book{id=" + id + ", title='" + title + '\'' +
                ", isbn='" + isbn + '\'' + ", year=" + publicationYear + '}';
    }
}
