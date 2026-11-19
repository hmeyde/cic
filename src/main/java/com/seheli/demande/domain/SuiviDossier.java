package com.seheli.demande.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Où en est un dossier, vu par le client : statut, date depuis laquelle il l'a, avancement de chaque
 * jalon du parcours et prochaine étape à faire.
 *
 * <p>Pour un dossier ACCEPTEE, la date limite de l'apport et de l'autorisation est la date
 * d'acceptation plus le délai paramétré ({@code delai.apport.autorisation.jours}, 7 jours) ; au-delà
 * le dossier passe ANNULEE.
 *
 * <p>La frise compte huit jalons ({@link EtapeDossier}), comme un suivi de livraison : ceux déjà
 * franchis sont FAITE, le jalon courant est EN_COURS (EN_ALERTE en retard ou en recouvrement), les
 * suivants A_VENIR. Un dossier refusé ou annulé est ARRETEE au jalon qu'il n'a pas atteint ; un
 * dossier soldé a tous ses jalons FAITE.
 */
public record SuiviDossier(Demande demande, LocalDate statutDepuis, List<EtapeSuivie> etapes,
        ProchaineEtape prochaine, Optional<LocalDate> dateLimite, int delaiApportJours) {

    public SuiviDossier {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(statutDepuis, "statutDepuis");
        etapes = List.copyOf(etapes);
        Objects.requireNonNull(prochaine, "prochaine");
        Objects.requireNonNull(dateLimite, "dateLimite");
        if (delaiApportJours < 1) {
            throw new IllegalArgumentException("Délai d'apport invalide : " + delaiApportJours);
        }
    }

    public static SuiviDossier de(Demande demande, ZoneId fuseau, int delaiApportJours) {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(fuseau, "fuseau");
        if (delaiApportJours < 1) {
            throw new IllegalArgumentException("Délai d'apport invalide : " + delaiApportJours);
        }
        StatutDossier statut = demande.statut();
        LocalDate depuis = LocalDate.ofInstant(demande.statutChangeLe(), fuseau);
        Optional<LocalDate> limite = statut == StatutDossier.ACCEPTEE
                ? Optional.of(depuis.plusDays(delaiApportJours))
                : Optional.empty();
        return new SuiviDossier(demande, depuis, etapes(statut), ProchaineEtape.de(statut), limite,
                delaiApportJours);
    }

    static List<EtapeSuivie> etapes(StatutDossier statut) {
        EtapeDossier courante = EtapeDossier.de(statut);
        EtatEtape etatCourante = switch (statut) {
            case REFUSEE, ANNULEE -> EtatEtape.ARRETEE;
            case SOLDEE -> EtatEtape.FAITE;
            case EN_RETARD, RECOUVREMENT -> EtatEtape.EN_ALERTE;
            default -> EtatEtape.EN_COURS;
        };
        return Arrays.stream(EtapeDossier.values()).map(e -> {
            int ordre = Integer.compare(e.ordinal(), courante.ordinal());
            EtatEtape etat = ordre < 0 ? EtatEtape.FAITE : ordre == 0 ? etatCourante : EtatEtape.A_VENIR;
            return new EtapeSuivie(e, etat);
        }).toList();
    }

    public StatutDossier statut() {
        return demande.statut();
    }

    /** Jalon mis en avant dans la frise. */
    public EtapeDossier courante() {
        return EtapeDossier.de(statut());
    }

    /**
     * Jours pleins restant avant la date limite de l'apport et de l'autorisation (0 le dernier jour
     * et au-delà) ; vide si le dossier n'attend pas son apport.
     */
    public Optional<Long> joursRestants(LocalDate aujourdhui) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        return dateLimite.map(l -> Math.max(0, ChronoUnit.DAYS.between(aujourdhui, l)));
    }

    /** Part du délai déjà écoulée, de 0 à 100, pour la barre du compte à rebours ; vide hors ACCEPTEE. */
    public Optional<Integer> pourcentageDelaiEcoule(LocalDate aujourdhui) {
        return joursRestants(aujourdhui).map(r -> (int) Math.min(100,
                Math.round(100.0 * (delaiApportJours - r) / delaiApportJours)));
    }
}
