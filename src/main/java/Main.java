import dao.AuthorDao;
import dao.BookDao;
import dao.CategoryDao;
import dao.PublisherDao;
import entity.Author;
import entity.Book;
import entity.Category;
import entity.Customer;
import entity.Employee;
import entity.Person;
import entity.Publisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.LazyInitializationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;


public class Main {

    public static void main(String[] args) {

        Publisher publisher1 = new Publisher("Penguin Random House", "1745 Broadway, New York", "USA");
        Publisher publisher2 = new Publisher("HarperCollins", "195 Broadway, New York", "USA");
        Publisher publisher3 = new Publisher("Simon & Schuster", "1230 Avenue of the Americas, New York", "USA");
        Publisher publisher4 = new Publisher("Macmillan Publishers", "120 Broadway, New York", "USA");

        Category fiction = new Category("Fiction");
        Category sciFi = new Category("Science Fiction");
        Category history = new Category("History");
        Category technology = new Category("Technology");

        Author author1 = new Author("Matt Haig", "matt.haig@email.com", "British");
        Author author2 = new Author("Yuval Noah Harari", "yuval.harari@email.com", "Israeli");
        Author author3 = new Author("Robert C. Martin", "uncle.bob@email.com", "American");
        Author author4 = new Author("Frank Herbert", "frank.herbert@email.com", "American");

        author1.addBook(new Book("The Midnight Library", "978-0525559474", 2020, publisher1, Set.of(fiction, sciFi)));
        author1.addBook(new Book("How to Stop Time", "978-0525522874", 2017, publisher1, Set.of(fiction)));
        author2.addBook(new Book("Sapiens", "978-0062316097", 2015, publisher2, Set.of(history)));
        author3.addBook(new Book("Clean Code", "978-0132350884", 2008, publisher3, Set.of(technology, fiction)));
        author4.addBook(new Book("Dune", "978-0441172719", 1965, publisher4, Set.of(sciFi)));

        Employee employee1 = new Employee("Sara", "Ahmed", "sara.ahmed@library.com",
                "EMP-001", "Circulation", Instant.now());
        Customer customer1 = new Customer("Omar", "Khaled", "omar.khaled@email.com",
                "MEM-1001", Instant.now(), true);

        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("libraryPU")) {
            EntityManager em = emf.createEntityManager();

            PublisherDao publisherDao = new PublisherDao(em);
            CategoryDao categoryDao = new CategoryDao(em);
            AuthorDao authorDao = new AuthorDao(em);
            BookDao bookDao;

            System.out.println("\n=== Inserting sample data ===");

            publisherDao.save(publisher1);
            publisherDao.save(publisher2);
            publisherDao.save(publisher3);
            publisherDao.save(publisher4);

            categoryDao.save(fiction);
            categoryDao.save(sciFi);
            categoryDao.save(history);
            categoryDao.save(technology);


            authorDao.save(author1);
            authorDao.save(author2);
            authorDao.save(author3);
            authorDao.save(author4);

            em.getTransaction().begin();
            em.persist(employee1);
            em.persist(customer1);
            em.getTransaction().commit();

            System.out.println("\n=== [Task 1] Owning vs inverse side ===");
            System.out.println("Book.author holds @JoinColumn(name=\"author_id\") -> Book is the OWNING side.");
            System.out.println("Author.books uses mappedBy=\"author\" -> Author is the INVERSE side (read-only for the FK).");
            System.out.println("Book.publisher (@ManyToOne) is the owning side of Book<->Publisher; the FK publisher_id lives on book.");
            System.out.println("Book<->Category is @ManyToMany; neither side can hold the FK alone, so a join table (book_category) is required.");

            System.out.println("\n=== [Task 1] Cascade demo: persisting an Author cascades to its Books ===");
            List<Book> author1Books = authorDao.findAllBooksByAuthorId(author1.getId());
            System.out.println(author1.getName() + " has " + author1Books.size());

            System.out.println("\n=== [Task 1] orphanRemoval demo ===");
            em.getTransaction().begin();
            Author managedAuthor1 = em.find(Author.class, author1.getId());
            int booksBefore = managedAuthor1.getBooks().size();
            Book removedBook = managedAuthor1.getBooks().remove(0); // removed from the collection, not just unlinked
            em.getTransaction().commit();
            long booksAfter = authorDao.findAllBooksByAuthorId(author1.getId()).size();
            System.out.println("Books before removal: " + booksBefore + ", after removal: " + booksAfter);
            System.out.println("\"" + removedBook.getTitle() + "\" was removed from author.getBooks() and, "
                    + "thanks to orphanRemoval=true, its row was deleted from the book table too.");

            System.out.println("\n=== [Task 1] Intentional LazyInitializationException ===");
            Author lazyAuthor = authorDao.findById(author2.getId());
            em.clear();
            System.out.println("Name still readable after detach: " + lazyAuthor.getName());
            try {
                System.out.println("Book count: " + lazyAuthor.getBooks().size());
            } catch (LazyInitializationException e) {
                System.out.println("Caught expected LazyInitializationException: " + e.getMessage());
                System.out.println("Why: Author.books is FetchType.LAZY, so em.find() returned a proxy. "
                        + "The proxy can only be initialized while its EntityManager/Session is open; "
                        + "em.clear() detached it, so accessing .size() fails.");
            }

            System.out.println("\n=== [Task 1] Fixing it with JOIN FETCH ===");
            em.clear();
            Author fetchedWithBooks = authorDao.findByIdWithBooks(author1.getId());
            em.close(); // fully close the EM; the collection was already loaded
            System.out.println(fetchedWithBooks.getName() + " -> " + fetchedWithBooks.getBooks().size()
                    + " books (readable even after the EntityManager is closed, because JOIN FETCH "
                    + "loaded the collection in the same SQL query as the Author).");


            em = emf.createEntityManager();
            authorDao = new AuthorDao(em);
            bookDao = new BookDao(em);

            System.out.println("\n=== [Task 1] Inheritance: Employee / Customer via SINGLE_TABLE ===");
            List<Person> allPeople = em.createQuery("SELECT p FROM Person p", Person.class).getResultList();
            for (Person p : allPeople) {
                String kind = (p instanceof Employee) ? "Employee" : "Customer";
                System.out.println(kind + ": " + p.getFirstName() + " " + p.getLastName());
            }
            System.out.println("Strategy chosen: SINGLE_TABLE with a person_type discriminator column. "
                    + "Reason: Employee and Customer share most columns and are a small, shallow hierarchy, "
                    + "so one table with nullable subclass columns avoids joins on every polymorphic query "
                    + "(\"SELECT p FROM Person p\" above runs as a single SELECT).");

            // TASK 2 — JPQL / HQL, JOIN FETCH, aggregation, Criteria API
            System.out.println("\n=== [Task 2] Requirement 1: books by author name (JPQL) ===");
            bookDao.findByAuthorName("Matt Haig").forEach(System.out::println);

            System.out.println("\n=== [Task 2] Requirement 2: books by publisher ===");
            Publisher managedPublisher1 = em.find(Publisher.class, publisher1.getId());
            bookDao.findByPublisher(managedPublisher1).forEach(System.out::println);

            System.out.println("\n=== [Task 2] Requirement 3: book by id, positional parameter ===");

            Long cleanCodeId = authorDao.findAllBooksByAuthorId(author3.getId()).get(0).getId();
            Book byIdPositional = bookDao.findByIdPositional(cleanCodeId);
            System.out.println(byIdPositional);

            System.out.println("\n=== [Task 2] Requirement 4: Author + Books via JOIN FETCH ===");
            Author authorWithBooks = authorDao.findByIdWithBooks(author3.getId());
            System.out.println(authorWithBooks.getName() + " -> " + authorWithBooks.getBooks());

            System.out.println("\n=== [Task 2] Requirement 5: COUNT + GROUP BY per author ===");
            for (Object[] row : authorDao.countBooksPerAuthor()) {
                System.out.println(row[0] + " -> " + row[1] + " book(s)");
            }

            System.out.println("\n=== [Task 2] Requirement 6: Criteria API — books by title ===");
            bookDao.findByTitleCriteria("Dune").forEach(System.out::println);

            System.out.println("\n=== [Task 2] Requirement 7: Criteria API — optional title/author filters ===");
            System.out.println("Only title=\"the\":");
            bookDao.searchBooks("the", null).forEach(System.out::println);
            System.out.println("Only author=\"Matt Haig\":");
            bookDao.searchBooks(null, "Matt Haig").forEach(System.out::println);
            System.out.println("Both filters:");
            bookDao.searchBooks("midnight", "Matt Haig").forEach(System.out::println);
            System.out.println("No filters (all books):");
            bookDao.searchBooks(null, null).forEach(System.out::println);

            System.out.println("\n=== [Task 2] JPQL vs HQL ===");
            System.out.println("Standard JPQL (portable across providers): explicit join + GROUP BY/HAVING to filter by book count.");
            List<Author> viaJpql = em.createQuery(
                            "SELECT a FROM Author a JOIN a.books b GROUP BY a HAVING COUNT(b) > 1", Author.class)
                    .getResultList();
            viaJpql.forEach(a -> System.out.println("  " + a.getName()));

            System.out.println("Hibernate-only HQL: size() is not part of the JPQL spec, so this ties the code to Hibernate.");
            authorDao.findAuthorsWithMoreThanNBooks(1)
                    .forEach(a -> System.out.println("  " + a.getName()));

            em.close();
        }
    }
}
