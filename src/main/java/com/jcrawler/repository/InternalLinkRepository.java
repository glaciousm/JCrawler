package com.jcrawler.repository;

import com.jcrawler.model.InternalLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalLinkRepository extends JpaRepository<InternalLink, Long> {
    List<InternalLink> findBySessionId(Long sessionId);
    List<InternalLink> findByFoundOnPage(String foundOnPage);
    List<InternalLink> findBySessionIdAndFoundOnPage(Long sessionId, String foundOnPage);
    long countBySessionId(Long sessionId);
}
