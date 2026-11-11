package com.cic.identite.adapter.in.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import com.cic.identite.application.ClientService;
import com.cic.identite.domain.Client;

/** Accueil de l'espace client, réservé au client connecté. */
@Controller
class EspaceClientController {

    private final ClientService clients;

    EspaceClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping({"/client", "/client/"})
    String accueil(Principal principal, Model model) {
        Client client = clients.parId(UUID.fromString(principal.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        model.addAttribute("prenom", client.prenom());
        return "client/accueil";
    }
}
