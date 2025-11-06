package com.jcrawler.repository;

import com.jcrawler.model.DownloadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadedFileRepository extends JpaRepository<DownloadedFile, Long> {

    List<DownloadedFile> findBySessionId(Long sessionId);

    List<DownloadedFile> findBySessionIdAndDownloadSuccess(Long sessionId, Boolean downloadSuccess);

    @Query("SELECT COUNT(f) FROM DownloadedFile f WHERE f.sessionId = :sessionId")
    Long countBySessionId(Long sessionId);
}
