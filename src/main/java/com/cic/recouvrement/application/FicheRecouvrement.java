package com.cic.recouvrement.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cic.contrat.domain.Contrat;
import com.cic.demande.domain.SituationEcheances;
import com.cic.demande.domain.StatutDossier;
import com.cic.identite.domain.Client;
import com.cic.recouvrement.domain.NoteRecouvrement;
import com.cic.recouvrement.domain.Relance;

/**
 * Fiche d'un dossier pour le recouvrement : statut, contrat et échéancier, client à joindre,
 * relances envoyées et notes datées, et les décisions possibles dans cet état.
 */
public record FicheRecouvrement(StatutDossier statut, Contrat contrat, Optional<Client> client,
        SituationEcheances situation, List<Relance> relances, List<NoteRecouvrement> notes) {

    public FicheRecouvrement {
        Objects.requireNonNull(statut, "statut");
        Objects.requireNonNull(contrat, "contrat");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(situation, "situation");
        relances = List.copyOf(relances);
        notes = List.copyOf(notes);
    }

    /** EN_RETARD et plus aucune échéance en retard : le dossier peut revenir EN_REMBOURSEMENT. */
    public boolean peutRegulariser() {
        return statut == StatutDossier.EN_RETARD && situation.aucunRetard();
    }

    /** EN_RETARD : le dossier peut passer en RECOUVREMENT. */
    public boolean peutPasserEnRecouvrement() {
        return statut == StatutDossier.EN_RETARD;
    }

    /** EN_REMBOURSEMENT et toutes les échéances payées : le dossier peut être SOLDEE. */
    public boolean peutSolder() {
        return statut == StatutDossier.EN_REMBOURSEMENT && situation.toutPaye();
    }
}
