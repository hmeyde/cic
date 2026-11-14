package com.seheli.identite.application;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.ClientRepository;

/**
 * Profils dev et demo : crée au démarrage un client de démonstration s'il n'existe pas, pour pouvoir
 * se connecter à l'espace client. Téléphone et NNI viennent de la configuration ({@code seheli.client-demo.*}).
 */
@Component
@Profile({"dev", "demo"})
class ClientDemonstration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ClientDemonstration.class);

    private final ClientRepository clients;
    private final Clock clock;
    private final String telephone;
    private final String nni;

    ClientDemonstration(ClientRepository clients, Clock clock,
            @Value("${seheli.client-demo.telephone}") String telephone,
            @Value("${seheli.client-demo.nni}") String nni) {
        this.clients = clients;
        this.clock = clock;
        this.telephone = Client.normaliserTelephone(telephone);
        this.nni = Client.normaliserNni(nni);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clients.parTelephone(telephone).isPresent() || clients.parNni(nni).isPresent()) {
            return;
        }
        clients.enregistrer(Client.nouveau(nni, "Démonstration", "Client", LocalDate.of(1990, 1, 15), telephone,
                clock.instant()));
        LOG.info("Client de démonstration créé");
    }
}
