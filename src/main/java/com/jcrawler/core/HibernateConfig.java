package com.jcrawler.core;

import com.jcrawler.model.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

@Slf4j
public class HibernateConfig {

    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                log.info("Initializing Hibernate SessionFactory...");

                Configuration configuration = new Configuration();

                // Database connection settings
                Properties properties = new Properties();
                properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
                properties.setProperty("hibernate.connection.url", "jdbc:h2:file:./data/jcrawler;AUTO_SERVER=TRUE");
                properties.setProperty("hibernate.connection.username", "sa");
                properties.setProperty("hibernate.connection.password", "");

                // Hibernate settings
                properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                properties.setProperty("hibernate.hbm2ddl.auto", "update");
                properties.setProperty("hibernate.show_sql", "false");
                properties.setProperty("hibernate.format_sql", "true");
                properties.setProperty("hibernate.jdbc.time_zone", "UTC");

                // Connection pool settings (HikariCP)
                properties.setProperty("hibernate.connection.provider_class",
                    "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
                properties.setProperty("hibernate.hikari.minimumIdle", "5");
                properties.setProperty("hibernate.hikari.maximumPoolSize", "20");
                properties.setProperty("hibernate.hikari.idleTimeout", "300000");

                // Performance settings
                properties.setProperty("hibernate.jdbc.batch_size", "20");
                properties.setProperty("hibernate.order_inserts", "true");
                properties.setProperty("hibernate.order_updates", "true");
                properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");

                configuration.setProperties(properties);

                // Add annotated entity classes
                configuration.addAnnotatedClass(CrawlSession.class);
                configuration.addAnnotatedClass(Page.class);
                configuration.addAnnotatedClass(NavigationFlow.class);
                configuration.addAnnotatedClass(ExtractionRule.class);
                configuration.addAnnotatedClass(ExtractedData.class);
                configuration.addAnnotatedClass(DownloadedFile.class);
                configuration.addAnnotatedClass(ExternalUrl.class);
                configuration.addAnnotatedClass(InternalLink.class);

                sessionFactory = configuration.buildSessionFactory();
                log.info("Hibernate SessionFactory initialized successfully");

            } catch (Exception e) {
                log.error("Failed to create SessionFactory", e);
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            log.info("Closing Hibernate SessionFactory...");
            sessionFactory.close();
        }
    }
}
