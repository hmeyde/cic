package com.seheli.identite.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.seheli.identite.domain.RoleBackOffice;

/**
 * Profils dev et demo : crée au démarrage un compte ADMIN, un compte ANALYSTE, un compte RECOUVREMENT et un
 * compte ASSOCIE s'ils n'existent pas.
 * Identifiants et mots de passe viennent de la configuration ({@code seheli.back-office.admin-dev.*},
 * {@code seheli.back-office.analyste-dev.*}, {@code seheli.back-office.recouvrement-dev.*},
 * {@code seheli.back-office.associe-dev.*}), surchargeables par
 * variables d'environnement. Les mots de passe ne sont jamais journalisés.
 */
@Component
@Profile({"dev", "demo"})
class ComptesBackOfficeDev implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ComptesBackOfficeDev.class);

    private final CompteBackOfficeService comptes;
    private final String admin;
    private final String motDePasseAdmin;
    private final String analyste;
    private final String motDePasseAnalyste;
    private final String recouvrement;
    private final String motDePasseRecouvrement;
    private final String associe;
    private final String motDePasseAssocie;

    ComptesBackOfficeDev(CompteBackOfficeService comptes,
            @Value("${seheli.back-office.admin-dev.identifiant}") String admin,
            @Value("${seheli.back-office.admin-dev.mot-de-passe}") String motDePasseAdmin,
            @Value("${seheli.back-office.analyste-dev.identifiant}") String analyste,
            @Value("${seheli.back-office.analyste-dev.mot-de-passe}") String motDePasseAnalyste,
            @Value("${seheli.back-office.recouvrement-dev.identifiant}") String recouvrement,
            @Value("${seheli.back-office.recouvrement-dev.mot-de-passe}") String motDePasseRecouvrement,
            @Value("${seheli.back-office.associe-dev.identifiant}") String associe,
            @Value("${seheli.back-office.associe-dev.mot-de-passe}") String motDePasseAssocie) {
        this.comptes = comptes;
        this.admin = admin;
        this.motDePasseAdmin = motDePasseAdmin;
        this.analyste = analyste;
        this.motDePasseAnalyste = motDePasseAnalyste;
        this.recouvrement = recouvrement;
        this.motDePasseRecouvrement = motDePasseRecouvrement;
        this.associe = associe;
        this.motDePasseAssocie = motDePasseAssocie;
    }

    @Override
    public void run(ApplicationArguments args) {
        creerSiAbsent(admin, motDePasseAdmin, RoleBackOffice.ADMIN);
        creerSiAbsent(analyste, motDePasseAnalyste, RoleBackOffice.ANALYSTE);
        creerSiAbsent(recouvrement, motDePasseRecouvrement, RoleBackOffice.RECOUVREMENT);
        creerSiAbsent(associe, motDePasseAssocie, RoleBackOffice.ASSOCIE);
    }

    private void creerSiAbsent(String identifiant, String motDePasse, RoleBackOffice role) {
        if (comptes.creerSiAbsent(identifiant, motDePasse, role)) {
            LOG.info("Compte {} du back-office créé au démarrage", role);
        }
    }
}
