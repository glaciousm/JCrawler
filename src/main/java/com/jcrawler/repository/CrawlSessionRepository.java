package com.jcrawler.repository;

import com.jcrawler.model.CrawlSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlSessionRepository extends JpaRepository<CrawlSession, Long> {

    List<CrawlSession> findByStatus(CrawlSession.CrawlStatus status);

    List<CrawlSession> findByBaseDomain(String baseDomain);
}
