package com.jcrawler.repository;

import com.jcrawler.model.ExtractedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedDataRepository extends JpaRepository<ExtractedData, Long> {

    List<ExtractedData> findBySessionId(Long sessionId);

    List<ExtractedData> findBySessionIdAndRuleId(Long sessionId, Long ruleId);

    List<ExtractedData> findBySessionIdAndPageId(Long sessionId, Long pageId);

    @Query("SELECT COUNT(e) FROM ExtractedData e WHERE e.sessionId = :sessionId")
    Long countBySessionId(Long sessionId);
}
