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

    public void incrementTotalPages(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET totalPages = totalPages + 1 WHERE id = :id")
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void incrementTotalFlows(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET totalFlows = totalFlows + 1 WHERE id = :id")
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void incrementTotalDownloaded(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET totalDownloaded = totalDownloaded + 1 WHERE id = :id")
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void incrementTotalExternalUrls(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET totalExternalUrls = totalExternalUrls + 1 WHERE id = :id")
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void markCompleted(Long sessionId, Integer totalPages, Integer totalDownloaded) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET status = :status, endTime = :endTime, totalPages = :totalPages, totalDownloaded = :totalDownloaded WHERE id = :id")
                .setParameter("status", CrawlSession.CrawlStatus.COMPLETED)
                .setParameter("endTime", java.time.LocalDateTime.now())
                .setParameter("totalPages", totalPages)
                .setParameter("totalDownloaded", totalDownloaded)
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void markFailed(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET status = :status, endTime = :endTime WHERE id = :id")
                .setParameter("status", CrawlSession.CrawlStatus.FAILED)
                .setParameter("endTime", java.time.LocalDateTime.now())
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void updateStatus(Long sessionId, CrawlSession.CrawlStatus status) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET status = :status WHERE id = :id")
                .setParameter("status", status)
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }

    public void markStopped(Long sessionId) {
        executeInTransaction(session -> {
            session.createQuery("UPDATE CrawlSession SET status = :status, endTime = :endTime WHERE id = :id")
                .setParameter("status", CrawlSession.CrawlStatus.STOPPED)
                .setParameter("endTime", java.time.LocalDateTime.now())
                .setParameter("id", sessionId)
                .executeUpdate();
        });
    }
}
