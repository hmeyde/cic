package com.seheli.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EtapeParcoursTest {

    @Test
    void chaque_page_du_parcours_a_son_etape() {
        assertThat(EtapeParcours.pour("/client/connexion/compte")).contains(EtapeParcours.COMPTE);
        assertThat(EtapeParcours.pour("/client/emploi")).contains(EtapeParcours.EMPLOI);
        assertThat(EtapeParcours.pour("/client/justificatifs/")).contains(EtapeParcours.JUSTIFICATIFS);
        assertThat(EtapeParcours.pour("/client/catalogue")).contains(EtapeParcours.DEMANDE);
        assertThat(EtapeParcours.pour("/client/catalogue/0b6e4f00-0000-0000-0000-000000000001"))
                .contains(EtapeParcours.DEMANDE);
        assertThat(EtapeParcours.pour("/client/catalogue/0b6e4f00-0000-0000-0000-000000000001/demande"))
                .contains(EtapeParcours.DEMANDE);
    }

    @Test
    void les_autres_pages_sont_hors_du_parcours() {
        assertThat(EtapeParcours.pour(null)).isEmpty();
        assertThat(EtapeParcours.pour("/client")).isEmpty();
        assertThat(EtapeParcours.pour("/client/connexion")).isEmpty();
        assertThat(EtapeParcours.pour("/client/dossier")).isEmpty();
        assertThat(EtapeParcours.pour("/client/justificatifs/0b6e4f00-0000-0000-0000-000000000001")).isEmpty();
        assertThat(EtapeParcours.pour("/client/catalogue/0b6e4f00-0000-0000-0000-000000000001/simulation")).isEmpty();
        assertThat(EtapeParcours.pour("/bo/dossiers")).isEmpty();
    }

    @Test
    void les_etapes_sont_numerotees_de_un_a_quatre() {
        assertThat(EtapeParcours.COMPTE.numero()).isEqualTo(1);
        assertThat(EtapeParcours.DEMANDE.numero()).isEqualTo(4);
    }
}
