package com.cic.paiement.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;
import com.cic.paiement.domain.Rattachement;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.PaiementEnDoubleException;
import com.cic.paiement.domain.PaiementRepository;
import com.cic.paiement.domain.ReferencePaiement;

/** Comme la base : la référence est unique (contrainte paiement_reference_uk). */
public class PaiementsEnMemoire implements PaiementRepository {

    private final Map<ReferencePaiement, Paiement> parReference = new LinkedHashMap<>();

    @Override
    public void enregistrer(Paiement paiement) {
        if (parReference.putIfAbsent(paiement.reference(), paiement) != null) {
            throw new PaiementEnDoubleException(null);
        }
    }

    @Override
    public Optional<Paiement> parId(UUID id) {
        return parReference.values().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Paiement> parReference(ReferencePaiement reference) {
        return Optional.ofNullable(parReference.get(reference));
    }

    @Override
    public List<Paiement> derniers(int limite) {
        List<Paiement> tous = new ArrayList<>(parReference.values());
        tous.sort(Comparator.comparing(Paiement::saisiLe).reversed());
        return tous.stream().limit(limite).toList();
    }

    @Override
    public List<Paiement> ecarts(int limite) {
        List<Paiement> avecEcart = new ArrayList<>(parReference.values().stream().filter(p -> p.ecart().isPresent())
                .toList());
        avecEcart.sort(Comparator.comparing(Paiement::recuLe).thenComparing(Paiement::saisiLe).reversed());
        return avecEcart.stream().limit(limite).toList();
    }

    @Override
    public Montant totalApportRecuPourDossier(UUID demandeId) {
        return parReference.values().stream()
                .filter(p -> p.rattachement().filter(Rattachement::estApport).map(Rattachement::demandeId)
                        .filter(demandeId::equals).isPresent())
                .map(Paiement::montant).reduce(Montant.ZERO, Montant::plus);
    }

    public List<Paiement> tous() {
        return List.copyOf(parReference.values());
    }
}
