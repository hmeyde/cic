package com.seheli.demande.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStream;

/** Vérifie que la première page porte le logo Seheli (image PNG de 1248 × 384 px). */
final class LogoPdfAssertions {

    private LogoPdfAssertions() {
    }

    static void premierePagePorteLeLogo(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfDictionary xobjets = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT);
            assertThat(xobjets).as("images de la page 1").isNotNull();
            boolean logo = xobjets.getKeys().stream()
                    .map(cle -> (PdfStream) PdfReader.getPdfObject(xobjets.get(cle)))
                    .anyMatch(image -> PdfName.IMAGE.equals(image.getAsName(PdfName.SUBTYPE))
                            && image.getAsNumber(PdfName.WIDTH).intValue() == 1248
                            && image.getAsNumber(PdfName.HEIGHT).intValue() == 384);
            assertThat(logo).as("logo Seheli en page 1").isTrue();
        } finally {
            reader.close();
        }
    }
}
