package com.seheli.identite.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.ClientRepository;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.DocumentRepository;
import com.seheli.identite.domain.FormatDocument;
import com.seheli.identite.domain.StockageDocuments;
import com.seheli.identite.domain.TypeDocument;

/**
 * Justificatifs des clients : envoi et consultation par le client propriétaire, consultation par le
 * back-office.
 *
 * <p>Le format (PDF, JPEG, PNG) est reconnu au contenu ; 5 Mo au plus ; au plus
 * {@value #MAX_PAR_TYPE} documents de chaque type par client (recto et verso, plusieurs bulletins…).
 * Un client ne lit que ses propres documents : un document d'un autre client est traité comme
 * introuvable.
 */
@Service
public class DocumentService {

    /** Nombre maximal de documents d'un même type pour un client. */
    public static final int MAX_PAR_TYPE = 10;

    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    private final ClientRepository clients;
    private final DocumentRepository documents;
    private final StockageDocuments stockage;
    private final Clock clock;

    public DocumentService(ClientRepository clients, DocumentRepository documents, StockageDocuments stockage,
            Clock clock) {
        this.clients = clients;
        this.documents = documents;
        this.stockage = stockage;
        this.clock = clock;
    }

    /**
     * Enregistre un justificatif du client.
     *
     * @throws DocumentVideException          contenu absent ou vide
     * @throws DocumentTropVolumineuxException plus de 5 Mo
     * @throws FormatDocumentRefuseException  ni PDF, ni JPEG, ni PNG
     * @throws TropDeDocumentsException       déjà {@value #MAX_PAR_TYPE} documents de ce type
     * @throws IllegalArgumentException       client inconnu
     */
    @Transactional
    public Document envoyer(UUID clientId, TypeDocument type, byte[] contenu) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(type, "type");
        if (clients.parId(clientId).isEmpty()) {
            throw new IllegalArgumentException("Client inconnu");
        }
        if (contenu == null || contenu.length == 0) {
            throw new DocumentVideException();
        }
        if (contenu.length > Document.TAILLE_MAX_OCTETS) {
            throw new DocumentTropVolumineuxException();
        }
        FormatDocument format = FormatDocument.detecter(contenu).orElseThrow(FormatDocumentRefuseException::new);
        if (documents.compterParClientEtType(clientId, type) >= MAX_PAR_TYPE) {
            throw new TropDeDocumentsException();
        }
        Document document = Document.nouveau(clientId, type, format, contenu.length, Instant.now(clock));
        stockage.ecrire(document.id(), contenu);
        try {
            documents.enregistrer(document);
        } catch (RuntimeException e) {
            stockage.supprimer(document.id());
            throw e;
        }
        LOG.info("Justificatif reçu : {}", document);
        return document;
    }

    /** Documents du client, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<Document> parClient(UUID clientId) {
        return documents.parClient(clientId);
    }

    /** Contenu d'un document du client ; vide si le document n'existe pas ou appartient à un autre client. */
    @Transactional(readOnly = true)
    public Optional<ContenuDocument> contenuPourClient(UUID clientId, UUID documentId) {
        return documents.parId(documentId)
                .filter(d -> d.appartientA(clientId))
                .map(this::avecContenu);
    }

    /** Derniers justificatifs reçus, pour le back-office. */
    @Transactional(readOnly = true)
    public List<DocumentRecu> derniersRecus(int limite) {
        return documents.derniers(limite).stream().map(d -> {
            Optional<Client> client = clients.parId(d.clientId());
            return new DocumentRecu(d, client.map(Client::prenom).orElse(""), client.map(Client::nom).orElse(""));
        }).toList();
    }

    /** Contenu d'un document pour le back-office. */
    @Transactional(readOnly = true)
    public Optional<ContenuDocument> contenuPourBackOffice(UUID documentId) {
        Optional<ContenuDocument> contenu = documents.parId(documentId).map(this::avecContenu);
        contenu.ifPresent(c -> LOG.info("Justificatif consulté par le back-office : {}", c.document()));
        return contenu;
    }

    private ContenuDocument avecContenu(Document document) {
        return new ContenuDocument(document, stockage.lire(document.id()));
    }
}
