package com.seheli.demonstration.domain;

/**
 * Initialisations faites par les modules au démarrage de l'application (port de sortie) : catalogue, client et
 * comptes du back-office de démonstration, créés s'ils n'existent pas.
 */
public interface InitialisationsDemarrage {

    /** Rejoue les initialisations de démarrage, dans leur ordre habituel. */
    void rejouer();
}
