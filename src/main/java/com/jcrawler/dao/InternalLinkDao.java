package com.jcrawler.dao;

import com.jcrawler.model.InternalLink;
import org.hibernate.query.Query;

import java.util.List;

public class InternalLinkDao extends BaseDao<InternalLink, Long> {

    public InternalLinkDao() {
        super(InternalLink.class);
    }

    public List<InternalLink> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<InternalLink> query = session.createQuery(
                "FROM InternalLink WHERE sessionId = :sessionId", InternalLink.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<InternalLink> findByFoundOnPage(String foundOnPage) {
        return executeInSession(session -> {
            Query<InternalLink> query = session.createQuery(
                "FROM InternalLink WHERE foundOnPage = :foundOnPage", InternalLink.class);
            query.setParameter("foundOnPage", foundOnPage);
            return query.list();
        });
    }

    public List<InternalLink> findBySessionIdAndFoundOnPage(Long sessionId, String foundOnPage) {
        return executeInSession(session -> {
            Query<InternalLink> query = session.createQuery(
                "FROM InternalLink WHERE sessionId = :sessionId AND foundOnPage = :foundOnPage",
                InternalLink.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("foundOnPage", foundOnPage);
            return query.list();
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(l) FROM InternalLink l WHERE l.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }
}
