package com.cic.identite.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;

public class ClientsEnMemoire implements ClientRepository {

    private final Map<UUID, Client> clients = new HashMap<>();

    @Override
    public void enregistrer(Client client) {
        clients.put(client.id(), client);
    }

    @Override
    public Optional<Client> parId(UUID id) {
        return Optional.ofNullable(clients.get(id));
    }

    @Override
    public Optional<Client> parNni(String nni) {
        return clients.values().stream().filter(c -> c.nni().equals(nni)).findFirst();
    }

    @Override
    public Optional<Client> parTelephone(String telephone) {
        return clients.values().stream().filter(c -> c.telephone().equals(telephone)).findFirst();
    }
}
