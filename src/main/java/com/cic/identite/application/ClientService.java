package com.cic.identite.application;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;

/** Lecture des clients et vérification de leur identité par le back-office. */
@Service
public class ClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clients;

    public ClientService(ClientRepository clients) {
        this.clients = clients;
    }

    @Transactional(readOnly = true)
    public Optional<Client> parId(UUID id) {
        return clients.parId(id);
    }

    /** Client portant ce téléphone (saisi librement, normalisé ici) ; vide si inconnu ou mal formé. */
    @Transactional(readOnly = true)
    public Optional<Client> parTelephone(String telephone) {
        String normalise;
        try {
            normalise = Client.normaliserTelephone(telephone);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return clients.parTelephone(normalise);
    }

    /**
     * Le back-office a contrôlé le NNI et la pièce d'identité du client (conditions éliminatoires du score).
     *
     * @throws IllegalArgumentException client inconnu
     */
    @Transactional
    public Client verifierIdentite(UUID id) {
        Client client = clients.parId(id).orElseThrow(() -> new IllegalArgumentException("Client inconnu"));
        client.verifierNni();
        client.verifierPieceIdentite();
        clients.enregistrer(client);
        LOG.info("Identité vérifiée : {}", client);
        return client;
    }
}
