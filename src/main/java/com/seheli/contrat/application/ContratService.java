package com.seheli.contrat.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.AutorisationPrelevementRepository;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.ContratDejaEtabliException;
import com.seheli.contrat.domain.ContratRepository;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.contrat.domain.Echeancier;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Établit l'offre et le contrat d'un dossier accepté : prix et apport repris tels quels de la
 * demande, échéances générées et fixées une fois pour toutes. L'autorisation de prélèvement des
 * mensualités est émise dans la même transaction, à signer par le client (virement permanent par
 * défaut).
 *
 * <p>L'échéancier court à partir de la date limite de l'apport et de l'autorisation de prélèvement
 * (date d'acceptation + {@code delai.apport.autorisation.jours}) : la première échéance tombe au
 * premier jour de paie qui suit, jamais avant que le client ait pu payer son apport. Pour un CDD, la
 * dernière échéance doit encore tomber avant la fin du contrat de travail. L'endettement n'est pas
 * recontrôlé : les montants sont ceux contrôlés à la soumission.
 */
@Service
public class ContratService {

    private static final Logger LOG = LoggerFactory.getLogger(ContratService.class);

    private static final int CHIFFRES_ANNEE = 4;
    /** Année puis numéro d'ordre, qui tient dans un {@code long} (18 chiffres au plus). */
    private static final int CHIFFRES_MAX = CHIFFRES_ANNEE + 18;
    /** Préfixe des numéros de contrat (marque Seheli). */
    static final String PREFIXE = "SEH";
    /**
     * Préfixe des contrats établis avant le changement de marque : leurs numéros, déjà imprimés et
     * repris dans les références de paiement, ne changent pas.
     */
    static final String PREFIXE_HISTORIQUE = "CIC";

    private final ContratRepository contrats;
    private final AutorisationPrelevementRepository autorisations;
    private final ParametreService parametres;
    private final Clock clock;

    public ContratService(ContratRepository contrats, AutorisationPrelevementRepository autorisations,
            ParametreService parametres, Clock clock) {
        this.contrats = contrats;
        this.autorisations = autorisations;
        this.parametres = parametres;
        this.clock = clock;
    }

    /**
     * @throws ContratDejaEtabliException le dossier a déjà un contrat
     * @throws com.seheli.contrat.domain.FinContratDepasseeException la dernière échéance tomberait après
     *         la fin du CDD
     */
    @Transactional
    public Contrat etablir(ContratAEtablir a) {
        Objects.requireNonNull(a, "a");
        if (contrats.parDemande(a.demandeId()).isPresent()) {
            throw new ContratDejaEtabliException();
        }
        LocalDate acceptation = LocalDate.ofInstant(a.accepteLe(), clock.getZone());
        LocalDate depart = acceptation.plusDays(parametres.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS));
        Echeancier plan = Echeancier.planifier(a.prix().resteAFinancer(a.apport()), a.nombreMois(), depart,
                a.jourPaie(), a.finContrat());
        Contrat contrat = Contrat.etablir(numero(acceptation, contrats.prochainNumeroOrdre()), a.demandeId(),
                a.clientId(), a.produitId(), a.fournisseurId(), a.designationProduit(), a.prix(), a.apport(), plan,
                a.accepteLe());
        contrats.enregistrer(contrat);
        AutorisationPrelevement autorisation = AutorisationPrelevement.emettre(contrat, a.accepteLe());
        autorisations.enregistrer(autorisation);
        LOG.info("Contrat établi : {}, {}", contrat, autorisation);
        return contrat;
    }

    /** Contrat d'un dossier ; vide tant que le dossier n'est pas accepté. */
    @Transactional(readOnly = true)
    public Optional<Contrat> parDemande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId);
    }

    /**
     * Échéances A_VENIR à prélever entre ces deux dates incluses, par date puis numéro de contrat :
     * sert aux rappels envoyés avant chaque prélèvement.
     */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesAVenirEntre(LocalDate du, LocalDate au) {
        Objects.requireNonNull(du, "du");
        Objects.requireNonNull(au, "au");
        return contrats.echeancesAVenirEntre(du, au);
    }

    /**
     * Échéances encore A_VENIR dont la date est passée ({@code date} exclue), par date puis numéro de
     * contrat : elles n'ont pas été payées à la date prévue.
     */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesAVenirAvant(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return contrats.echeancesEntre(StatutEcheance.A_VENIR, LocalDate.EPOCH, date.minusDays(1));
    }

    /** Échéances EN_RETARD dont la date est comprise entre ces deux dates incluses : sert aux relances. */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesEnRetardEntre(LocalDate du, LocalDate au) {
        Objects.requireNonNull(du, "du");
        Objects.requireNonNull(au, "au");
        return contrats.echeancesEntre(StatutEcheance.EN_RETARD, du, au);
    }

    /** Échéances PAYEE dont la date est comprise entre ces deux dates incluses : prélèvements réussis. */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesPayeesEntre(LocalDate du, LocalDate au) {
        Objects.requireNonNull(du, "du");
        Objects.requireNonNull(au, "au");
        return contrats.echeancesEntre(StatutEcheance.PAYEE, du, au);
    }

    /** Le client a au moins une échéance EN_RETARD : ses nouveaux achats sont bloqués. */
    @Transactional(readOnly = true)
    public boolean clientAUneEcheanceEnRetard(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return contrats.clientAUneEcheanceEnRetard(clientId);
    }

    /**
     * Constate qu'une échéance n'a pas été payée à sa date : A_VENIR → EN_RETARD. Sans effet si elle
     * est déjà payée ou déjà en retard, ou si sa date n'est pas encore passée. Le montant ne change pas.
     *
     * @return vrai si l'échéance vient de passer à EN_RETARD
     * @throws IllegalArgumentException le contrat n'a pas d'échéance de ce rang
     */
    @Transactional
    public boolean constaterEcheanceEnRetard(UUID contratId, int rang) {
        Objects.requireNonNull(contratId, "contratId");
        Contrat contrat = contrats.parId(contratId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contratId));
        Echeance echeance = contrat.echeance(rang)
                .orElseThrow(() -> new IllegalArgumentException("Pas d'échéance " + rang + " pour " + contrat));
        if (echeance.statut() != StatutEcheance.A_VENIR || !echeance.date().isBefore(LocalDate.now(clock))) {
            return false;
        }
        contrats.enregistrerStatut(contratId, echeance.enRetard());
        LOG.info("Échéance {} de {} impayée à sa date du {} : EN_RETARD", rang, contrat, echeance.date());
        return true;
    }

    /** Autorisation de prélèvement d'un contrat ; une par contrat, émise avec lui. */
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> autorisation(UUID contratId) {
        Objects.requireNonNull(contratId, "contratId");
        return autorisations.parContrat(contratId);
    }

    /**
     * Contrat dont le numéro, débarrassé de ses séparateurs, donne ces chiffres : {@code 2026000042}
     * désigne {@code SEH-2026-000042} (ou {@code CIC-2026-000042} pour un contrat établi avant le
     * changement de marque ; la séquence des numéros d'ordre est commune aux deux préfixes). Sert à
     * retrouver le dossier d'une référence de paiement tirée du numéro de contrat. Vide si les chiffres
     * ne forment aucun numéro possible ou si aucun contrat ne porte ce numéro.
     */
    @Transactional(readOnly = true)
    public Optional<Contrat> parChiffresDuNumero(String chiffres) {
        if (chiffres == null || !chiffres.matches("\\d{" + (CHIFFRES_ANNEE + 1) + "," + CHIFFRES_MAX + "}")) {
            return Optional.empty();
        }
        int annee = Integer.parseInt(chiffres.substring(0, CHIFFRES_ANNEE));
        long ordre = Long.parseLong(chiffres.substring(CHIFFRES_ANNEE));
        String numero = numero(PREFIXE, annee, ordre);
        if (!numero.replaceAll("\\D", "").equals(chiffres)) {
            return Optional.empty();
        }
        return contrats.parNumero(numero).or(() -> contrats.parNumero(numero(PREFIXE_HISTORIQUE, annee, ordre)));
    }

    /**
     * Constate le paiement de l'échéance de ce rang : A_VENIR ou EN_RETARD → PAYEE. Sans effet si
     * elle est déjà payée (un second paiement reste visible dans les paiements, sans changer
     * l'échéance).
     *
     * @return vrai si l'échéance vient de passer à PAYEE
     * @throws IllegalArgumentException le contrat n'a pas d'échéance de ce rang
     */
    @Transactional
    public boolean constaterEcheancePayee(UUID contratId, int rang) {
        Objects.requireNonNull(contratId, "contratId");
        Contrat contrat = contrats.parId(contratId)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + contratId));
        Echeance echeance = contrat.echeance(rang)
                .orElseThrow(() -> new IllegalArgumentException("Pas d'échéance " + rang + " pour " + contrat));
        if (echeance.estPayee()) {
            LOG.info("Échéance {} de {} déjà payée", rang, contrat);
            return false;
        }
        contrats.enregistrerStatut(contratId, echeance.payee());
        LOG.info("Échéance {} de {} payée", rang, contrat);
        return true;
    }

    /** Numéro lisible du contrat, par exemple {@code SEH-2026-000042}. */
    static String numero(LocalDate le, long ordre) {
        return numero(PREFIXE, le.getYear(), ordre);
    }

    private static String numero(String prefixe, int annee, long ordre) {
        return "%s-%d-%06d".formatted(prefixe, annee, ordre);
    }
}
