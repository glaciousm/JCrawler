package com.jcrawler.repository;

import com.jcrawler.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    List<Page> findBySessionId(Long sessionId);

    List<Page> findBySessionIdAndDepthLevel(Long sessionId, Integer depthLevel);

    Optional<Page> findBySessionIdAndUrl(Long sessionId, String url);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.sessionId = :sessionId")
    Long countBySessionId(Long sessionId);

    @Query("SELECT p FROM Page p WHERE p.sessionId = :sessionId AND p.processed = false")
    List<Page> findUnprocessedPages(Long sessionId);
}
