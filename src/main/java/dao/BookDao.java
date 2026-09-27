package dao;

import entity.Book;
import entity.Publisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

public class BookDao {

    private final EntityManager em;

    public BookDao(EntityManager em) {
        this.em = em;
    }

    private void beginTransaction() { em.getTransaction().begin(); }
    private void commitTransaction() { em.getTransaction().commit(); }

    public void save(Book book) {
        beginTransaction();
        em.persist(book);
        commitTransaction();
    }

    public List<Book> findByAuthorName(String name) {
        return em.createQuery(
                        "SELECT b FROM Book b WHERE b.author.name = :name",
                        Book.class)
                .setParameter("name", name)
                .getResultList();
    }

    public List<Book> findByPublisher(Publisher publisher) {
        return em.createQuery(
                        "SELECT b FROM Book b WHERE b.publisher = :p",
                        Book.class)
                .setParameter("p", publisher)
                .getResultList();
    }

    public Book findByIdPositional(Long id) {
        return em.createQuery(
                        "SELECT b FROM Book b WHERE b.id = ?1",
                        Book.class)
                .setParameter(1, id)
                .getSingleResult();
    }

    public List<Book> findByTitleCriteria(String title) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);
        cq.select(root).where(cb.equal(root.get("title"), title));
        return em.createQuery(cq).getResultList();
    }

    public List<Book> searchBooks(String title, String authorName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);

        List<Predicate> predicates = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("title")),
                    "%" + title.toLowerCase() + "%"));
        }
        if (authorName != null && !authorName.isBlank()) {
            predicates.add(cb.equal(root.get("author").get("name"), authorName));
        }

        cq.select(root).where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList();
    }
}