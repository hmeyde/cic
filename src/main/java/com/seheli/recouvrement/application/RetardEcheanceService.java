package com.seheli.recouvrement.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.application.RetardDossierService;
import com.seheli.demande.application.StatutDossierService;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.domain.Client;
import com.seheli.notification.domain.Message;
import com.seheli.notification.domain.Notification;
import com.seheli.paiement.domain.ReferenceEcheance;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.recouvrement.domain.CalendrierRelances;
import com.seheli.recouvrement.domain.LienWhatsApp;
import com.seheli.recouvrement.domain.Relance;
import com.seheli.recouvrement.domain.RelanceDue;
import com.seheli.recouvrement.domain.RelanceRepository;

/**
 * Retards de paiement : détection des échéances impayées et relances du client.
 *
 * <ol>
 *   <li><b>Détection</b> : une échéance encore A_VENIR le lendemain de sa date n'a pas été payée à
 *       la date prévue ; elle passe EN_RETARD, et son dossier EN_REMBOURSEMENT passe EN_RETARD
 *       (journal d'audit, événement). Seuls les dossiers dont les prélèvements sont en place sont
 *       concernés ({@link StatutDossier#estEnPrelevement()}) : les échéances d'un dossier annulé ne
 *       sont jamais en retard. Le montant ne change pas : aucune pénalité.</li>
 *   <li><b>Rattrapage</b> : un dossier livré après la date d'une échéance déjà EN_RETARD passe
 *       EN_RETARD dès que son remboursement a démarré.</li>
 *   <li><b>Relances</b> : SMS à J+{@code relance.echec.premiere.jours} (3) puis
 *       J+{@code relance.echec.seconde.jours} (7) après la date d'une échéance toujours impayée, avec
 *       la référence Masrvi pour la régler et un lien WhatsApp vers Seheli
 *       ({@link CalendrierRelances}). Chaque relance est enregistrée dans la même transaction que
 *       l'envoi : une seule par échéance et par type.</li>
 * </ol>
 *
 * Tant qu'une échéance est EN_RETARD, le client ne peut plus faire de demande
 * ({@link com.seheli.demande.application.ClientEnRetardException}).
 */
@Service
public class RetardEcheanceService {

    private static final Logger LOG = LoggerFactory.getLogger(RetardEcheanceService.class);
    private static final Locale LANGUE = Locale.FRENCH;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratService contrats;
    private final StatutDossierService statuts;
    private final RetardDossierService retards;
    private final ClientService clients;
    private final RelanceRepository relances;
    private final Notification notification;
    private final ParametreService parametres;
    private final MessageSource messages;
    private final Clock clock;

    public RetardEcheanceService(ContratService contrats, StatutDossierService statuts, RetardDossierService retards,
            ClientService clients, RelanceRepository relances, Notification notification, ParametreService parametres,
            MessageSource messages, Clock clock) {
        this.contrats = contrats;
        this.statuts = statuts;
        this.retards = retards;
        this.clients = clients;
        this.relances = relances;
        this.notification = notification;
        this.parametres = parametres;
        this.messages = messages;
        this.clock = clock;
    }

    /** Échéances encore A_VENIR dont la date est passée, par date. */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesImpayees() {
        return contrats.echeancesAVenirAvant(LocalDate.now(clock));
    }

    /**
     * L'échéance n'a pas été payée à sa date : elle passe EN_RETARD et son dossier aussi s'il est
     * EN_REMBOURSEMENT, dans une seule transaction.
     *
     * @return {@code true} si l'échéance vient de passer EN_RETARD
     */
    @Transactional
    public boolean constaterRetard(EcheanceAPrelever a) {
        Objects.requireNonNull(a, "a");
        if (!enPrelevement(a)) {
            return false;
        }
        Echeance e = a.echeance();
        if (!contrats.constaterEcheanceEnRetard(a.contratId(), e.rang())) {
            return false;
        }
        retards.constaterRetard(a.demandeId(), detail(a));
        LOG.info("Retard constaté : échéance {}/{} du contrat {}, prévue le {}", e.rang(), a.nombreEcheances(),
                a.numeroContrat(), e.date());
        return true;
    }

    /** Échéances EN_RETARD, de la plus ancienne à la plus récente. */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesEnRetard() {
        return contrats.echeancesEnRetardEntre(LocalDate.EPOCH, LocalDate.now(clock));
    }

    /**
     * Passe EN_RETARD le dossier d'une échéance EN_RETARD s'il est EN_REMBOURSEMENT (livré après la
     * date de l'échéance).
     *
     * @return {@code true} si le dossier vient de passer EN_RETARD
     */
    @Transactional
    public boolean rattraperDossier(EcheanceAPrelever a) {
        Objects.requireNonNull(a, "a");
        return retards.constaterRetard(a.demandeId(), detail(a));
    }

    /** Relances après échec à envoyer aujourd'hui, par date d'échéance. */
    @Transactional(readOnly = true)
    public List<RelanceDue> relancesDues() {
        LocalDate aujourdhui = LocalDate.now(clock);
        CalendrierRelances calendrier = calendrier();
        return contrats.echeancesEnRetardEntre(calendrier.plusAncienneEcheance(aujourdhui),
                calendrier.plusRecenteEcheance(aujourdhui)).stream()
                .flatMap(a -> calendrier.relanceDue(a.echeance().date(), aujourdhui)
                        .map(type -> new RelanceDue(a, type)).stream())
                .toList();
    }

    /**
     * Envoie la relance si elle n'a pas déjà été envoyée et que le dossier est en prélèvement.
     *
     * @return {@code true} si le message vient d'être envoyé
     */
    @Transactional
    public boolean relancer(RelanceDue due) {
        Objects.requireNonNull(due, "due");
        EcheanceAPrelever a = due.echeance();
        Echeance e = a.echeance();
        if (relances.existe(e.id(), due.type()) || !enPrelevement(a)) {
            return false;
        }
        Optional<Client> client = clients.parId(a.clientId());
        if (client.isEmpty()) {
            LOG.warn("Relance de l'échéance {} du contrat {} impossible : client introuvable", e.rang(),
                    a.numeroContrat());
            return false;
        }
        Instant maintenant = clock.instant();
        relances.enregistrer(Relance.apresEchec(due.type(), e.id(), a.contratId(), e.date(), maintenant));
        notification.envoyer(Message.sms(client.get().telephone(), texte(due)));
        LOG.info("Relance {} envoyée : échéance {}/{} du contrat {}, prévue le {}", due.type(), e.rang(),
                a.nombreEcheances(), a.numeroContrat(), e.date());
        return true;
    }

    private boolean enPrelevement(EcheanceAPrelever a) {
        return statuts.statut(a.demandeId()).filter(StatutDossier::estEnPrelevement).isPresent();
    }

    private CalendrierRelances calendrier() {
        return new CalendrierRelances(parametres.entier(CleParametre.RELANCE_ECHEC_PREMIERE_JOURS),
                parametres.entier(CleParametre.RELANCE_ECHEC_SECONDE_JOURS));
    }

    private static String detail(EcheanceAPrelever a) {
        Echeance e = a.echeance();
        return "Échéance " + e.rang() + "/" + a.nombreEcheances() + " du contrat " + a.numeroContrat()
                + " impayée à sa date du " + DATE.format(e.date());
    }

    private String texte(RelanceDue due) {
        EcheanceAPrelever a = due.echeance();
        Echeance e = a.echeance();
        String rang = String.valueOf(e.rang());
        String total = String.valueOf(a.nombreEcheances());
        String date = DATE.format(e.date());
        String question = messages.getMessage("recouvrement.rappel.prelevement.whatsapp",
                new Object[] {rang, total, date, a.numeroContrat()}, LANGUE);
        String lien = LienWhatsApp.vers(parametres.texte(CleParametre.CIC_WHATSAPP_NUMERO), question);
        String reference = ReferenceEcheance.pourEcheance(a.numeroContrat(), e.rang()).valeur();
        return messages.getMessage("recouvrement.relance." + due.type().name() + ".sms",
                new Object[] {rang, total, e.montant().valeur().toPlainString(), a.numeroContrat(), date,
                        parametres.texte(CleParametre.MASRVI_CODE_MARCHAND), reference, lien},
                LANGUE);
    }
}
