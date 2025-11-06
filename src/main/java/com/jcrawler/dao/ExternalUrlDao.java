package com.jcrawler.dao;

import com.jcrawler.model.ExternalUrl;
import org.hibernate.query.Query;

import java.util.List;

public class ExternalUrlDao extends BaseDao<ExternalUrl, Long> {

    public ExternalUrlDao() {
        super(ExternalUrl.class);
    }

    public List<ExternalUrl> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<ExternalUrl> query = session.createQuery(
                "FROM ExternalUrl WHERE sessionId = :sessionId", ExternalUrl.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(e) FROM ExternalUrl e WHERE e.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }
}
