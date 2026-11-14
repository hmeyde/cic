package com.seheli.recouvrement.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.application.DossierIntrouvableException;
import com.seheli.demande.application.StatutDossierService;
import com.seheli.demande.application.SuiviRemboursementService;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.application.ClientService;
import com.seheli.recouvrement.domain.NoteRecouvrement;
import com.seheli.recouvrement.domain.NoteRecouvrementRepository;
import com.seheli.recouvrement.domain.RelanceRepository;

/**
 * Lecture de l'écran du recouvrement et notes datées sur un dossier.
 *
 * <p>Prélèvements à venir : échéances A_VENIR d'aujourd'hui à J+{@value #JOURS_A_VENIR}. Réussis :
 * échéances PAYEE dont la date tombe de J-{@value #JOURS_REUSSIS} à J+{@value #JOURS_A_VENIR} (une
 * échéance peut être payée d'avance). En échec : toutes les échéances EN_RETARD. Seuls les dossiers
 * dont les prélèvements sont en place sont montrés (ou soldés, pour les réussis) : les échéances d'un
 * dossier annulé n'apparaissent jamais. Ces fenêtres règlent l'affichage, pas une règle de gestion.
 *
 * <p>Les transitions du dossier (régularisation, recouvrement, solde) sont décidées par
 * {@link SuiviRemboursementService} du module demande.
 */
@Service
public class SuiviRecouvrementService {

    static final int JOURS_A_VENIR = 30;
    static final int JOURS_REUSSIS = 60;

    private static final Logger LOG = LoggerFactory.getLogger(SuiviRecouvrementService.class);

    private final ContratService contrats;
    private final StatutDossierService statuts;
    private final SuiviRemboursementService remboursements;
    private final ClientService clients;
    private final RelanceRepository relances;
    private final NoteRecouvrementRepository notes;
    private final Clock clock;

    public SuiviRecouvrementService(ContratService contrats, StatutDossierService statuts,
            SuiviRemboursementService remboursements, ClientService clients, RelanceRepository relances,
            NoteRecouvrementRepository notes, Clock clock) {
        this.contrats = contrats;
        this.statuts = statuts;
        this.remboursements = remboursements;
        this.clients = clients;
        this.relances = relances;
        this.notes = notes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TableauRecouvrement tableau() {
        LocalDate aujourdhui = LocalDate.now(clock);
        LocalDate jusquAu = aujourdhui.plusDays(JOURS_A_VENIR);
        LocalDate depuis = aujourdhui.minusDays(JOURS_REUSSIS);
        Map<UUID, Optional<StatutDossier>> cache = new HashMap<>();
        List<PrelevementSuivi> aVenir = suivis(contrats.echeancesAVenirEntre(aujourdhui, jusquAu), cache,
                StatutDossier::estEnPrelevement);
        List<PrelevementSuivi> reussis = suivis(contrats.echeancesPayeesEntre(depuis, jusquAu),
                cache, s -> s.estEnPrelevement() || s == StatutDossier.SOLDEE);
        List<PrelevementSuivi> enEchec = suivis(contrats.echeancesEnRetardEntre(LocalDate.EPOCH, aujourdhui), cache,
                StatutDossier::estEnPrelevement);
        return new TableauRecouvrement(aVenir, reussis.reversed(), enEchec, jusquAu, depuis);
    }

    /**
     * @throws DossierIntrouvableException dossier inconnu ou sans contrat
     */
    @Transactional(readOnly = true)
    public FicheRecouvrement fiche(UUID demandeId) {
        Objects.requireNonNull(demandeId, "demandeId");
        StatutDossier statut = statuts.statut(demandeId).orElseThrow(DossierIntrouvableException::new);
        Contrat contrat = contrats.parDemande(demandeId).orElseThrow(DossierIntrouvableException::new);
        return new FicheRecouvrement(statut, contrat, clients.parId(contrat.clientId()),
                remboursements.situation(demandeId), relances.parContrat(contrat.id()), notes.parDemande(demandeId));
    }

    /**
     * Ajoute une note datée au dossier.
     *
     * @param auteur identifiant de connexion du back-office
     * @throws DossierIntrouvableException dossier inconnu
     * @throws com.seheli.recouvrement.domain.NoteInvalideException texte vide ou trop long
     */
    @Transactional
    public NoteRecouvrement noter(UUID demandeId, String auteur, String texte) {
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(auteur, "auteur");
        if (statuts.statut(demandeId).isEmpty()) {
            throw new DossierIntrouvableException();
        }
        NoteRecouvrement note = NoteRecouvrement.ecrire(demandeId, auteur, texte, clock.instant());
        notes.enregistrer(note);
        LOG.info("Note de recouvrement ajoutée : {}", note);
        return note;
    }

    private List<PrelevementSuivi> suivis(List<EcheanceAPrelever> echeances, Map<UUID, Optional<StatutDossier>> cache,
            Predicate<StatutDossier> retenu) {
        return echeances.stream()
                .flatMap(e -> cache.computeIfAbsent(e.demandeId(), statuts::statut).filter(retenu)
                        .map(s -> new PrelevementSuivi(e, s)).stream())
                .toList();
    }
}
