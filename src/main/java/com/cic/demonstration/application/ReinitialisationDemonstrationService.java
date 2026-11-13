package com.cic.demonstration.application;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.cic.demonstration.domain.BaseDeDemonstration;
import com.cic.demonstration.domain.InitialisationsDemarrage;
import com.cic.demonstration.domain.ReinitialisationEnCoursException;
import com.cic.parametrage.application.ParametreService;

/**
 * Profil demo : ramène la démonstration à son état initial, comme au premier démarrage sur une base vide.
 * La base est remise à neuf (migrations rejouées, donc paramètres de départ), les paramètres sont relus, les
 * initialisations de démarrage des modules sont rejouées, puis les données de démonstration sont rechargées.
 * Une seule réinitialisation à la fois.
 */
@Service
@Profile("demo")
public class ReinitialisationDemonstrationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReinitialisationDemonstrationService.class);

    private final BaseDeDemonstration base;
    private final ParametreService parametres;
    private final InitialisationsDemarrage initialisations;
    private final DonneesDemonstrationService donnees;
    private final AtomicBoolean enCours = new AtomicBoolean();

    ReinitialisationDemonstrationService(BaseDeDemonstration base, ParametreService parametres,
            InitialisationsDemarrage initialisations, DonneesDemonstrationService donnees) {
        this.base = base;
        this.parametres = parametres;
        this.initialisations = initialisations;
        this.donnees = donnees;
    }

    /**
     * @return {@code true} si les données de démonstration ont été rechargées
     * @throws ReinitialisationEnCoursException si une réinitialisation est déjà en cours
     */
    public boolean reinitialiser() {
        if (!enCours.compareAndSet(false, true)) {
            throw new ReinitialisationEnCoursException();
        }
        try {
            LOG.warn("Réinitialisation de la démonstration : toutes les données sont effacées");
            base.remettreANeuf();
            parametres.recharger();
            initialisations.rejouer();
            boolean chargees = donnees.chargerSiAbsentes();
            LOG.info("Démonstration réinitialisée");
            return chargees;
        } finally {
            enCours.set(false);
        }
    }
}
