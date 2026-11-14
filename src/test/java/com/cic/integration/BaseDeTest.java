package com.cic.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base PostgreSQL des tests d'intégration, démarrée une seule fois par JVM.
 *
 * <p>Par défaut : un conteneur PostgreSQL jetable (Testcontainers), même version majeure
 * que {@code docker-compose.yml}. Si Docker est absent (conteneur de développement sans
 * socket Docker) et qu'une base est fournie par {@code SPRING_DATASOURCE_URL}, les tests
 * utilisent un schéma dédié {@value #SCHEMA_SECOURS}, recréé vide à chaque exécution :
 * le schéma {@code public} de la base de développement n'est jamais touché.
 */
record BaseDeTest(String url, String utilisateur, String motDePasse, String schema) {

    static final String IMAGE = "postgres:17";
    static final String SCHEMA_SECOURS = "tests_integration";

    static BaseDeTest demarrer() {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return conteneur();
        }
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Tests d'intégration : ni Docker (Testcontainers) ni SPRING_DATASOURCE_URL disponible.");
        }
        return schemaDeSecours(url,
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                System.getenv("SPRING_DATASOURCE_PASSWORD"));
    }

    @SuppressWarnings("resource") // arrêté par Testcontainers (Ryuk) à la fin de la JVM
    private static BaseDeTest conteneur() {
        PostgreSQLContainer postgres = new PostgreSQLContainer(IMAGE)
                .withDatabaseName("cic")
                .withUsername("cic")
                .withPassword("cic");
        postgres.start();
        return new BaseDeTest(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), null);
    }

    private static BaseDeTest schemaDeSecours(String url, String utilisateur, String motDePasse) {
        try (Connection connexion = DriverManager.getConnection(url, utilisateur, motDePasse);
             Statement sql = connexion.createStatement()) {
            sql.execute("DROP SCHEMA IF EXISTS " + SCHEMA_SECOURS + " CASCADE");
            sql.execute("CREATE SCHEMA " + SCHEMA_SECOURS);
        } catch (SQLException e) {
            throw new IllegalStateException("Tests d'intégration : base de secours inaccessible.", e);
        }
        String separateur = url.contains("?") ? "&" : "?";
        return new BaseDeTest(url + separateur + "currentSchema=" + SCHEMA_SECOURS,
                utilisateur, motDePasse, SCHEMA_SECOURS);
    }
}
