package com.seheli.inscription.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.inscription.domain.Inscription;
import com.seheli.inscription.domain.InscriptionInvalideException;
import com.seheli.inscription.domain.InscriptionRepository;
import com.seheli.inscription.domain.TelephoneDejaInscritException;
import com.seheli.inscription.domain.TrancheSalaire;
import com.seheli.inscription.domain.TypeContrat;

/** Pré-inscription publique : une seule par numéro de téléphone. */
@Service
public class InscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(InscriptionService.class);

    private final InscriptionRepository inscriptions;
    private final Clock clock;

    public InscriptionService(InscriptionRepository inscriptions, Clock clock) {
        this.inscriptions = inscriptions;
        this.clock = clock;
    }

    /**
     * @throws InscriptionInvalideException  un champ manque ou est mal formé
     * @throws TelephoneDejaInscritException une pré-inscription existe déjà pour ce téléphone
     */
    @Transactional
    public Inscription inscrire(SaisieInscription saisie) {
        Objects.requireNonNull(saisie, "saisie");
        Inscription inscription = Inscription.nouvelle(saisie.nom(), saisie.telephone(), saisie.employeur(),
                valeur(TypeContrat.class, saisie.typeContrat(), InscriptionInvalideException.TYPE_CONTRAT_INVALIDE),
                valeur(TrancheSalaire.class, saisie.trancheSalaire(),
                        InscriptionInvalideException.TRANCHE_SALAIRE_INVALIDE),
                saisie.produitSouhaite(), saisie.consentement(), clock.instant());
        if (inscriptions.existeTelephone(inscription.telephone())) {
            throw new TelephoneDejaInscritException();
        }
        inscriptions.enregistrer(inscription);
        LOG.info("Pré-inscription enregistrée : {}", inscription);
        return inscription;
    }

    /** Nombre total de pré-inscriptions. */
    @Transactional(readOnly = true)
    public long nombre() {
        return inscriptions.nombre();
    }

    /** Toutes les pré-inscriptions, de la plus récente à la plus ancienne (export du back-office). */
    @Transactional(readOnly = true)
    public List<Inscription> toutes() {
        return inscriptions.toutesDeLaPlusRecente();
    }

    private static <E extends Enum<E>> E valeur(Class<E> type, String saisie, String code) {
        if (saisie == null || saisie.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, saisie.strip());
        } catch (IllegalArgumentException e) {
            throw new InscriptionInvalideException(code);
        }
    }
}
