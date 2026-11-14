package com.seheli.demande.application;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.seheli.demande.domain.ConditionsEliminatoires;
import com.seheli.demande.domain.DossierEligibilite;
import com.seheli.demande.domain.Eligibilite;
import com.seheli.demande.domain.ReglesEligibilite;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/** Évalue les conditions éliminatoires du score avec les paramètres en vigueur et la date du jour. */
@Service
public class EligibiliteService {

    private final ParametreService parametres;
    private final Clock clock;

    public EligibiliteService(ParametreService parametres, Clock clock) {
        this.parametres = parametres;
        this.clock = clock;
    }

    public Eligibilite evaluer(DossierEligibilite dossier) {
        return ConditionsEliminatoires.evaluer(dossier, regles(), LocalDate.now(clock));
    }

    public ReglesEligibilite regles() {
        Parametres p = parametres.courants();
        return new ReglesEligibilite(p.entier(CleParametre.AGE_MINIMUM_ANS),
                p.fraction(CleParametre.ENDETTEMENT_MAX_FRACTION));
    }
}
