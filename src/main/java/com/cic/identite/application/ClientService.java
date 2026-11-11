package com.cic.identite.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;

/** Lecture des clients. */
@Service
public class ClientService {

    private final ClientRepository clients;

    public ClientService(ClientRepository clients) {
        this.clients = clients;
    }

    @Transactional(readOnly = true)
    public Optional<Client> parId(UUID id) {
        return clients.parId(id);
    }
}
