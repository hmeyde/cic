package com.cic.identite.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;

@Component
class ClientPersistenceAdapter implements ClientRepository {

    private final ClientJpaRepository repository;

    ClientPersistenceAdapter(ClientJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void enregistrer(Client c) {
        repository.save(new ClientEntity(c.id(), c.nni(), c.nom(), c.prenom(), c.dateNaissance(), c.telephone(),
                c.consentementDonneesLe(), c.estNniVerifie(), c.estPieceIdentiteVerifiee()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> parId(UUID id) {
        return repository.findById(id).map(ClientPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> parNni(String nni) {
        return repository.findByNni(nni).map(ClientPersistenceAdapter::versDomaine);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> parTelephone(String telephone) {
        return repository.findByTelephone(telephone).map(ClientPersistenceAdapter::versDomaine);
    }

    private static Client versDomaine(ClientEntity e) {
        return Client.reconstituer(e.getId(), e.getNni(), e.getNom(), e.getPrenom(), e.getDateNaissance(),
                e.getTelephone(), e.getConsentementDonneesLe(), e.isNniVerifie(), e.isPieceIdentiteVerifiee());
    }
}
