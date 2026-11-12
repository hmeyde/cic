package com.cic.comptabilite.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.audit.application.JournalAuditService;
import com.cic.audit.domain.Acteur;
import com.cic.audit.domain.EntreeJournal;
import com.cic.audit.domain.ObjetAudite;
import com.cic.commun.domain.Montant;
import com.cic.comptabilite.domain.ApportCapital;
import com.cic.comptabilite.domain.ApportCapitalInvalideException;
import com.cic.comptabilite.domain.ApportCapitalRepository;
import com.cic.comptabilite.domain.EcritureComptable;
import com.cic.comptabilite.domain.EcritureComptableRepository;

/**
 * Saisie des apports en capital des associés. Chaque apport est enregistré, comptabilisé
 * (banque 512 / capital 101) et journalisé (objet {@value #TYPE_OBJET}) dans la même transaction.
 */
@Service
public class ApportCapitalService {

    static final String TYPE_OBJET = "APPORT_CAPITAL";
    static final String ACTION_SAISIE = "SAISIE_APPORT_CAPITAL";

    private static final int CHIFFRES_MONTANT_MAX = 12;
    private static final Logger LOG = LoggerFactory.getLogger(ApportCapitalService.class);

    private final ApportCapitalRepository apports;
    private final EcritureComptableRepository ecritures;
    private final JournalAuditService journal;
    private final Clock clock;

    public ApportCapitalService(ApportCapitalRepository apports, EcritureComptableRepository ecritures,
            JournalAuditService journal, Clock clock) {
        this.apports = apports;
        this.ecritures = ecritures;
        this.journal = journal;
        this.clock = clock;
    }

    /**
     * @param auteur identifiant de connexion du back-office
     * @throws ApportCapitalInvalideException associé, montant ou date du versement invalide
     */
    @Transactional
    public ApportCapital saisir(SaisieApportCapital saisie, String auteur) {
        Objects.requireNonNull(saisie, "saisie");
        Objects.requireNonNull(auteur, "auteur");
        Instant maintenant = clock.instant();
        ApportCapital apport = ApportCapital.saisir(saisie.associe(), montant(saisie.montant()),
                date(saisie.verseLe()), LocalDate.now(clock), auteur, maintenant);
        apports.enregistrer(apport);
        ecritures.enregistrerSiNouvelle(
                EcritureComptable.apportCapital(apport.id(), apport.montant(), apport.verseLe(), maintenant));
        journal.journaliser(EntreeJournal.action(maintenant, Acteur.backOffice(auteur), ACTION_SAISIE,
                new ObjetAudite(TYPE_OBJET, apport.id()), "Apport en capital de " + apport.montant()));
        LOG.info("Apport en capital saisi : {}", apport);
        return apport;
    }

    /** Tous les apports, du versement le plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<ApportCapital> tous() {
        return apports.tous();
    }

    private static Montant montant(String saisie) {
        String chiffres = saisie == null ? "" : saisie.replaceAll("[\\s\\u00A0\\u202F]", "");
        if (!chiffres.matches("\\d{1," + CHIFFRES_MONTANT_MAX + "}")) {
            throw new ApportCapitalInvalideException(ApportCapitalInvalideException.MONTANT_INVALIDE);
        }
        return Montant.de(Long.parseLong(chiffres));
    }

    private static LocalDate date(String saisie) {
        try {
            return LocalDate.parse(saisie == null ? "" : saisie.strip());
        } catch (DateTimeParseException e) {
            throw new ApportCapitalInvalideException(ApportCapitalInvalideException.DATE_INVALIDE);
        }
    }
}
