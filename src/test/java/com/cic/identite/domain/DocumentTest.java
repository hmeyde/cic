package com.cic.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DocumentTest {

    private static final Instant RECU = Instant.parse("2026-11-11T09:00:00Z");
    private final UUID clientId = UUID.randomUUID();

    @Test
    void accepte_jusqu_a_cinq_mo() {
        Document d = Document.nouveau(clientId, TypeDocument.RIB, FormatDocument.PDF, 5L * 1024 * 1024, RECU);

        assertThat(d.tailleOctets()).isEqualTo(Document.TAILLE_MAX_OCTETS);
        assertThat(d.appartientA(clientId)).isTrue();
        assertThat(d.appartientA(UUID.randomUUID())).isFalse();
    }

    @Test
    void refuse_un_document_vide_ou_de_plus_de_cinq_mo() {
        assertThatThrownBy(() -> Document.nouveau(clientId, TypeDocument.RIB, FormatDocument.PDF, 0, RECU))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Document.nouveau(clientId, TypeDocument.RIB, FormatDocument.PDF,
                Document.TAILLE_MAX_OCTETS + 1, RECU)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sa_description_ne_contient_que_des_identifiants() {
        Document d = Document.nouveau(clientId, TypeDocument.PIECE_IDENTITE, FormatDocument.PNG, 10, RECU);

        assertThat(d.toString()).contains(d.id().toString(), clientId.toString(), "PIECE_IDENTITE");
    }
}
