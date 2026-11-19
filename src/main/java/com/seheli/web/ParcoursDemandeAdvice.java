package com.seheli.web;

import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.seheli.demande.application.DemandeService;
import com.seheli.demande.domain.Demande;
import com.seheli.identite.application.DocumentService;
import com.seheli.identite.application.EmploiClientService;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.TypeDocument;

/**
 * Expose aux gabarits la barre de progression du parcours de demande ({@code parcours}, un
 * {@link ParcoursDemande} ou {@code null}) sur les pages Compte, Emploi, Justificatifs et Demande.
 * Un client qui a déjà un dossier actif n'est plus dans le parcours : pas de barre. Les requêtes
 * HTMX (fragments) ne la calculent pas.
 */
@ControllerAdvice
class ParcoursDemandeAdvice {

    private final EmploiClientService emplois;
    private final DocumentService documents;
    private final DemandeService demandes;

    ParcoursDemandeAdvice(EmploiClientService emplois, DocumentService documents, DemandeService demandes) {
        this.emplois = emplois;
        this.documents = documents;
        this.demandes = demandes;
    }

    @ModelAttribute("parcours")
    ParcoursDemande parcours(HttpServletRequest request, Principal principal) {
        if ("true".equals(request.getHeader("HX-Request")) || !"GET".equals(request.getMethod())
                && !"POST".equals(request.getMethod())) {
            return null;
        }
        String chemin = request.getRequestURI().substring(request.getContextPath().length());
        EtapeParcours etape = EtapeParcours.pour(chemin).orElse(null);
        if (etape == null) {
            return null;
        }
        if (etape == EtapeParcours.COMPTE) {
            return ParcoursDemande.pour(etape, false, false, false);
        }
        UUID clientId = clientId(principal);
        if (clientId == null || demandes.parClient(clientId).stream().anyMatch(Demande::estActive)) {
            return null;
        }
        Set<TypeDocument> recus = documents.parClient(clientId).stream().map(Document::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TypeDocument.class)));
        return ParcoursDemande.pour(etape, true, emplois.parClient(clientId).isPresent(),
                recus.containsAll(EnumSet.allOf(TypeDocument.class)));
    }

    private static UUID clientId(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
