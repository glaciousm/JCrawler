package com.jcrawler.dao;

import com.jcrawler.model.ExtractionRule;
import org.hibernate.query.Query;

import java.util.List;

public class ExtractionRuleDao extends BaseDao<ExtractionRule, Long> {

    public ExtractionRuleDao() {
        super(ExtractionRule.class);
    }

    public List<ExtractionRule> findBySessionId(Long sessionId) {
        return executeInSession(session -> {
            Query<ExtractionRule> query = session.createQuery(
                "FROM ExtractionRule WHERE sessionId = :sessionId", ExtractionRule.class);
            query.setParameter("sessionId", sessionId);
            return query.list();
        });
    }

    public List<ExtractionRule> findBySessionIdAndEnabled(Long sessionId, Boolean enabled) {
        return executeInSession(session -> {
            Query<ExtractionRule> query = session.createQuery(
                "FROM ExtractionRule WHERE sessionId = :sessionId AND enabled = :enabled", ExtractionRule.class);
            query.setParameter("sessionId", sessionId);
            query.setParameter("enabled", enabled);
            return query.list();
        });
    }
}
