package com.cic.contrat.application;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratRepository;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.EcheanceAPrelever;
import com.cic.contrat.domain.StatutEcheance;

public class ContratsEnMemoire implements ContratRepository {

    private final Map<UUID, Contrat> parDemande = new LinkedHashMap<>();
    private long ordre;

    @Override
    public void enregistrer(Contrat contrat) {
        if (parDemande.putIfAbsent(contrat.demandeId(), contrat) != null) {
            throw new IllegalStateException("contrat_demande_uk");
        }
    }

    @Override
    public Optional<Contrat> parId(UUID id) {
        return parDemande.values().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Contrat> parDemande(UUID demandeId) {
        return Optional.ofNullable(parDemande.get(demandeId));
    }

    @Override
    public Optional<Contrat> parNumero(String numero) {
        return parDemande.values().stream().filter(c -> c.numero().equals(numero)).findFirst();
    }

    @Override
    public void enregistrerStatut(UUID contratId, Echeance echeance) {
        Contrat c = parId(contratId).orElseThrow();
        parDemande.put(c.demandeId(), Contrat.reconstituer(c.id(), c.numero(), c.demandeId(), c.clientId(),
                c.produitId(), c.fournisseurId(), c.designationProduit(), c.prix(), c.apport(),
                c.echeances().stream().map(e -> e.id().equals(echeance.id()) ? echeance : e).toList(),
                c.etabliLe()));
    }

    @Override
    public List<EcheanceAPrelever> echeancesAVenirEntre(LocalDate du, LocalDate au) {
        return parDemande.values().stream()
                .flatMap(c -> c.echeances().stream()
                        .filter(e -> e.statut() == StatutEcheance.A_VENIR && !e.date().isBefore(du)
                                && !e.date().isAfter(au))
                        .map(e -> EcheanceAPrelever.de(c, e)))
                .sorted(Comparator.comparing((EcheanceAPrelever e) -> e.echeance().date())
                        .thenComparing(EcheanceAPrelever::numeroContrat))
                .toList();
    }

    @Override
    public long prochainNumeroOrdre() {
        return ++ordre;
    }

    public List<Contrat> tous() {
        return List.copyOf(parDemande.values());
    }
}
