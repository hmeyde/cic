package com.seheli.paiement.adapter.in.web;

import java.time.Clock;
import java.util.Optional;

import com.seheli.paiement.application.IssueLigne;
import com.seheli.paiement.application.LigneImportee;

/**
 * Ligne d'un fichier CSV importé telle qu'affichée au back-office.
 *
 * <p>Référence, montant et date sont les valeurs lues dans le fichier, avant contrôle.
 *
 * @param paiement   paiement enregistré par la ligne, ou déjà enregistré sous sa référence
 */
record VueLigneImportee(int numero, String reference, String montant, String recuLe, IssueLigne issue,
        Optional<String> erreur, Optional<VuePaiement> paiement) {

    /** Valeur lue affichée telle quelle : tronquée, une ligne mal formée pouvant être très longue. */
    static final int LONGUEUR_AFFICHEE_MAX = 40;

    static VueLigneImportee depuis(LigneImportee l, Clock clock) {
        return new VueLigneImportee(l.numero(), court(l.lue().reference()), court(l.lue().montant()),
                court(l.lue().recuLe()), l.issue(), l.erreur(), l.paiement().map(p -> VuePaiement.depuis(p, clock)));
    }

    private static String court(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur.length() <= LONGUEUR_AFFICHEE_MAX ? valeur : valeur.substring(0, LONGUEUR_AFFICHEE_MAX) + "…";
    }
}
