package com.seheli.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** L'application démarre sur PostgreSQL et Flyway applique toutes les migrations. */
class DemarrageBaseIntegrationTest extends TestIntegration {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void laBaseEstPostgresqlEtMigree() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL");

        Integer echecs = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);
        Integer migrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version is not null", Integer.class);
        assertThat(echecs).isZero();
        assertThat(migrations).isPositive();
    }

    @Test
    void lesParametresDeGestionSontCharges() {
        Integer parametres = jdbc.queryForObject("select count(*) from parametre", Integer.class);
        assertThat(parametres).isPositive();
    }
}
