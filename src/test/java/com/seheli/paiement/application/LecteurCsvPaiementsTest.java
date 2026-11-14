package com.seheli.paiement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class LecteurCsvPaiementsTest {

    private static List<LigneCsv> lire(String texte) {
        return LecteurCsvPaiements.lire(texte.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void lit_un_fichier_avec_en_tete_point_virgule_et_bom() {
        List<LigneCsv> lignes = lire("﻿Référence;Montant;Date\r\nAP20260000011;8 320;2026-11-12\r\n\r\n"
                + "\"AP20260000029\";\"12480,00\";13/11/2026\r\n");

        assertThat(lignes).extracting(LigneCsv::numero).containsExactly(2, 4);
        assertThat(lignes.get(0).saisie()).isEqualTo(new SaisiePaiement("AP20260000011", "8 320", "2026-11-12"));
        assertThat(lignes.get(1).saisie()).isEqualTo(new SaisiePaiement("AP20260000029", "12480", "2026-11-13"));
        assertThat(lignes).allSatisfy(l -> assertThat(l.erreur()).isEmpty());
    }

    @Test
    void lit_un_fichier_sans_en_tete_separe_par_des_virgules() {
        List<LigneCsv> lignes = lire("AP20260000011,8320.00,1/2/2026,\nXYZ,100,2026-11-01");

        assertThat(lignes).extracting(LigneCsv::saisie).containsExactly(
                new SaisiePaiement("AP20260000011", "8320", "2026-02-01"), new SaisiePaiement("XYZ", "100", "2026-11-01"));
    }

    @Test
    void une_ligne_sans_trois_colonnes_est_signalee() {
        List<LigneCsv> lignes = lire("reference;montant;date\nAP20260000011;8320\nAP1;1;2026-11-01;en trop");

        assertThat(lignes).allSatisfy(l -> assertThat(l.erreur()).contains(LecteurCsvPaiements.COLONNES_INVALIDES));
        assertThat(lignes.getFirst().saisie().reference()).isEqualTo("AP20260000011");
    }

    @Test
    void un_fichier_vide_ou_sans_ligne_de_paiement_est_refuse() {
        assertThatThrownBy(() -> LecteurCsvPaiements.lire(new byte[0])).isInstanceOf(FichierCsvInvalideException.class)
                .extracting("code").isEqualTo(FichierCsvInvalideException.VIDE);
        assertThatThrownBy(() -> lire("reference;montant;date\n\n")).extracting("code")
                .isEqualTo(FichierCsvInvalideException.VIDE);
    }

    @Test
    void un_fichier_trop_long_ou_trop_gros_est_refuse() {
        String ligne = "AP20260000011;8320;2026-11-12\n";
        assertThatThrownBy(() -> lire(ligne.repeat(LecteurCsvPaiements.LIGNES_MAX + 1))).extracting("code")
                .isEqualTo(FichierCsvInvalideException.TROP_DE_LIGNES);
        assertThat(lire(ligne.repeat(LecteurCsvPaiements.LIGNES_MAX))).hasSize(LecteurCsvPaiements.LIGNES_MAX);
        assertThatThrownBy(() -> LecteurCsvPaiements.lire(new byte[LecteurCsvPaiements.TAILLE_MAX_OCTETS + 1]))
                .extracting("code").isEqualTo(FichierCsvInvalideException.TROP_GROS);
    }
}
