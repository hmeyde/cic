package com.cic.identite.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Création du compte d'un nouveau client, juste après qu'il a prouvé par un code à usage unique que
 * le téléphone est à lui.
 *
 * <p>Le consentement explicite à l'usage des données est obligatoire ; son instant (horloge injectée)
 * est conservé. L'âge minimum vient de la table {@code parametre}. Le NNI et la pièce d'identité
 * restent à vérifier par le back-office.
 */
@Service
public class CreationCompteClientService {

    /** Au-delà, la date de naissance est sans doute une erreur de saisie. */
    static final int AGE_MAXIMUM_VRAISEMBLABLE = 120;

    private static final Logger LOG = LoggerFactory.getLogger(CreationCompteClientService.class);

    private final ClientRepository clients;
    private final ParametreService parametres;
    private final Clock clock;

    public CreationCompteClientService(ClientRepository clients, ParametreService parametres, Clock clock) {
        this.clients = clients;
        this.parametres = parametres;
        this.clock = clock;
    }

    /**
     * Crée le compte et renvoie l'identifiant du nouveau client.
     *
     * @throws ConsentementManquantException   sans consentement explicite
     * @throws DateNaissanceInvalideException  date future ou invraisemblable
     * @throws AgeMinimumNonAtteintException   client trop jeune
     * @throws NniDejaUtiliseException         un client porte déjà ce NNI
     * @throws CompteDejaExistantException     un compte existe déjà pour ce téléphone
     * @throws IllegalArgumentException        NNI, nom, prénom ou téléphone mal formés
     */
    @Transactional
    public Client creer(NouveauCompteClient saisie) {
        Objects.requireNonNull(saisie, "saisie");
        if (!saisie.consentement()) {
            throw new ConsentementManquantException();
        }
        String telephone = Client.normaliserTelephone(saisie.telephone());
        String nni = Client.normaliserNni(saisie.nni());
        verifierDateNaissance(saisie.dateNaissance());
        if (clients.parTelephone(telephone).isPresent()) {
            throw new CompteDejaExistantException();
        }
        if (clients.parNni(nni).isPresent()) {
            throw new NniDejaUtiliseException();
        }
        Client client = Client.nouveau(nni, saisie.nom(), saisie.prenom(), saisie.dateNaissance(), telephone,
                clock.instant());
        clients.enregistrer(client);
        LOG.info("Compte créé pour {}", client);
        return client;
    }

    private void verifierDateNaissance(LocalDate dateNaissance) {
        Objects.requireNonNull(dateNaissance, "dateNaissance");
        LocalDate aujourdhui = LocalDate.now(clock);
        if (dateNaissance.isAfter(aujourdhui)
                || dateNaissance.isBefore(aujourdhui.minusYears(AGE_MAXIMUM_VRAISEMBLABLE))) {
            throw new DateNaissanceInvalideException();
        }
        int ageMinimum = parametres.entier(CleParametre.AGE_MINIMUM_ANS);
        if (dateNaissance.plusYears(ageMinimum).isAfter(aujourdhui)) {
            throw new AgeMinimumNonAtteintException(ageMinimum);
        }
    }
}
