package com.seheli.contrat.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.AutorisationPrelevementRepository;
import com.seheli.contrat.domain.CompteBeneficiaire;
import com.seheli.contrat.domain.ContenuAutorisation;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.ContratRepository;
import com.seheli.contrat.domain.RenduAutorisationPdf;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Emploi;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Autorisation de prélèvement d'un dossier, en PDF, remplie et prête à être signée par le client
 * pour sa banque. Produite à la demande à partir de l'autorisation et du contrat enregistrés : les
 * montants et les dates sont ceux des échéances fixées à l'acceptation.
 *
 * <p>Le compte du vendeur qui reçoit les virements est lu dans la table {@code parametre}
 * ({@code cic.compte.*}). La banque du client n'est connue que si son salaire est domicilié à la
 * BMCI ; sinon, comme son numéro de compte, il la complète à la main. La date limite est celle de
 * l'apport : date d'acceptation + {@code delai.apport.autorisation.jours}.
 */
@Service
public class AutorisationPdfService {

    static final String BANQUE_BMCI = "BMCI";

    private final ContratRepository contrats;
    private final AutorisationPrelevementRepository autorisations;
    private final ClientService clients;
    private final EmploiClientService emplois;
    private final ParametreService parametres;
    private final RenduAutorisationPdf rendu;
    private final Clock clock;

    public AutorisationPdfService(ContratRepository contrats, AutorisationPrelevementRepository autorisations,
            ClientService clients, EmploiClientService emplois, ParametreService parametres,
            RenduAutorisationPdf rendu, Clock clock) {
        this.contrats = contrats;
        this.autorisations = autorisations;
        this.clients = clients;
        this.emplois = emplois;
        this.parametres = parametres;
        this.rendu = rendu;
        this.clock = clock;
    }

    /** Autorisation PDF du dossier ; vide tant que le dossier n'a pas de contrat. */
    @Transactional(readOnly = true)
    public Optional<ContratPdf> pourDemande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId)
                .flatMap(c -> autorisations.parContrat(c.id()).map(a -> imprimer(c, a)));
    }

    private ContratPdf imprimer(Contrat contrat, AutorisationPrelevement autorisation) {
        Client client = clients.parId(contrat.clientId())
                .orElseThrow(() -> new IllegalStateException("Client introuvable pour le " + contrat));
        Optional<String> banque = emplois.parClient(contrat.clientId())
                .filter(Emploi::estSalaireDomicilieBmci)
                .map(e -> BANQUE_BMCI);
        CompteBeneficiaire vendeur = new CompteBeneficiaire(parametres.texte(CleParametre.SEHELI_COMPTE_TITULAIRE),
                parametres.texte(CleParametre.SEHELI_COMPTE_BANQUE), parametres.texte(CleParametre.SEHELI_COMPTE_NUMERO));
        LocalDate emiseLe = LocalDate.ofInstant(autorisation.emiseLe(), clock.getZone());
        LocalDate dateLimite = LocalDate.ofInstant(contrat.etabliLe(), clock.getZone())
                .plusDays(parametres.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS));
        byte[] pdf = rendu.rendre(new ContenuAutorisation(autorisation, contrat, ContratPdfService.acheteur(client),
                vendeur, banque, emiseLe, dateLimite, parametres.texte(CleParametre.SOCIETE_RAISON_SOCIALE)));
        return new ContratPdf("autorisation-prelevement-" + contrat.numero() + ".pdf", pdf);
    }
}
