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

import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.ContratDejaEtabliException;
import com.cic.contrat.domain.ContratRepository;
import com.cic.contrat.domain.Echeancier;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Établit l'offre et le contrat d'un dossier accepté : prix et apport repris tels quels de la
 * demande, échéances générées et fixées une fois pour toutes.
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

    private final ContratRepository contrats;
    private final ParametreService parametres;
    private final Clock clock;

    public ContratService(ContratRepository contrats, ParametreService parametres, Clock clock) {
        this.contrats = contrats;
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
        LOG.info("Contrat établi : {}", contrat);
        return contrat;
    }

    /** Contrat d'un dossier ; vide tant que le dossier n'est pas accepté. */
    @Transactional(readOnly = true)
    public Optional<Contrat> parDemande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId);
    }

    /** Numéro lisible du contrat, par exemple {@code CIC-2026-000042}. */
    static String numero(LocalDate le, long ordre) {
        return "CIC-%d-%06d".formatted(le.getYear(), ordre);
    }
}
