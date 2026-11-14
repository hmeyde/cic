package com.seheli.demande.application;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.seheli.demande.domain.ConditionsEliminatoires;
import com.seheli.demande.domain.DossierEligibilite;
import com.seheli.demande.domain.DossierScore;
import com.seheli.demande.domain.GrilleScoreV1;
import com.seheli.demande.domain.NoteScore;
import com.seheli.demande.domain.ReglesScore;
import com.seheli.demande.domain.Score;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/**
 * Calcule le score d'une demande (conditions éliminatoires et note sur 100) avec la grille
 * désignée par {@code score.grille.version}, les seuils en vigueur et la date du jour.
 */
@Service
public class ScoreService {

    private final ParametreService parametres;
    private final EligibiliteService eligibilite;
    private final Clock clock;

    public ScoreService(ParametreService parametres, EligibiliteService eligibilite, Clock clock) {
        this.parametres = parametres;
        this.eligibilite = eligibilite;
        this.clock = clock;
    }

    public Score evaluer(DossierEligibilite dossierEligibilite, DossierScore dossierScore) {
        LocalDate date = LocalDate.now(clock);
        return new Score(
                ConditionsEliminatoires.evaluer(dossierEligibilite, eligibilite.regles(), date),
                noter(dossierScore, date));
    }

    public NoteScore noter(DossierScore dossier) {
        return noter(dossier, LocalDate.now(clock));
    }

    private NoteScore noter(DossierScore dossier, LocalDate date) {
        ReglesScore regles = regles();
        if (!GrilleScoreV1.VERSION.equals(regles.versionGrille())) {
            throw new IllegalStateException("Grille de score non prise en charge : " + regles.versionGrille());
        }
        return GrilleScoreV1.noter(dossier, regles, date);
    }

    public ReglesScore regles() {
        Parametres p = parametres.courants();
        return new ReglesScore(p.texte(CleParametre.SCORE_GRILLE_VERSION),
                p.entier(CleParametre.SCORE_SEUIL_FAVORABLE),
                p.entier(CleParametre.SCORE_SEUIL_A_EXAMINER),
                p.fraction(CleParametre.ENDETTEMENT_MAX_FRACTION));
    }
}
