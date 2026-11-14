package com.seheli.integration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Classe de base des tests d'intégration : application complète (profil par défaut, dev)
 * sur une vraie base PostgreSQL migrée par Flyway.
 *
 * <p>La base est partagée par tous les tests d'intégration de la JVM et le contexte Spring
 * est mis en cache : un test ne doit pas supposer une base vide, il crée ses propres données.
 * Les justificatifs et autorisations signées sont écrits dans un répertoire temporaire, jamais
 * dans celui du poste de développement ; le code de connexion est fixe ({@link CodeConnexionFixe}).
 */
@SpringBootTest
@Import(CodeConnexionFixe.class)
public abstract class TestIntegration {

    private static final BaseDeTest BASE = BaseDeTest.demarrer();
    private static final Path FICHIERS = repertoireTemporaire();

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry proprietes) {
        proprietes.add("spring.datasource.url", BASE::url);
        proprietes.add("spring.datasource.username", BASE::utilisateur);
        proprietes.add("spring.datasource.password", BASE::motDePasse);
        if (BASE.schema() != null) {
            proprietes.add("spring.flyway.schemas", BASE::schema);
            proprietes.add("spring.jpa.properties.hibernate.default_schema", BASE::schema);
        }
        proprietes.add("seheli.documents.repertoire", () -> FICHIERS.resolve("justificatifs").toString());
        proprietes.add("seheli.autorisations.repertoire", () -> FICHIERS.resolve("autorisations").toString());
    }

    private static Path repertoireTemporaire() {
        try {
            return Files.createTempDirectory("seheli-integration-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
