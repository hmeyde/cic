package com.seheli.identite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.seheli.identite.application.ClientsEnMemoire;
import com.seheli.identite.application.DocumentService;
import com.seheli.identite.application.DocumentsEnMemoire;
import com.seheli.identite.application.StockageEnMemoire;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.TypeDocument;

class JustificatifsClientControllerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private static final byte[] PDF = "%PDF-1.7\ncontenu".getBytes(StandardCharsets.US_ASCII);

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final DocumentsEnMemoire documents = new DocumentsEnMemoire();
    private final DocumentService service = new DocumentService(clients, documents, new StockageEnMemoire(), clock);
    private final JustificatifsClientController controller = new JustificatifsClientController(service, clock);
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Sidi", LocalDate.of(1990, 1, 15),
            "22123456", MAINTENANT);
    private final Client autre = Client.nouveau("1234567891", "Mint Salem", "Aicha", LocalDate.of(1992, 5, 2),
            "22123457", MAINTENANT);
    private final Principal principal = () -> client.id().toString();

    JustificatifsClientControllerTest() {
        clients.enregistrer(client);
        clients.enregistrer(autre);
    }

    private static MockMultipartFile fichier(byte[] contenu) {
        return new MockMultipartFile("fichier", "carte-sidi.pdf", "application/pdf", contenu);
    }

    @Test
    void le_client_envoie_puis_revoit_son_justificatif() throws IOException {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.envoyer(principal, "PIECE_IDENTITE", fichier(PDF), null, new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/client/justificatifs");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("client.justificatifs.envoye");

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.liste(principal, model)).isEqualTo("client/justificatifs");
        @SuppressWarnings("unchecked")
        List<LigneDocument> lignes = (List<LigneDocument>) model.get("documents");
        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l.type()).isEqualTo(TypeDocument.PIECE_IDENTITE);
            assertThat(l.format()).isEqualTo("PDF");
            assertThat(l.recuLe()).isEqualTo("11/11/2026 09:00");
            assertThat(l.tailleKo()).isEqualTo(1);
        });
        assertThat(model.get("typesRecus")).isEqualTo(Set.of(TypeDocument.PIECE_IDENTITE));

        ResponseEntity<byte[]> reponse = controller.voir(principal, lignes.getFirst().id());
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=\"piece-identite-2026-11-11.pdf\"");
        assertThat(reponse.getHeaders().getCacheControl()).contains("no-store");
        assertThat(reponse.getBody()).isEqualTo(PDF);
    }

    @Test
    void la_photo_prise_avec_le_telephone_est_envoyee_quand_aucun_fichier_n_est_choisi() throws IOException {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
        MockMultipartFile vide = new MockMultipartFile("fichier", "", "application/octet-stream", new byte[0]);
        MockMultipartFile photo = new MockMultipartFile("photo", "image.jpg", "image/jpeg", jpeg);

        assertThat(controller.envoyer(principal, "PIECE_IDENTITE", vide, photo, new ExtendedModelMap(),
                new RedirectAttributesModelMap())).isEqualTo("redirect:/client/justificatifs");

        assertThat(documents.parClient(client.id())).singleElement()
                .satisfies(d -> assertThat(d.type()).isEqualTo(TypeDocument.PIECE_IDENTITE));
    }

    @Test
    void propose_la_prochaine_piece_attendue() throws IOException {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.liste(principal, model);
        assertThat(model).containsEntry("typeChoisi", "PIECE_IDENTITE").containsEntry("nombreRecus", 0)
                .containsEntry("complets", false);

        controller.envoyer(principal, "PIECE_IDENTITE", fichier(PDF), null, new ExtendedModelMap(),
                new RedirectAttributesModelMap());
        model = new ExtendedModelMap();
        controller.liste(principal, model);
        assertThat(model).containsEntry("typeChoisi", "CONTRAT_OU_ATTESTATION").containsEntry("nombreRecus", 1);

        for (String type : new String[] {"CONTRAT_OU_ATTESTATION", "BULLETIN_PAIE", "RIB"}) {
            controller.envoyer(principal, type, fichier(PDF), null, new ExtendedModelMap(),
                    new RedirectAttributesModelMap());
        }
        model = new ExtendedModelMap();
        controller.liste(principal, model);
        assertThat(model).containsEntry("typeChoisi", "").containsEntry("complets", true);
    }

    @Test
    void reaffiche_la_page_avec_l_erreur() throws IOException {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.envoyer(principal, "", fichier(PDF), null, model, new RedirectAttributesModelMap()))
                .isEqualTo("client/justificatifs");
        assertThat(model).containsEntry("erreur", "client.justificatifs.type.obligatoire");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier(new byte[0]), null, model, new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.obligatoire")
                .containsEntry("typeChoisi", "RIB");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", null, null, model, new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.obligatoire");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier("GIF89a".getBytes(StandardCharsets.US_ASCII)), null, model,
                new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.format.refuse");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier(new byte[(int) Document.TAILLE_MAX_OCTETS + 1]), null, model,
                new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.trop-volumineux");

        assertThat(documents.parClient(client.id())).isEmpty();
    }

    @Test
    void le_document_d_un_autre_client_est_introuvable() {
        Document d = service.envoyer(autre.id(), TypeDocument.RIB, PDF);

        assertThatThrownBy(() -> controller.voir(principal, d.id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
