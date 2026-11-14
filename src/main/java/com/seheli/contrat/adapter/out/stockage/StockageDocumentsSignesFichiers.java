package com.seheli.contrat.adapter.out.stockage;

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

import com.seheli.contrat.domain.StockageDocumentsSignes;

/**
 * Autorisations signées rangées sur disque, dans un répertoire hors de l'application et de ses
 * ressources statiques ({@code seheli.autorisations.repertoire}, variable
 * {@code CIC_AUTORISATIONS_REPERTOIRE}). Un fichier par document, nommé par son identifiant (UUID) :
 * ni nom d'origine, ni donnée personnelle. Écriture atomique (fichier temporaire puis renommage) ;
 * répertoire et fichiers réservés au compte qui exécute l'application quand le système le permet.
 */
@Component
class StockageDocumentsSignesFichiers implements StockageDocumentsSignes {

    private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private final Path racine;

    StockageDocumentsSignesFichiers(@Value("${seheli.autorisations.repertoire}") String repertoire) {
        this.racine = Path.of(Objects.requireNonNull(repertoire, "repertoire")).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            if (POSIX) {
                Files.setPosixFilePermissions(racine, PosixFilePermissions.fromString("rwx------"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Répertoire des autorisations signées inutilisable", e);
        }
    }

    @Override
    public void ecrire(UUID documentId, byte[] contenu) {
        Path cible = chemin(documentId);
        Path temporaire = null;
        try {
            temporaire = Files.createTempFile(racine, "depot-", ".tmp");
            Files.write(temporaire, contenu);
            if (POSIX) {
                Files.setPosixFilePermissions(temporaire, PosixFilePermissions.fromString("rw-------"));
            }
            Files.move(temporaire, cible, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            supprimerSilencieusement(temporaire);
            throw new UncheckedIOException("Écriture de l'autorisation signée " + documentId + " impossible", e);
        }
    }

    @Override
    public byte[] lire(UUID documentId) {
        try {
            return Files.readAllBytes(chemin(documentId));
        } catch (IOException e) {
            throw new IllegalStateException("Autorisation signée " + documentId + " illisible", e);
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
            // fichier orphelin sans conséquence : il n'est référencé par aucune autorisation
        }
    }
}
