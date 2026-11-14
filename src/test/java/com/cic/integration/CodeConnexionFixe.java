package com.cic.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.cic.identite.domain.GenerateurCode;

/**
 * Tests d'intégration : le code de connexion envoyé au client vaut toujours {@value #CODE}, pour que
 * le test puisse le saisir comme le ferait le client qui l'a reçu par SMS. Le reste du parcours
 * (stockage haché, expiration, essais, usage unique) reste celui de l'application.
 */
@TestConfiguration(proxyBeanMethods = false)
class CodeConnexionFixe {

    static final String CODE = "424242";

    @Bean
    @Primary
    GenerateurCode generateurCodeFixe() {
        return () -> CODE;
    }
}
