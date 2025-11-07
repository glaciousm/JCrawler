package com.jcrawler.dao;

import com.jcrawler.core.HibernateConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseDao<T, ID> {

    private final Class<T> entityClass;
    protected final SessionFactory sessionFactory;

    public BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    // Save or update entity
    public T save(T entity) {
        return executeInTransaction(session -> {
            session.persist(entity);
            session.flush();  // Force immediate write to database
            session.clear();  // Clear first-level cache to prevent memory buildup
            return entity;
        });
    }

    // Update entity
    public T update(T entity) {
        return executeInTransaction(session -> {
            return session.merge(entity);
        });
    }

    // Find by ID
    public Optional<T> findById(ID id) {
        return executeInSession(session -> {
            T entity = session.get(entityClass, id);
            return Optional.ofNullable(entity);
        });
    }

    // Find all
    public List<T> findAll() {
        return executeInSession(session -> {
            Query<T> query = session.createQuery("FROM " + entityClass.getSimpleName(), entityClass);
            return query.list();
        });
    }

    // Delete by ID
    public void deleteById(ID id) {
        executeInTransaction(session -> {
            T entity = session.get(entityClass, id);
            if (entity != null) {
                session.remove(entity);
            }
        });
    }

    // Delete entity
    public void delete(T entity) {
        executeInTransaction(session -> {
            session.remove(entity);
        });
    }

    // Count all
    public Long count() {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class);
            return query.uniqueResult();
        });
    }

    // Execute in transaction (for write operations)
    protected <R> R executeInTransaction(Function<Session, R> action) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            R result = action.apply(session);
            transaction.commit();
            return result;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Database operation failed", e);
        }
    }

    // Execute in transaction without return value
    protected void executeInTransaction(Consumer<Session> action) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            action.accept(session);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Database operation failed", e);
        }
    }

    // Execute in session (for read operations)
    protected <R> R executeInSession(Function<Session, R> action) {
        try (Session session = sessionFactory.openSession()) {
            return action.apply(session);
        } catch (Exception e) {
            throw new RuntimeException("Database query failed", e);
        }
    }
}
