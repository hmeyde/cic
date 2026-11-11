package com.cic.parametrage.application;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cic.commun.domain.Fraction;
import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.ParametreInvalideException;
import com.cic.parametrage.domain.Parametres;

/**
 * Lecture typée des paramètres métier.
 *
 * <p>Les paramètres sont chargés au démarrage : s'il en manque un ou qu'une valeur est
 * invalide, l'application ne démarre pas. Ils sont ensuite rechargés périodiquement
 * ({@code cic.parametres.rechargement}, 5 minutes par défaut) pour prendre en compte une
 * modification en base sans redéploiement ; un rechargement invalide est journalisé et
 * l'ancien jeu de paramètres reste en vigueur.
 */
@Service
public class ParametreService implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ParametreService.class);

    private final ChargementParametres chargement;
    private final AtomicReference<Parametres> courants = new AtomicReference<>();

    public ParametreService(ChargementParametres chargement) {
        this.chargement = chargement;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Parametres parametres = charger();
        log.info("{} paramètres métier chargés", parametres.taille());
    }

    /** Relit la table ; en cas d'erreur l'ancien jeu est conservé et l'erreur propagée. */
    public Parametres recharger() {
        return charger();
    }

    @Scheduled(initialDelayString = "${cic.parametres.rechargement:PT5M}",
               fixedDelayString = "${cic.parametres.rechargement:PT5M}")
    void rechargerPeriodiquement() {
        try {
            charger();
        } catch (RuntimeException e) {
            log.error("Rechargement des paramètres refusé, les valeurs précédentes restent en vigueur : {}",
                    e.getMessage());
        }
    }

    private Parametres charger() {
        Parametres parametres = Parametres.de(chargement.chargerTous());
        courants.set(parametres);
        return parametres;
    }

    /** Jeu de paramètres courant, cohérent pour toute la durée d'un calcul. */
    public Parametres courants() {
        Parametres parametres = courants.get();
        if (parametres == null) {
            throw new ParametreInvalideException("Paramètres pas encore chargés");
        }
        return parametres;
    }

    public int entier(CleParametre cle) {
        return courants().entier(cle);
    }

    public BigDecimal pourcentage(CleParametre cle) {
        return courants().pourcentage(cle);
    }

    public Fraction fraction(CleParametre cle) {
        return courants().fraction(cle);
    }

    public String texte(CleParametre cle) {
        return courants().texte(cle);
    }
}
