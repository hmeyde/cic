package com.cic.identite.adapter.out.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StockageDocumentsFichiersTest {

    @TempDir
    Path dossier;

    @Test
    void ecrit_lit_et_supprime_un_contenu_nomme_par_l_identifiant() throws IOException {
        Path racine = dossier.resolve("justificatifs");
        StockageDocumentsFichiers stockage = new StockageDocumentsFichiers(racine.toString());
        UUID id = UUID.randomUUID();
        byte[] contenu = {1, 2, 3};

        stockage.ecrire(id, contenu);

        assertThat(stockage.lire(id)).isEqualTo(contenu);
        try (var fichiers = Files.list(racine)) {
            assertThat(fichiers.map(p -> p.getFileName().toString())).containsExactly(id.toString());
        }

        stockage.supprimer(id);
        assertThatThrownBy(() -> stockage.lire(id)).isInstanceOf(IllegalStateException.class);
        stockage.supprimer(id);
    }
}
