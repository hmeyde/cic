package com.cic.recouvrement.domain;

import java.util.UUID;

/** Stockage des relances envoyées (port de sortie). */
public interface RelanceRepository {

    /** Une relance de ce type a déjà été envoyée pour cette échéance. */
    boolean existe(UUID echeanceId, TypeRelance type);

    /** Insère la relance ; échoue si l'échéance a déjà une relance de ce type. */
    void enregistrer(Relance relance);
}
