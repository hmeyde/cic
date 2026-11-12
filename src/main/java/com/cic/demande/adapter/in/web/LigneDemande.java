package com.cic.demande.adapter.in.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.TermesDemande;

/**
 * Demande telle que le client la voit : produit, conditions et statut. Ni le score, ni le prix
 * d'achat ne lui sont montrés.
 */
public record LigneDemande(UUID id, String designation, LocalDate soumiseLe, StatutDossier statut, Montant prixVente,
        Montant apport, int nombreMois, Montant mensualite, Montant derniereMensualite) {

    static LigneDemande depuis(Demande d, ZoneId fuseau) {
        TermesDemande t = d.termes();
        return new LigneDemande(d.id(), t.designationProduit(), LocalDate.ofInstant(d.soumiseLe(), fuseau),
                d.statut(), t.prixVente(), t.apport(), t.nombreMois(), t.mensualite(), t.derniereMensualite());
    }

    public boolean derniereDiffere() {
        return derniereMensualite.compareTo(mensualite) != 0;
    }
}
