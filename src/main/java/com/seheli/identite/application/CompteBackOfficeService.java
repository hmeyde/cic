package com.seheli.identite.application;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.RoleBackOffice;
import com.seheli.identite.domain.UtilisateurBackOffice;
import com.seheli.identite.domain.UtilisateurBackOfficeRepository;

/** Création et lecture des comptes du back-office. Le mot de passe est haché (BCrypt) avant stockage. */
@Service
public class CompteBackOfficeService {

    /** Longueur minimale d'un mot de passe. */
    public static final int MOT_DE_PASSE_LONGUEUR_MIN = 8;
    /** BCrypt ignore tout ce qui dépasse 72 octets : on refuse plutôt que de tronquer en silence. */
    public static final int MOT_DE_PASSE_OCTETS_MAX = 72;

    private final UtilisateurBackOfficeRepository repository;
    private final PasswordEncoder encodeur;

    public CompteBackOfficeService(UtilisateurBackOfficeRepository repository, PasswordEncoder encodeur) {
        this.repository = repository;
        this.encodeur = encodeur;
    }

    /**
     * Crée un compte actif.
     *
     * @throws IdentifiantDejaUtiliseException si l'identifiant existe déjà
     * @throws IllegalArgumentException si l'identifiant ou le mot de passe est invalide
     */
    @Transactional
    public UtilisateurBackOffice creer(String identifiant, String motDePasse, RoleBackOffice role) {
        String normalise = UtilisateurBackOffice.normaliser(identifiant);
        verifierMotDePasse(motDePasse);
        if (repository.existe(normalise)) {
            throw new IdentifiantDejaUtiliseException();
        }
        UtilisateurBackOffice utilisateur = UtilisateurBackOffice.nouveau(normalise, encodeur.encode(motDePasse), role);
        repository.enregistrer(utilisateur);
        return utilisateur;
    }

    /** Crée le compte s'il n'existe pas encore ; ne modifie jamais un compte existant. */
    @Transactional
    public boolean creerSiAbsent(String identifiant, String motDePasse, RoleBackOffice role) {
        if (repository.existe(UtilisateurBackOffice.normaliser(identifiant))) {
            return false;
        }
        creer(identifiant, motDePasse, role);
        return true;
    }

    /** Compte correspondant à l'identifiant saisi (insensible à la casse) ; vide si l'identifiant est mal formé. */
    @Transactional(readOnly = true)
    public Optional<UtilisateurBackOffice> parIdentifiant(String identifiant) {
        String normalise;
        try {
            normalise = UtilisateurBackOffice.normaliser(identifiant);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return repository.parIdentifiant(normalise);
    }

    private static void verifierMotDePasse(String motDePasse) {
        Objects.requireNonNull(motDePasse, "motDePasse");
        if (motDePasse.length() < MOT_DE_PASSE_LONGUEUR_MIN) {
            throw new IllegalArgumentException("Mot de passe trop court (min " + MOT_DE_PASSE_LONGUEUR_MIN + ")");
        }
        if (motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MOT_DE_PASSE_OCTETS_MAX) {
            throw new IllegalArgumentException("Mot de passe trop long (max " + MOT_DE_PASSE_OCTETS_MAX + " octets)");
        }
    }
}
