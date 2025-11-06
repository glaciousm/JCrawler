package com.jcrawler.repository;

import com.jcrawler.model.ExtractionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractionRuleRepository extends JpaRepository<ExtractionRule, Long> {

    List<ExtractionRule> findBySessionId(Long sessionId);

    List<ExtractionRule> findBySessionIdAndEnabled(Long sessionId, Boolean enabled);
}
