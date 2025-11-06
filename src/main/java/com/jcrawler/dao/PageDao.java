package com.jcrawler.dao;

import com.jcrawler.model.Page;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class PageDao extends BaseDao<Page, Long> {

    public PageDao() {
        super(Page.class);
    }

    public List<Page> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Page> query = session.createQuery(
                "FROM Page WHERE sessionId = :sessionId", Page.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<Page> findBySessionIdAndDepthLevel(Long sessionId, Integer depthLevel) {
        return executeInSession(session -> {
            Query<Page> query = session.createQuery(
                "FROM Page WHERE sessionId = :sessionId AND depthLevel = :depthLevel", Page.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("depthLevel", depthLevel);
            return query.list();
        });
    }

    public Optional<Page> findBySessionIdAndUrl(Long sessionId, String url) {
        return executeInSession(session -> {
            Query<Page> query = session.createQuery(
                "FROM Page WHERE sessionId = :sessionId AND url = :url", Page.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("url", url);
            List<Page> results = query.list();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(p) FROM Page p WHERE p.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }

    public List<Page> findUnprocessedPages(Long sessionId) {
        return executeInSession(session -> {
            Query<Page> query = session.createQuery(
                "FROM Page p WHERE p.sessionId = :sessionId AND p.processed = false", Page.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }
}
