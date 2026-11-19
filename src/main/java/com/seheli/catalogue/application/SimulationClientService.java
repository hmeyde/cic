package com.seheli.catalogue.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.application.SimulationService;
import com.seheli.contrat.domain.Prix;
import com.seheli.contrat.domain.SituationPaie;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/**
 * Simulation de la page produit pour le client connecté : durée, apport choisi au curseur (en %
 * du prix de vente, de {@code apport.minimum.pct} à {@code apport.simulation.max.pct}) et
 * échéancier daté au jour de paie déclaré dans son emploi, à partir d'aujourd'hui (horloge injectée).
 */
@Service
public class SimulationClientService {

    private final CatalogueClientService catalogue;
    private final SimulationService simulation;
    private final EmploiClientService emplois;
    private final ParametreService parametres;
    private final Clock clock;

    public SimulationClientService(CatalogueClientService catalogue, SimulationService simulation,
            EmploiClientService emplois, ParametreService parametres, Clock clock) {
        this.catalogue = catalogue;
        this.simulation = simulation;
        this.emplois = emplois;
        this.parametres = parametres;
        this.clock = clock;
    }

    /**
     * @param apportPct apport souhaité en % du prix de vente ; absent ou hors bornes, il est ramené
     *                  dans les bornes (minimum par défaut)
     * @return vide si le produit n'est pas proposé au client
     * @throws IllegalArgumentException si la durée sort des bornes en vigueur
     */
    @Transactional(readOnly = true)
    public Optional<SimulationClient> simuler(UUID produitId, UUID clientId, int nombreMois, Integer apportPct) {
        Objects.requireNonNull(produitId, "produitId");
        Objects.requireNonNull(clientId, "clientId");
        Parametres p = parametres.courants();
        int min = p.pourcentage(CleParametre.APPORT_MINIMUM_PCT).intValue();
        int max = Math.max(min, p.pourcentage(CleParametre.APPORT_SIMULATION_MAX_PCT).intValue());
        int retenu = apportPct == null ? min : Math.clamp(apportPct, min, max);

        Optional<SituationPaie> paie = emplois.parClient(clientId)
                .map(e -> new SituationPaie(e.salaireNet(), e.jourPaie(), e.dateFin()));
        LocalDate aujourdhui = LocalDate.now(clock);
        return catalogue.conditions(produitId).map(conditions -> {
            Prix prix = conditions.prix();
            // Le pourcentage minimum redonne exactement l'apport minimum du prix (même calcul).
            var apport = retenu == min ? prix.apportMinimum() : prix.prixVente().pourcentage(BigDecimal.valueOf(retenu));
            return new SimulationClient(simulation.detailler(prix, nombreMois, apport, aujourdhui, paie),
                    retenu, min, max);
        });
    }
}
