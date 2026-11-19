package com.seheli.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.seheli.web.MenuBackOffice.Entree;
import com.seheli.web.MenuBackOffice.Lien;

class MenuBackOfficeTest {

    private static List<Entree> entrees(MenuBackOffice menu) {
        return menu.groupes().stream().flatMap(g -> g.liens().stream()).map(Lien::entree).toList();
    }

    private static List<Entree> actives(MenuBackOffice menu) {
        return menu.groupes().stream().flatMap(g -> g.liens().stream()).filter(Lien::actif).map(Lien::entree)
                .toList();
    }

    @Test
    void l_analyste_voit_l_analyse_et_rien_du_recouvrement_ni_du_pilotage() {
        MenuBackOffice menu = MenuBackOffice.pour("/bo/dossiers", "analyste", List.of("ANALYSTE"), false).orElseThrow();
        assertThat(entrees(menu)).containsExactly(Entree.ACCUEIL, Entree.DOSSIERS, Entree.JUSTIFICATIFS,
                Entree.AUTORISATIONS, Entree.COMMANDES);
        assertThat(actives(menu)).containsExactly(Entree.DOSSIERS);
    }

    @Test
    void la_fiche_d_un_dossier_active_la_file() {
        MenuBackOffice menu = MenuBackOffice.pour("/bo/dossiers/1b2c", "analyste", List.of("ANALYSTE"), false)
                .orElseThrow();
        assertThat(actives(menu)).containsExactly(Entree.DOSSIERS);
    }

    @Test
    void l_import_des_paiements_l_emporte_sur_les_paiements() {
        MenuBackOffice menu = MenuBackOffice.pour("/bo/paiements/import", "reco", List.of("RECOUVREMENT"), false)
                .orElseThrow();
        assertThat(entrees(menu)).containsExactly(Entree.ACCUEIL, Entree.RECOUVREMENT, Entree.PAIEMENTS,
                Entree.IMPORT_PAIEMENTS);
        assertThat(actives(menu)).containsExactly(Entree.IMPORT_PAIEMENTS);
    }

    @Test
    void l_accueil_n_est_actif_que_sur_bo() {
        assertThat(actives(MenuBackOffice.pour("/bo/", "a", List.of("ASSOCIE"), false).orElseThrow()))
                .containsExactly(Entree.ACCUEIL);
        assertThat(entrees(MenuBackOffice.pour("/bo", "a", List.of("ASSOCIE"), false).orElseThrow()))
                .containsExactly(Entree.ACCUEIL, Entree.PILOTAGE, Entree.CAPITAL, Entree.INSCRIPTIONS);
    }

    @Test
    void la_reinitialisation_de_la_demo_seulement_en_profil_demo() {
        assertThat(entrees(MenuBackOffice.pour("/bo", "admin", List.of("ADMIN"), false).orElseThrow()))
                .doesNotContain(Entree.DEMONSTRATION, Entree.DOSSIERS, Entree.AUTORISATIONS);
        assertThat(entrees(MenuBackOffice.pour("/bo", "admin", List.of("ADMIN"), true).orElseThrow()))
                .contains(Entree.DEMONSTRATION);
    }

    @Test
    void pas_de_menu_hors_du_back_office_ou_sur_la_connexion() {
        assertThat(MenuBackOffice.pour("/bo/login", "x", List.of("ADMIN"), false)).isEmpty();
        assertThat(MenuBackOffice.pour("/client", "x", List.of("ADMIN"), false)).isEmpty();
        assertThat(MenuBackOffice.pour("/bo", "x", List.of("CLIENT"), false)).isEmpty();
        assertThat(MenuBackOffice.pour("/bofoo", "x", List.of("ADMIN"), false)).isEmpty();
    }
}
