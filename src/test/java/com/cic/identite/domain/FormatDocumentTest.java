package com.cic.identite.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FormatDocumentTest {

    static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    @Test
    void reconnait_le_format_a_la_signature_du_contenu() {
        assertThat(FormatDocument.detecter(PDF)).contains(FormatDocument.PDF);
        assertThat(FormatDocument.detecter(JPEG)).contains(FormatDocument.JPEG);
        assertThat(FormatDocument.detecter(PNG)).contains(FormatDocument.PNG);
    }

    @Test
    void refuse_les_autres_contenus() {
        assertThat(FormatDocument.detecter("<html><script>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(FormatDocument.detecter("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(FormatDocument.detecter(new byte[] {'%', 'P', 'D'})).isEmpty();
        assertThat(FormatDocument.detecter(new byte[0])).isEmpty();
        assertThat(FormatDocument.detecter(null)).isEmpty();
    }

    @Test
    void donne_le_type_mime_et_l_extension() {
        assertThat(FormatDocument.PDF.typeMime()).isEqualTo("application/pdf");
        assertThat(FormatDocument.JPEG.extension()).isEqualTo("jpg");
        assertThat(FormatDocument.PNG.typeMime()).isEqualTo("image/png");
    }
}
