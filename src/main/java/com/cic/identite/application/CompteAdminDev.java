package com.cic.identite.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cic.identite.domain.RoleBackOffice;

/**
 * Profil dev uniquement : crée au démarrage un compte ADMIN s'il n'existe pas.
 * Identifiant et mot de passe viennent de la configuration ({@code cic.back-office.admin-dev.*}),
 * surchargeables par variables d'environnement. Le mot de passe n'est jamais journalisé.
 */
@Component
@Profile("dev")
class CompteAdminDev implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CompteAdminDev.class);

    private final CompteBackOfficeService comptes;
    private final String identifiant;
    private final String motDePasse;

    CompteAdminDev(CompteBackOfficeService comptes,
            @Value("${cic.back-office.admin-dev.identifiant}") String identifiant,
            @Value("${cic.back-office.admin-dev.mot-de-passe}") String motDePasse) {
        this.comptes = comptes;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (comptes.creerSiAbsent(identifiant, motDePasse, RoleBackOffice.ADMIN)) {
            LOG.info("Compte ADMIN de développement créé");
        }
    }
}
