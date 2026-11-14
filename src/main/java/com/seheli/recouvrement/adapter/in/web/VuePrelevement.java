package com.seheli.recouvrement.adapter.in.web;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.recouvrement.application.PrelevementSuivi;

/** Ligne d'un tableau de prélèvements de l'écran du recouvrement. */
record VuePrelevement(UUID demandeId, String numeroContrat, int rang, int nombreEcheances, String date,
        Montant montant, StatutDossier statutDossier) {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static VuePrelevement depuis(PrelevementSuivi p) {
        EcheanceAPrelever e = p.echeance();
        return new VuePrelevement(e.demandeId(), e.numeroContrat(), e.echeance().rang(), e.nombreEcheances(),
                DATE.format(e.echeance().date()), e.echeance().montant(), p.statutDossier());
    }
}
