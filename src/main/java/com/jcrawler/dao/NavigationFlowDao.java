package com.jcrawler.dao;

import com.jcrawler.model.NavigationFlow;
import org.hibernate.query.Query;

import java.util.List;

public class NavigationFlowDao extends BaseDao<NavigationFlow, Long> {

    public NavigationFlowDao() {
        super(NavigationFlow.class);
    }

    public List<NavigationFlow> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<NavigationFlow> query = session.createQuery(
                "FROM NavigationFlow WHERE sessionId = :sessionId", NavigationFlow.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<NavigationFlow> findBySessionIdAndDepth(Long sessionId, Integer depth) {
        return executeInSession(session -> {
            Query<NavigationFlow> query = session.createQuery(
                "FROM NavigationFlow WHERE sessionId = :sessionId AND depth = :depth", NavigationFlow.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("depth", depth);
            return query.list();
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(f) FROM NavigationFlow f WHERE f.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }
}
