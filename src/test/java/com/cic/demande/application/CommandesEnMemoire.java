package com.cic.demande.application;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.CommandeFournisseurRepository;

/** Comme la base : au plus une commande par dossier (contrainte commande_fournisseur_demande_uk). */
public class CommandesEnMemoire implements CommandeFournisseurRepository {

    private final Map<UUID, CommandeFournisseur> parDemande = new LinkedHashMap<>();
    private long ordre;

    @Override
    public void enregistrer(CommandeFournisseur commande) {
        if (parDemande.putIfAbsent(commande.demandeId(), commande) != null) {
            throw new IllegalStateException("commande_fournisseur_demande_uk");
        }
    }

    @Override
    public Optional<CommandeFournisseur> parDemande(UUID demandeId) {
        return Optional.ofNullable(parDemande.get(demandeId));
    }

    @Override
    public List<CommandeFournisseur> dernieres(int limite) {
        return parDemande.values().stream().sorted(Comparator.comparing(CommandeFournisseur::valideLe).reversed())
                .limit(limite).toList();
    }

    @Override
    public long prochainNumeroOrdre() {
        return ++ordre;
    }

    public List<CommandeFournisseur> toutes() {
        return List.copyOf(parDemande.values());
    }
}
