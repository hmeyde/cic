package com.seheli.identite.adapter.out.stockage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.seheli.identite.domain.StockageDocuments;

/**
 * Justificatifs rangés sur disque, dans un répertoire hors de l'application et de ses ressources
 * statiques ({@code seheli.documents.repertoire}, variable {@code CIC_DOCUMENTS_REPERTOIRE}). Un fichier
 * par document, nommé par son identifiant (UUID) : ni nom d'origine, ni donnée personnelle, ni
 * extension. Écriture atomique (fichier temporaire puis renommage) ; répertoire et fichiers réservés
 * au compte qui exécute l'application quand le système de fichiers le permet.
 */
@Component
class StockageDocumentsFichiers implements StockageDocuments {

    private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private final Path racine;

    StockageDocumentsFichiers(@Value("${seheli.documents.repertoire}") String repertoire) {
        this.racine = Path.of(Objects.requireNonNull(repertoire, "repertoire")).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            if (POSIX) {
                Files.setPosixFilePermissions(racine, PosixFilePermissions.fromString("rwx------"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Répertoire des justificatifs inutilisable", e);
        }
    }

    @Override
    public void ecrire(UUID documentId, byte[] contenu) {
        Path cible = chemin(documentId);
        Path temporaire = null;
        try {
            temporaire = Files.createTempFile(racine, "envoi-", ".tmp");
            Files.write(temporaire, contenu);
            if (POSIX) {
                Files.setPosixFilePermissions(temporaire, PosixFilePermissions.fromString("rw-------"));
            }
            Files.move(temporaire, cible, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            supprimerSilencieusement(temporaire);
            throw new UncheckedIOException("Écriture du justificatif " + documentId + " impossible", e);
        }
    }

    @Override
    public byte[] lire(UUID documentId) {
        try {
            return Files.readAllBytes(chemin(documentId));
        } catch (IOException e) {
            throw new IllegalStateException("Contenu du justificatif " + documentId + " illisible", e);
        }
    }

    @Override
    public void supprimer(UUID documentId) {
        supprimerSilencieusement(chemin(documentId));
    }

    /** Le nom vient d'un UUID : aucun séparateur de chemin possible. */
    private Path chemin(UUID documentId) {
        return racine.resolve(Objects.requireNonNull(documentId, "documentId").toString());
    }

    private static void supprimerSilencieusement(Path fichier) {
        if (fichier == null) {
            return;
        }
        try {
            Files.deleteIfExists(fichier);
        } catch (IOException ignoree) {
            // fichier orphelin sans conséquence : il n'est référencé par aucun document
        }
    }
}
