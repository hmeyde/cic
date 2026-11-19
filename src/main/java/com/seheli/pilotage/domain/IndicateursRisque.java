package com.seheli.pilotage.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.seheli.commun.domain.Montant;

/**
 * Indicateurs de risque du tableau de bord des associés et alertes sur les seuils de CLAUDE.md.
 *
 * <ul>
 *   <li><b>Retard à 30 jours</b> : dossiers dont la plus ancienne échéance EN_RETARD a au moins
 *       {@code provision.palier1.jours} jours, sans avoir atteint la perte. Leur restant dû est
 *       provisionné à 50 %, puis à 100 % à partir de {@code provision.palier2.jours}.</li>
 *   <li><b>Perte</b> : dossiers en retard depuis au moins {@code perte.retard.jours} ; tout leur
 *       restant dû est compté en perte (et non plus en provision).</li>
 *   <li><b>Prélèvements réussis</b> : échéances échues payées / échéances échues (payées ou
 *       EN_RETARD). Pas de taux tant qu'aucune échéance n'est échue.</li>
 *   <li><b>Utilisation du capital</b> : encours / capital disponible, le capital disponible étant le
 *       capital apporté plus le bénéfice réalisé.</li>
 *   <li><b>Concentration</b> : répartition de l'encours par employeur (nom comparé
 *       sans tenir compte de la casse ni des espaces autour) et par fournisseur, du plus gros au plus
 *       petit ; la plus forte part est comparée au seuil.</li>
 * </ul>
 *
 * <p>Une alerte est levée quand un seuil est dépassé strictement (au moins un dossier pour les retards).
 * Les parts sont en % arrondis au dixième (HALF_UP) ; les montants à l'ouguiya.
 */
public record IndicateursRisque(int dossiersRetard30, Montant restantDuRetard30, Montant provisions,
        int dossiersPerte, Montant perte, int prelevementsReussis, int prelevementsEchoues,
        Optional<BigDecimal> tauxPrelevementsReussisPct, Montant capitalDisponible,
        Optional<BigDecimal> utilisationCapitalPct, List<Concentration> repartitionEmployeurs,
        List<Concentration> repartitionFournisseurs, SeuilsRisque seuils, List<TypeAlerte> alertes) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public IndicateursRisque {
        Objects.requireNonNull(restantDuRetard30, "restantDuRetard30");
        Objects.requireNonNull(provisions, "provisions");
        Objects.requireNonNull(perte, "perte");
        Objects.requireNonNull(tauxPrelevementsReussisPct, "tauxPrelevementsReussisPct");
        Objects.requireNonNull(capitalDisponible, "capitalDisponible");
        Objects.requireNonNull(utilisationCapitalPct, "utilisationCapitalPct");
        repartitionEmployeurs = List.copyOf(repartitionEmployeurs);
        repartitionFournisseurs = List.copyOf(repartitionFournisseurs);
        Objects.requireNonNull(seuils, "seuils");
        alertes = List.copyOf(alertes);
    }

    /**
     * @param positions           dossiers qui portent de l'encours
     * @param prelevementsReussis échéances échues payées
     * @param prelevementsEchoues échéances échues EN_RETARD
     * @param capitalDisponible   capital apporté plus bénéfice réalisé
     */
    public static IndicateursRisque calculer(List<PositionDossier> positions, int prelevementsReussis,
            int prelevementsEchoues, Montant capitalDisponible, SeuilsRisque seuils, LocalDate aujourdhui) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(capitalDisponible, "capitalDisponible");
        Objects.requireNonNull(seuils, "seuils");
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        if (prelevementsReussis < 0 || prelevementsEchoues < 0) {
            throw new IllegalArgumentException("Nombre de prélèvements négatif");
        }

        int dossiersRetard30 = 0;
        int dossiersPerte = 0;
        Montant restantDuRetard30 = Montant.ZERO;
        Montant provisions = Montant.ZERO;
        Montant perte = Montant.ZERO;
        Montant encours = Montant.ZERO;
        for (PositionDossier p : positions) {
            encours = encours.plus(p.restantDu());
            if (p.plusAncienRetard().isEmpty()) {
                continue;
            }
            long jours = ChronoUnit.DAYS.between(p.plusAncienRetard().get(), aujourdhui);
            if (jours >= seuils.perteJours()) {
                dossiersPerte++;
                perte = perte.plus(p.restantDu());
            } else if (jours >= seuils.provisionPalier1Jours()) {
                dossiersRetard30++;
                restantDuRetard30 = restantDuRetard30.plus(p.restantDu());
                BigDecimal taux = jours >= seuils.provisionPalier2Jours() ? seuils.provisionPalier2Pct()
                        : seuils.provisionPalier1Pct();
                provisions = provisions.plus(p.restantDu().pourcentage(taux));
            }
        }

        int echus = prelevementsReussis + prelevementsEchoues;
        Optional<BigDecimal> tauxReussis = echus == 0 ? Optional.empty()
                : Optional.of(part(BigDecimal.valueOf(prelevementsReussis), BigDecimal.valueOf(echus)));
        Optional<BigDecimal> utilisation = capitalDisponible.estZero() ? Optional.empty()
                : Optional.of(part(encours.valeur(), capitalDisponible.valeur()));
        List<Concentration> employeurs = repartition(positions, encours,
                p -> p.employeur().map(String::strip).filter(e -> !e.isEmpty()));
        List<Concentration> fournisseurs = repartition(positions, encours, p -> Optional.of(p.fournisseur()));
        Optional<Concentration> employeur = employeurs.stream().findFirst();
        Optional<Concentration> fournisseur = fournisseurs.stream().findFirst();

        List<TypeAlerte> alertes = new ArrayList<>();
        if (dossiersRetard30 > 0) {
            alertes.add(TypeAlerte.RETARD_30_JOURS);
        }
        if (dossiersPerte > 0) {
            alertes.add(TypeAlerte.PERTE);
        }
        if (tauxReussis.filter(t -> t.compareTo(seuils.prelevementsReussisMinPct()) < 0).isPresent()) {
            alertes.add(TypeAlerte.PRELEVEMENTS_REUSSIS);
        }
        boolean capitalDepasse = capitalDisponible.estZero() ? !encours.estZero()
                : utilisation.get().compareTo(seuils.encoursCapitalMaxPct()) > 0;
        if (capitalDepasse) {
            alertes.add(TypeAlerte.UTILISATION_CAPITAL);
        }
        if (employeur.filter(c -> c.partPct().compareTo(seuils.employeurMaxPct()) > 0).isPresent()) {
            alertes.add(TypeAlerte.CONCENTRATION_EMPLOYEUR);
        }
        if (fournisseur.filter(c -> c.partPct().compareTo(seuils.fournisseurMaxPct()) > 0).isPresent()) {
            alertes.add(TypeAlerte.CONCENTRATION_FOURNISSEUR);
        }

        return new IndicateursRisque(dossiersRetard30, restantDuRetard30, provisions, dossiersPerte, perte,
                prelevementsReussis, prelevementsEchoues, tauxReussis, capitalDisponible, utilisation, employeurs,
                fournisseurs, seuils, alertes);
    }

    public boolean aDesAlertes() {
        return !alertes.isEmpty();
    }

    /** Au moins une alerte appelle une décision immédiate (voir {@link TypeAlerte#critique()}). */
    public boolean aUneAlerteCritique() {
        return alertes.stream().anyMatch(TypeAlerte::critique);
    }

    public boolean alerte(TypeAlerte type) {
        return alertes.contains(type);
    }

    /** Pour les gabarits : {@code enAlerte('PERTE')}. */
    public boolean enAlerte(String type) {
        return alerte(TypeAlerte.valueOf(type));
    }

    /** Plus forte concentration de l'encours sur un même employeur ; vide s'il n'y a pas d'encours. */
    public Optional<Concentration> employeurPrincipal() {
        return repartitionEmployeurs.stream().findFirst();
    }

    /** Plus forte concentration de l'encours sur un même fournisseur ; vide s'il n'y a pas d'encours. */
    public Optional<Concentration> fournisseurPrincipal() {
        return repartitionFournisseurs.stream().findFirst();
    }

    /**
     * Regroupe l'encours par libellé (casse et espaces ignorés ; vide = non renseigné), du plus gros
     * groupe au plus petit ; à égalité, le premier rencontré d'abord. Vide s'il n'y a pas d'encours.
     */
    private static List<Concentration> repartition(List<PositionDossier> positions, Montant encours,
            Function<PositionDossier, Optional<String>> libelle) {
        if (encours.estZero()) {
            return List.of();
        }
        Map<String, Optional<String>> libelles = new LinkedHashMap<>();
        Map<String, Montant> montants = new LinkedHashMap<>();
        for (PositionDossier p : positions) {
            Optional<String> l = libelle.apply(p);
            String cle = l.map(s -> s.strip().toLowerCase(Locale.ROOT)).orElse("");
            libelles.putIfAbsent(cle, l);
            montants.merge(cle, p.restantDu(), Montant::plus);
        }
        // Tri stable : à égalité, l'ordre de première rencontre est conservé.
        return montants.entrySet().stream()
                .sorted(Map.Entry.<String, Montant>comparingByValue(Comparator.naturalOrder()).reversed())
                .map(e -> new Concentration(libelles.get(e.getKey()), e.getValue(),
                        part(e.getValue().valeur(), encours.valeur())))
                .toList();
    }

    /** {@code numerateur / denominateur} en %, arrondi au dixième. */
    private static BigDecimal part(BigDecimal numerateur, BigDecimal denominateur) {
        return numerateur.multiply(CENT).divide(denominateur, 1, RoundingMode.HALF_UP);
    }
}
