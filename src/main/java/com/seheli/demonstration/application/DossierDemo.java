package com.seheli.demonstration.application;

import java.util.Objects;
import java.util.UUID;

/** Client fictif suivi pendant la simulation : identifiants connus au fil des étapes. */
final class DossierDemo {

    private final ProfilClient profil;
    private UUID clientId;
    private UUID demandeId;

    DossierDemo(ProfilClient profil) {
        this.profil = Objects.requireNonNull(profil, "profil");
    }

    ProfilClient profil() {
        return profil;
    }

    UUID clientId() {
        return Objects.requireNonNull(clientId, "client pas encore créé");
    }

    UUID demandeId() {
        return Objects.requireNonNull(demandeId, "demande pas encore soumise");
    }

    void client(UUID id) {
        this.clientId = id;
    }

    /** Demande en cours : la plus récente soumise par ce client. */
    void demande(UUID id) {
        this.demandeId = id;
    }

    @Override
    public String toString() {
        return "DossierDemo[client " + clientId + ", demande " + demandeId + "]";
    }
}
