package com.seheli.paiement.application;

import java.util.List;

/** Résultat de l'import d'un fichier CSV de paiements, ligne par ligne, dans l'ordre du fichier. */
public record ResultatImport(List<LigneImportee> lignes) {

    public ResultatImport {
        lignes = List.copyOf(lignes);
    }

    public long nombre(IssueLigne issue) {
        return lignes.stream().filter(l -> l.issue() == issue).count();
    }

    /** Paiements enregistrés par cet import, avec ou sans écart. */
    public long enregistres() {
        return nombre(IssueLigne.CONFORME) + nombre(IssueLigne.MONTANT_DIFFERENT)
                + nombre(IssueLigne.REFERENCE_INCONNUE);
    }

    /** Lignes enregistrées avec un écart : montant différent ou référence inconnue. */
    public List<LigneImportee> ecarts() {
        return lignes.stream().filter(l -> l.issue().estUnEcart()).toList();
    }

    /** Lignes non enregistrées : déjà enregistrées ou invalides. */
    public List<LigneImportee> ignorees() {
        return lignes.stream().filter(l -> !l.issue().estUnEcart() && l.issue() != IssueLigne.CONFORME).toList();
    }
}
