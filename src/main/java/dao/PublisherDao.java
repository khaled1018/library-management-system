package dao;

import entity.Publisher;
import jakarta.persistence.EntityManager;

public class PublisherDao {

    private final EntityManager em;

    public PublisherDao(EntityManager em) {
        this.em = em;
    }

    private void beginTransaction() { em.getTransaction().begin(); }
    private void commitTransaction() { em.getTransaction().commit(); }

    public void save(Publisher publisher) {
        beginTransaction();
        em.persist(publisher);
        commitTransaction();
    }
}