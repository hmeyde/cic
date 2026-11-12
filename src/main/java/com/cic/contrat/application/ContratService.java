package com.cic.contrat.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.contrat.domain.AutorisationPrelevement;
import com.cic.contrat.domain.AutorisationPrelevementRepository;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratDejaEtabliException;
import com.cic.contrat.domain.ContratRepository;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.Echeancier;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

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
     * @throws com.cic.contrat.domain.FinContratDepasseeException la dernière échéance tomberait après
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

    /** Autorisation de prélèvement d'un contrat ; une par contrat, émise avec lui. */
    @Transactional(readOnly = true)
    public Optional<AutorisationPrelevement> autorisation(UUID contratId) {
        Objects.requireNonNull(contratId, "contratId");
        return autorisations.parContrat(contratId);
    }

    /**
     * Contrat dont le numéro, débarrassé de ses séparateurs, donne ces chiffres : {@code 2026000042}
     * désigne {@code CIC-2026-000042}. Sert à retrouver le dossier d'une référence de paiement tirée
     * du numéro de contrat. Vide si les chiffres ne forment aucun numéro possible ou si aucun contrat
     * ne porte ce numéro.
     */
    @Transactional(readOnly = true)
    public Optional<Contrat> parChiffresDuNumero(String chiffres) {
        if (chiffres == null || !chiffres.matches("\\d{" + (CHIFFRES_ANNEE + 1) + "," + CHIFFRES_MAX + "}")) {
            return Optional.empty();
        }
        String numero = numero(Integer.parseInt(chiffres.substring(0, CHIFFRES_ANNEE)),
                Long.parseLong(chiffres.substring(CHIFFRES_ANNEE)));
        if (!numero.replaceAll("\\D", "").equals(chiffres)) {
            return Optional.empty();
        }
        return contrats.parNumero(numero);
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

    /** Numéro lisible du contrat, par exemple {@code CIC-2026-000042}. */
    static String numero(LocalDate le, long ordre) {
        return numero(le.getYear(), ordre);
    }

    private static String numero(int annee, long ordre) {
        return "CIC-%d-%06d".formatted(annee, ordre);
    }
}
