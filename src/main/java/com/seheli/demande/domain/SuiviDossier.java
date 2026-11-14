package com.seheli.demande.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Où en est un dossier, vu par le client : statut, date depuis laquelle il l'a, avancement de chaque
 * étape du parcours et prochaine étape à faire.
 *
 * <p>Pour un dossier ACCEPTEE, la date limite de l'apport et de l'autorisation est la date
 * d'acceptation plus le délai paramétré ({@code delai.apport.autorisation.jours}, 7 jours) ; au-delà
 * le dossier passe ANNULEE.
 */
public record SuiviDossier(Demande demande, LocalDate statutDepuis, List<EtapeSuivie> etapes,
        ProchaineEtape prochaine, Optional<LocalDate> dateLimite) {

    public SuiviDossier {
        Objects.requireNonNull(demande, "demande");
        Objects.requireNonNull(statutDepuis, "statutDepuis");
        etapes = List.copyOf(etapes);
        Objects.requireNonNull(prochaine, "prochaine");
        Objects.requireNonNull(dateLimite, "dateLimite");
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
        return new SuiviDossier(demande, depuis, etapes(statut), ProchaineEtape.de(statut), limite);
    }

    static List<EtapeSuivie> etapes(StatutDossier statut) {
        EtapeDossier courante = EtapeDossier.de(statut);
        EtatEtape etatCourante = switch (statut) {
            case REFUSEE, ANNULEE -> EtatEtape.ARRETEE;
            case SOLDEE -> EtatEtape.FAITE;
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
}
