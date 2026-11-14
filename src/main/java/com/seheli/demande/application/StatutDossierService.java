package com.seheli.demande.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.DemandeRepository;
import com.seheli.demande.domain.StatutDossier;

/** Lecture du statut d'un dossier par les autres modules (par exemple les rappels du recouvrement). */
@Service
public class StatutDossierService {

    private final DemandeRepository demandes;

    public StatutDossierService(DemandeRepository demandes) {
        this.demandes = demandes;
    }

    /** Statut courant du dossier ; vide s'il est inconnu. */
    @Transactional(readOnly = true)
    public Optional<StatutDossier> statut(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return demandes.parId(demandeId).map(Demande::statut);
    }
}
