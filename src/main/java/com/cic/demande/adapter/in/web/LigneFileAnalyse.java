package com.cic.demande.adapter.in.web;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.demande.application.DossierEnAnalyse;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.Recommandation;
import com.cic.demande.domain.TermesDemande;

/**
 * Ligne de la file des analystes : rang, ancienneté de la demande, client, produit, conditions et
 * recommandation du score (le score recommande, l'analyste décide).
 *
 * @param attenteJours jours entiers écoulés depuis la soumission
 */
record LigneFileAnalyse(int rang, UUID id, String soumiseLe, long attenteJours, String client, String designation,
        Montant prixVente, Montant apport, int nombreMois, Montant mensualite, boolean eligible, int note,
        Recommandation recommandation) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static LigneFileAnalyse depuis(int rang, DossierEnAnalyse dossier, Instant maintenant, ZoneId fuseau) {
        Demande d = dossier.demande();
        TermesDemande t = d.termes();
        long attente = Math.max(0, Duration.between(d.soumiseLe(), maintenant).toDays());
        return new LigneFileAnalyse(rang, d.id(), DATE.format(d.soumiseLe().atZone(fuseau)), attente,
                dossier.nomClient(), t.designationProduit(), t.prixVente(), t.apport(), t.nombreMois(),
                t.mensualite(), d.score().eligibilite().estEligible(), d.score().note().note(),
                d.score().recommandation());
    }
}
