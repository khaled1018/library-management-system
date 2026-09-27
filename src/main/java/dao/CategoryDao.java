package dao;

import entity.Category;
import jakarta.persistence.EntityManager;

public class CategoryDao {

    private final EntityManager em;

    public CategoryDao(EntityManager em) {
        this.em = em;
    }

    private void beginTransaction() { em.getTransaction().begin(); }
    private void commitTransaction() { em.getTransaction().commit(); }

    public void save(Category category) {
        beginTransaction();
        em.persist(category);
        commitTransaction();
    }
}