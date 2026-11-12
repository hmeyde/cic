package com.cic.paiement.adapter.in.web;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.paiement.domain.ApportAttendu;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.StatutRapprochement;

/**
 * Paiement tel qu'affiché au back-office : dates à l'heure locale.
 *
 * @param numeroContrat  contrat du dossier rattaché ; vide si non rapproché
 * @param montantAttendu apport fixé au contrat ; vide si non rapproché
 */
record VuePaiement(UUID id, String reference, Montant montant, String recuLe, StatutRapprochement statut,
        Optional<String> numeroContrat, Optional<Montant> montantAttendu, boolean ecartDeMontant, String saisiLe,
        String saisiPar) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static VuePaiement depuis(Paiement p, Clock clock) {
        return new VuePaiement(p.id(), p.reference().valeur(), p.montant(), DATE.format(p.recuLe()), p.statut(),
                p.apportAttendu().map(ApportAttendu::numeroContrat), p.apportAttendu().map(ApportAttendu::montant),
                p.ecartDeMontant(), DATE_HEURE.format(p.saisiLe().atZone(clock.getZone())), p.saisiPar());
    }
}
