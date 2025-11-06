package com.jcrawler.repository;

import com.jcrawler.model.ExternalUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalUrlRepository extends JpaRepository<ExternalUrl, Long> {
    List<ExternalUrl> findBySessionId(Long sessionId);
    long countBySessionId(Long sessionId);
}
