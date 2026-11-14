package com.seheli.paiement.adapter.in.web;

import java.time.Clock;
import java.util.List;

import com.seheli.paiement.application.IssueLigne;
import com.seheli.paiement.application.ResultatImport;

/** Bilan d'un import CSV affiché au back-office : compteurs, écarts, lignes non enregistrées. */
record VueResultatImport(int lignes, long enregistres, long conformes, long dejaEnregistres, long invalides,
        List<VueLigneImportee> ecarts, List<VueLigneImportee> ignorees) {

    static VueResultatImport depuis(ResultatImport r, Clock clock) {
        return new VueResultatImport(r.lignes().size(), r.enregistres(), r.nombre(IssueLigne.CONFORME),
                r.nombre(IssueLigne.DEJA_ENREGISTRE), r.nombre(IssueLigne.INVALIDE),
                r.ecarts().stream().map(l -> VueLigneImportee.depuis(l, clock)).toList(),
                r.ignorees().stream().map(l -> VueLigneImportee.depuis(l, clock)).toList());
    }
}
