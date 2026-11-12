package com.cic.paiement.domain;

import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.ErreurMetier;

/**
 * Un paiement porte déjà cette référence : la saisie est refusée et rien n'est enregistré, que le
 * montant soit le même (paiement saisi deux fois) ou non.
 */
public class PaiementEnDoubleException extends ErreurMetier {

    public static final String CODE = "paiement.doublon";

    private final UUID paiementExistantId;

    /** @param paiementExistantId paiement déjà enregistré ; {@code null} s'il n'est pas connu (saisies simultanées) */
    public PaiementEnDoubleException(UUID paiementExistantId) {
        super(CODE, "Paiement déjà enregistré sous cette référence");
        this.paiementExistantId = paiementExistantId;
    }

    public Optional<UUID> paiementExistantId() {
        return Optional.ofNullable(paiementExistantId);
    }
}
