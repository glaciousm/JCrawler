package com.jcrawler.dao;

import com.jcrawler.model.ExtractedData;
import org.hibernate.query.Query;

import java.util.List;

public class ExtractedDataDao extends BaseDao<ExtractedData, Long> {

    public ExtractedDataDao() {
        super(ExtractedData.class);
    }

    public List<ExtractedData> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<ExtractedData> query = session.createQuery(
                "FROM ExtractedData WHERE sessionId = :sessionId", ExtractedData.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<ExtractedData> findBySessionIdAndRuleId(Long sessionId, Long ruleId) {
        return executeInSession(session -> {
            Query<ExtractedData> query = session.createQuery(
                "FROM ExtractedData WHERE sessionId = :sessionId AND ruleId = :ruleId", ExtractedData.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("ruleId", ruleId);
            return query.list();
        });
    }

    public List<ExtractedData> findBySessionIdAndPageId(Long sessionId, Long pageId) {
        return executeInSession(session -> {
            Query<ExtractedData> query = session.createQuery(
                "FROM ExtractedData WHERE sessionId = :sessionId AND pageId = :pageId", ExtractedData.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("pageId", pageId);
            return query.list();
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(e) FROM ExtractedData e WHERE e.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }
}
