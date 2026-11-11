package com.cic.identite.application;

import com.cic.identite.domain.Document;

/** Justificatif reçu, avec le nom du client, pour le back-office. */
public record DocumentRecu(Document document, String prenomClient, String nomClient) {
}
