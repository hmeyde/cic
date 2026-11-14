package com.seheli.identite.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formats de justificatif acceptés : PDF, JPEG, PNG.
 *
 * <p>Le format est reconnu à la signature des premiers octets du fichier, jamais à son nom ni au type
 * annoncé par le navigateur, qui sont fournis par le client.
 */
public enum FormatDocument {

    PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
    JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String typeMime;
    private final String extension;
    private final byte[] signature;

    FormatDocument(String typeMime, String extension, byte[] signature) {
        this.typeMime = typeMime;
        this.extension = extension;
        this.signature = signature;
    }

    /** Format dont la signature ouvre ce contenu ; vide si le contenu n'est ni PDF, ni JPEG, ni PNG. */
    public static Optional<FormatDocument> detecter(byte[] contenu) {
        if (contenu == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(f -> f.ouvre(contenu)).findFirst();
    }

    private boolean ouvre(byte[] contenu) {
        return contenu.length >= signature.length
                && Arrays.equals(contenu, 0, signature.length, signature, 0, signature.length);
    }

    public String typeMime() {
        return typeMime;
    }

    public String extension() {
        return extension;
    }
}
