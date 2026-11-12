package com.cic.demande.adapter.in.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;

/**
 * Offre telle que le client la voit : numéro du contrat, prix fixé, apport, reste à rembourser et
 * échéancier. Ni le prix d'achat, ni la marge, ni le capital engagé par CIC ne lui sont montrés.
 */
public record OffreClient(String numero, String designation, LocalDate etablieLe, Montant prixTotal,
        Montant apport, Montant resteAFinancer, int nombreMois, List<Echeance> echeances) {

    public OffreClient {
        echeances = List.copyOf(echeances);
    }

    static OffreClient depuis(Contrat c, ZoneId fuseau) {
        return new OffreClient(c.numero(), c.designationProduit(), LocalDate.ofInstant(c.etabliLe(), fuseau),
                c.prix().prixVente(), c.apport(), c.resteAFinancer(), c.nombreMois(), c.echeances());
    }
}
