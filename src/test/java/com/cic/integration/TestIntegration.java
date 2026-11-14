package com.cic.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Classe de base des tests d'intégration : application complète (profil par défaut, dev)
 * sur une vraie base PostgreSQL migrée par Flyway.
 *
 * <p>La base est partagée par tous les tests d'intégration de la JVM et le contexte Spring
 * est mis en cache : un test ne doit pas supposer une base vide, il crée ses propres données.
 */
@SpringBootTest
public abstract class TestIntegration {

    private static final BaseDeTest BASE = BaseDeTest.demarrer();

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry proprietes) {
        proprietes.add("spring.datasource.url", BASE::url);
        proprietes.add("spring.datasource.username", BASE::utilisateur);
        proprietes.add("spring.datasource.password", BASE::motDePasse);
        if (BASE.schema() != null) {
            proprietes.add("spring.flyway.schemas", BASE::schema);
            proprietes.add("spring.jpa.properties.hibernate.default_schema", BASE::schema);
        }
    }
}
