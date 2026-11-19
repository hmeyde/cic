package com.seheli.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.seheli.web.ParcoursDemande.Etape;
import com.seheli.web.ParcoursDemande.Statut;

class ParcoursDemandeTest {

    @Test
    void sur_la_page_du_compte_aucune_etape_n_est_un_lien() {
        ParcoursDemande parcours = ParcoursDemande.pour(EtapeParcours.COMPTE, false, false, false);

        assertThat(parcours.numero()).isEqualTo(1);
        assertThat(parcours.total()).isEqualTo(4);
        assertThat(parcours.etapes()).extracting(Etape::statut)
                .containsExactly(Statut.COURANTE, Statut.A_FAIRE, Statut.A_FAIRE, Statut.A_FAIRE);
        assertThat(parcours.etapes()).extracting(Etape::lien).containsOnlyNulls();
    }

    @Test
    void les_etapes_enregistrees_sont_faites_et_permettent_d_y_revenir() {
        ParcoursDemande parcours = ParcoursDemande.pour(EtapeParcours.JUSTIFICATIFS, true, true, false);

        assertThat(parcours.nom()).isEqualTo("JUSTIFICATIFS");
        assertThat(parcours.etapes()).extracting(Etape::statut)
                .containsExactly(Statut.FAITE, Statut.FAITE, Statut.COURANTE, Statut.A_FAIRE);
        assertThat(parcours.etapes()).extracting(Etape::lien)
                .containsExactly(null, "/client/emploi", null, "/client/catalogue");
        assertThat(parcours.faite("EMPLOI")).isTrue();
        assertThat(parcours.faite("JUSTIFICATIFS")).isFalse();
    }

    @Test
    void une_etape_sautee_reste_a_faire() {
        ParcoursDemande parcours = ParcoursDemande.pour(EtapeParcours.DEMANDE, true, false, true);

        assertThat(parcours.etapes()).extracting(Etape::statut)
                .containsExactly(Statut.FAITE, Statut.A_FAIRE, Statut.FAITE, Statut.COURANTE);
        assertThat(parcours.faite("EMPLOI")).isFalse();
    }
}
