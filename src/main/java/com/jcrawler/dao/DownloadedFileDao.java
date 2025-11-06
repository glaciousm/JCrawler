package com.jcrawler.dao;

import com.jcrawler.model.DownloadedFile;
import org.hibernate.query.Query;

import java.util.List;

public class DownloadedFileDao extends BaseDao<DownloadedFile, Long> {

    public DownloadedFileDao() {
        super(DownloadedFile.class);
    }

    public List<DownloadedFile> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<DownloadedFile> query = session.createQuery(
                "FROM DownloadedFile WHERE sessionId = :sessionId", DownloadedFile.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<DownloadedFile> findBySessionIdAndDownloadSuccess(Long sessionId, Boolean downloadSuccess) {
        return executeInSession(session -> {
            Query<DownloadedFile> query = session.createQuery(
                "FROM DownloadedFile WHERE sessionId = :sessionId AND downloadSuccess = :downloadSuccess",
                DownloadedFile.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("downloadSuccess", downloadSuccess);
            return query.list();
        });
    }

    public Long countBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(f) FROM DownloadedFile f WHERE f.sessionId = :sessionId", Long.class);
            query.setParameter("sessionId", sessionId);
            return query.uniqueResult();
        });
    }
}
