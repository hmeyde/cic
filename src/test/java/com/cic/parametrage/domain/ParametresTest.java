package com.cic.parametrage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.cic.commun.domain.Fraction;
import com.cic.parametrage.ParametresDeDepart;

class ParametresTest {

    @Test
    void lit_les_valeurs_de_depart_typees() {
        Parametres p = Parametres.de(ParametresDeDepart.lignes());

        assertThat(p.taille()).isEqualTo(CleParametre.values().length);
        assertThat(p.pourcentage(CleParametre.APPORT_MINIMUM_PCT)).isEqualByComparingTo("40");
        assertThat(p.pourcentage(CleParametre.MARGE_CIC_DEFAUT_PCT)).isEqualByComparingTo("4");
        assertThat(p.entier(CleParametre.DUREE_MAX_MOIS)).isEqualTo(6);
        assertThat(p.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS)).isEqualTo(7);
        assertThat(p.fraction(CleParametre.ENDETTEMENT_MAX_FRACTION)).isEqualTo(new Fraction(1, 3));
        assertThat(p.texte(CleParametre.SCORE_GRILLE_VERSION)).isEqualTo("v1");
    }

    @Test
    void ignore_les_cles_inconnues() {
        List<Parametre> lignes = new ArrayList<>(ParametresDeDepart.lignes());
        lignes.add(new Parametre("cle.obsolete", "1", TypeParametre.ENTIER));

        assertThat(Parametres.de(lignes).taille()).isEqualTo(CleParametre.values().length);
    }

    @Test
    void refuse_un_jeu_incomplet_en_listant_les_cles_manquantes() {
        List<Parametre> lignes = ParametresDeDepart.lignes().stream()
                .filter(p -> !p.cle().startsWith("duree."))
                .toList();

        assertThatThrownBy(() -> Parametres.de(lignes))
                .isInstanceOf(ParametreInvalideException.class)
                .hasMessageContaining("duree.min.mois manquant")
                .hasMessageContaining("duree.max.mois manquant");
    }

    @Test
    void refuse_un_type_different_de_celui_attendu() {
        List<Parametre> lignes = ParametresDeDepart.lignes().stream()
                .map(p -> p.cle().equals("duree.max.mois") ? new Parametre(p.cle(), "6", TypeParametre.TEXTE) : p)
                .toList();

        assertThatThrownBy(() -> Parametres.de(lignes))
                .isInstanceOf(ParametreInvalideException.class)
                .hasMessageContaining("duree.max.mois de type TEXTE au lieu de ENTIER");
    }

    @Test
    void refuse_des_bornes_incoherentes() {
        assertThatThrownBy(() -> Parametres.de(ParametresDeDepart.avec(CleParametre.DUREE_MIN_MOIS, "8")))
                .isInstanceOf(ParametreInvalideException.class)
                .hasMessageContaining("duree.min.mois");
        assertThatThrownBy(() -> Parametres.de(ParametresDeDepart.avec(CleParametre.SCORE_SEUIL_A_EXAMINER, "75")))
                .isInstanceOf(ParametreInvalideException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "ENTIER; abc",
            "ENTIER; -1",
            "ENTIER; 2.5",
            "POURCENTAGE; 101",
            "POURCENTAGE; -0.5",
            "POURCENTAGE; 4%",
            "FRACTION; 0.33",
            "FRACTION; 1/0",
            "FRACTION; 1/3/4"
    })
    void refuse_une_valeur_non_conforme_a_son_type(TypeParametre type, String valeur) {
        assertThatThrownBy(() -> new Parametre("une.cle", valeur, type))
                .isInstanceOf(ParametreInvalideException.class)
                .hasMessageContaining("une.cle");
    }

    @Test
    void refuse_une_valeur_vide() {
        assertThatThrownBy(() -> new Parametre("une.cle", "  ", TypeParametre.TEXTE))
                .isInstanceOf(ParametreInvalideException.class);
    }

    @Test
    void accepte_un_pourcentage_decimal_et_retire_les_espaces() {
        Parametre p = new Parametre("une.cle", " 7.5 ", TypeParametre.POURCENTAGE);
        assertThat(p.valeur()).isEqualTo("7.5");
        assertThat(new BigDecimal(p.valeur())).isEqualByComparingTo("7.5");
    }

    @Test
    void refuse_de_lire_une_cle_avec_le_mauvais_accesseur() {
        Parametres p = Parametres.de(ParametresDeDepart.lignes());
        assertThatThrownBy(() -> p.entier(CleParametre.APPORT_MINIMUM_PCT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chaque_cle_est_inseree_par_la_migration() throws Exception {
        StringBuilder migrations = new StringBuilder();
        for (String fichier : new String[] {"V2__parametre_regles_de_gestion.sql", "V7__code_connexion_client.sql",
                "V15__autorisation_prelevement.sql", "V18__masrvi_code_marchand.sql", "V24__relance.sql"}) {
            try (var in = getClass().getResourceAsStream("/db/migration/" + fichier)) {
                assertThat(in).as(fichier).isNotNull();
                migrations.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        String migration = migrations.toString();
        for (CleParametre cle : CleParametre.values()) {
            assertThat(migration).contains("('" + cle.cle() + "',");
        }
    }
}
