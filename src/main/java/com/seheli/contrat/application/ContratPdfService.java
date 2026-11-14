package com.seheli.contrat.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.domain.Acheteur;
import com.seheli.contrat.domain.ContenuContrat;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.ContratRepository;
import com.seheli.contrat.domain.RenduContratPdf;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.domain.Client;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Contrat de vente à tempérament d'un dossier, en PDF. Le document est produit à la demande à partir
 * du contrat enregistré : les montants sont ceux fixés à l'acceptation et ne changent jamais.
 *
 * <p>La date limite de l'apport est la date d'acceptation + {@code delai.apport.autorisation.jours},
 * comme pour l'échéancier. L'identité de l'acheteur est lue dans le module Identité au moment de
 * l'impression.
 */
@Service
public class ContratPdfService {

    private final ContratRepository contrats;
    private final ClientService clients;
    private final ParametreService parametres;
    private final RenduContratPdf rendu;
    private final Clock clock;

    public ContratPdfService(ContratRepository contrats, ClientService clients, ParametreService parametres,
            RenduContratPdf rendu, Clock clock) {
        this.contrats = contrats;
        this.clients = clients;
        this.parametres = parametres;
        this.rendu = rendu;
        this.clock = clock;
    }

    /** Contrat PDF du dossier ; vide tant que le dossier n'a pas de contrat. */
    @Transactional(readOnly = true)
    public Optional<ContratPdf> pourDemande(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        return contrats.parDemande(demandeId).map(this::imprimer);
    }

    private ContratPdf imprimer(Contrat contrat) {
        Client client = clients.parId(contrat.clientId())
                .orElseThrow(() -> new IllegalStateException("Client introuvable pour le " + contrat));
        LocalDate etabliLe = LocalDate.ofInstant(contrat.etabliLe(), clock.getZone());
        LocalDate dateLimite = etabliLe.plusDays(parametres.entier(CleParametre.DELAI_APPORT_AUTORISATION_JOURS));
        byte[] pdf = rendu.rendre(new ContenuContrat(contrat, acheteur(client), etabliLe, dateLimite,
                parametres.texte(CleParametre.SOCIETE_RAISON_SOCIALE)));
        return new ContratPdf("contrat-" + contrat.numero() + ".pdf", pdf);
    }

    /** Identité du client telle qu'elle figure sur les documents du contrat. */
    static Acheteur acheteur(Client client) {
        return new Acheteur(client.nom(), client.prenom(), client.nni(), client.dateNaissance(), client.telephone());
    }
}
