package com.seheli.identite.adapter.in.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import com.seheli.identite.application.ClientService;
import com.seheli.identite.domain.Client;

/** Accueil et profil de l'espace client, réservés au client connecté. */
@Controller
class EspaceClientController {

    private final ClientService clients;

    EspaceClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping({"/client", "/client/"})
    String accueil(Principal principal, Model model) {
        model.addAttribute("prenom", clientConnecte(principal).prenom());
        return "client/accueil";
    }

    @GetMapping("/client/profil")
    String profil(Principal principal, Model model) {
        Client client = clientConnecte(principal);
        model.addAttribute("prenom", client.prenom());
        model.addAttribute("nom", client.nom());
        model.addAttribute("telephone", client.telephone());
        return "client/profil";
    }

    private Client clientConnecte(Principal principal) {
        return clients.parId(UUID.fromString(principal.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
