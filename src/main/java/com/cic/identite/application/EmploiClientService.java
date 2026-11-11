package com.cic.identite.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.ClientRepository;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.EmploiRepository;

/**
 * Emploi actuel du client : employeur, contrat, salaire net, jour de paie et mode de remboursement.
 *
 * <p>Le client déclare son emploi puis peut le modifier ; un client n'a qu'un emploi enregistré.
 * Seul un emploi en cours est accepté (commencé, et pour un CDD pas encore terminé), à la date de
 * l'horloge injectée.
 */
@Service
public class EmploiClientService {

    private static final Logger LOG = LoggerFactory.getLogger(EmploiClientService.class);

    private final ClientRepository clients;
    private final EmploiRepository emplois;
    private final Clock clock;

    public EmploiClientService(ClientRepository clients, EmploiRepository emplois, Clock clock) {
        this.clients = clients;
        this.emplois = emplois;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<Emploi> parClient(UUID clientId) {
        return emplois.parClient(clientId);
    }

    /**
     * Enregistre l'emploi du client, ou remplace celui déjà déclaré.
     *
     * @throws DateDebutEmploiFutureException emploi pas encore commencé
     * @throws ContratTermineException        CDD déjà terminé
     * @throws IllegalArgumentException       client inconnu ou saisie incohérente (voir {@link Emploi})
     */
    @Transactional
    public Emploi enregistrer(UUID clientId, SaisieEmploi s) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(s, "saisie");
        if (clients.parId(clientId).isEmpty()) {
            throw new IllegalArgumentException("Client inconnu");
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        if (s.dateDebut() != null && s.dateDebut().isAfter(aujourdhui)) {
            throw new DateDebutEmploiFutureException();
        }
        if (s.dateFin() != null && s.dateFin().isBefore(aujourdhui)) {
            throw new ContratTermineException();
        }
        Optional<Emploi> existant = emplois.parClient(clientId);
        Emploi emploi;
        if (existant.isPresent()) {
            emploi = existant.get();
            emploi.modifier(s.employeur(), s.typeContrat(), s.dateDebut(), s.dateFin(), s.salaireNet(), s.jourPaie(),
                    s.salaireVerseSurCompte(), s.salaireDomicilieBmci(), s.modeRemboursement());
        } else {
            emploi = Emploi.nouveau(clientId, s.employeur(), s.typeContrat(), s.dateDebut(), s.dateFin(),
                    s.salaireNet(), s.jourPaie(), s.salaireVerseSurCompte(), s.salaireDomicilieBmci(),
                    s.modeRemboursement());
        }
        emplois.enregistrer(emploi);
        LOG.info("{} {}", existant.isPresent() ? "Emploi modifié :" : "Emploi déclaré :", emploi);
        return emploi;
    }
}
