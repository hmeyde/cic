package com.seheli.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OngletClientTest {

    @Test
    void l_accueil_de_l_espace_client() {
        assertThat(OngletClient.pour("/client")).contains(OngletClient.ACCUEIL);
        assertThat(OngletClient.pour("/client/")).contains(OngletClient.ACCUEIL);
    }

    @Test
    void le_catalogue_et_ses_produits() {
        assertThat(OngletClient.pour("/client/catalogue")).contains(OngletClient.CATALOGUE);
        assertThat(OngletClient.pour("/client/catalogue/42")).contains(OngletClient.CATALOGUE);
    }

    @Test
    void le_dossier_et_les_demandes() {
        assertThat(OngletClient.pour("/client/dossier")).contains(OngletClient.DOSSIER);
        assertThat(OngletClient.pour("/client/demandes")).contains(OngletClient.DOSSIER);
    }

    @Test
    void le_profil_regroupe_emploi_et_justificatifs() {
        assertThat(OngletClient.pour("/client/profil")).contains(OngletClient.PROFIL);
        assertThat(OngletClient.pour("/client/emploi")).contains(OngletClient.PROFIL);
        assertThat(OngletClient.pour("/client/justificatifs")).contains(OngletClient.PROFIL);
    }

    @Test
    void pas_de_barre_hors_de_l_espace_client_connecte() {
        assertThat(OngletClient.pour("/client/connexion")).isEmpty();
        assertThat(OngletClient.pour("/client/connexion/code")).isEmpty();
        assertThat(OngletClient.pour("/")).isEmpty();
        assertThat(OngletClient.pour("/bo/dossiers")).isEmpty();
        assertThat(OngletClient.pour("/clientele")).isEmpty();
        assertThat(OngletClient.pour("/client/catalogues")).isEmpty();
        assertThat(OngletClient.pour(null)).isEmpty();
    }
}
