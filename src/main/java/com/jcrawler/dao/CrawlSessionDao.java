package com.jcrawler.dao;

import com.jcrawler.model.CrawlSession;
import org.hibernate.query.Query;

import java.util.List;

public class CrawlSessionDao extends BaseDao<CrawlSession, Long> {

    public CrawlSessionDao() {
        super(CrawlSession.class);
    }

    public List<CrawlSession> findByStatus(CrawlSession.CrawlStatus status) {
        return executeInSession(session -> {
            Query<CrawlSession> query = session.createQuery(
                "FROM CrawlSession WHERE status = :status", CrawlSession.class);
            query.setParameter("status", status);
            return query.list();
        });
    }

    public List<CrawlSession> findByBaseDomain(String baseDomain) {
        return executeInSession(session -> {
            Query<CrawlSession> query = session.createQuery(
                "FROM CrawlSession WHERE baseDomain = :baseDomain", CrawlSession.class);
            query.setParameter("baseDomain", baseDomain);
            return query.list();
        });
    }
}
