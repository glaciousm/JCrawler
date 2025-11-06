package com.jcrawler.repository;

import com.jcrawler.model.NavigationFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavigationFlowRepository extends JpaRepository<NavigationFlow, Long> {

    List<NavigationFlow> findBySessionId(Long sessionId);

    List<NavigationFlow> findBySessionIdAndDepth(Long sessionId, Integer depth);

    @Query("SELECT COUNT(f) FROM NavigationFlow f WHERE f.sessionId = :sessionId")
    Long countBySessionId(Long sessionId);
}
