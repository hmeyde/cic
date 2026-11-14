package com.seheli.demonstration.domain;

/**
 * Base de données de la démonstration (port de sortie) : remise dans l'état d'une base neuve, schéma et
 * paramètres de départ compris.
 */
public interface BaseDeDemonstration {

    /** Efface toutes les données et rejoue les migrations depuis le début. */
    void remettreANeuf();
}
