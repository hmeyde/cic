package com.cic.demande.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Fraction;
import com.cic.commun.domain.Montant;

/** Exemple chiffré de CLAUDE.md : prix public 20 000, vente 20 800, apport 8 320, 6 × 2 080. */
public final class DemandesFixtures {

    public static final Instant LE = Instant.parse("2026-11-12T10:00:00Z");

    private DemandesFixtures() {
    }

    public static TermesDemande termes() {
        return new TermesDemande(UUID.randomUUID(), UUID.randomUUID(), "Galaxy A35", Montant.de(20_000),
                Montant.de(18_400), Montant.de(20_800), Montant.de(8_320), Montant.de(8_320), 6, Montant.de(2_080),
                Montant.de(2_080));
    }

    public static Score score() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        DossierEligibilite eligibilite = new DossierEligibilite(true, true, LocalDate.of(1990, 5, 1),
                LocalDate.of(2020, 1, 1), Optional.empty(), true, Montant.de(30_000), Montant.ZERO,
                Montant.de(2_080), false);
        DossierScore dossier = new DossierScore(TypeContrat.CDI, LocalDate.of(2020, 1, 1), Optional.empty(),
                LocalDate.of(2027, 5, 25), ModeRemboursement.RETENUE_EMPLOYEUR, Montant.de(30_000), Montant.ZERO,
                Montant.de(2_080), Montant.de(20_800), Montant.de(8_320), Montant.de(8_320), false, 0);
        ReglesScore regles = new ReglesScore("v1", 70, 50, Fraction.de("1/3"));
        return new Score(ConditionsEliminatoires.evaluer(eligibilite, new ReglesEligibilite(18, Fraction.de("1/3")),
                date), GrilleScoreV1.noter(dossier, regles, date));
    }

    public static Demande demande(UUID clientId, StatutDossier statut) {
        return Demande.reconstituer(UUID.randomUUID(), clientId, termes(), score(), LE, statut, LE);
    }

    static List<StatutDossier> inactifs() {
        return List.of(StatutDossier.REFUSEE, StatutDossier.ANNULEE, StatutDossier.SOLDEE);
    }
}
