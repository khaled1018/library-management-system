package dao;

import entity.Author;
import entity.Book;
import jakarta.persistence.EntityManager;

import java.util.List;

public class AuthorDao {

    private final EntityManager em;

    public AuthorDao(EntityManager em) {
        this.em = em;
    }

    private void beginTransaction() { em.getTransaction().begin(); }
    private void commitTransaction() { em.getTransaction().commit(); }

    public void save(Author author) {
        beginTransaction();
        em.persist(author);
        commitTransaction();
    }

    public Author findById(Long id) {
        return em.find(Author.class, id);
    }

    public Author findByIdWithBooks(Long id) {
        List<Author> result = em.createQuery(
                        "SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id = :id",
                        Author.class)
                .setParameter("id", id)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Book> findAllBooksByAuthorId(Long authorId) {
        return em.createQuery(
                        "SELECT b FROM Book b WHERE b.author.id = :authorId",
                        Book.class)
                .setParameter("authorId", authorId)
                .getResultList();
    }

    public List<Author> findAuthorsWithMoreThanNBooks(int n) {
        return em.createQuery(
                        "SELECT a FROM Author a WHERE size(a.books) > :n",
                        Author.class)
                .setParameter("n", n)
                .getResultList();
    }

    public List<Object[]> countBooksPerAuthor() {
        return em.createQuery(
                        "SELECT a.name, COUNT(b) FROM Author a " +
                                "LEFT JOIN a.books b " +
                                "GROUP BY a.name " +
                                "ORDER BY a.name",
                        Object[].class)
                .getResultList();
    }
}