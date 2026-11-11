package com.cic.identite.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.ClientRepository;
import com.cic.identite.domain.CodeConnexion;
import com.cic.identite.domain.CodeConnexion.Verification;
import com.cic.identite.domain.CodeConnexionRepository;
import com.cic.identite.domain.GenerateurCode;
import com.cic.notification.domain.Message;
import com.cic.notification.domain.Notification;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

/**
 * Connexion du client par téléphone et code à usage unique.
 *
 * <p>Le code (6 chiffres) est envoyé par SMS via le port {@link Notification} ; sa durée de validité,
 * le nombre d'essais et le délai avant renvoi viennent de la table {@code parametre}.
 * Seul le hachage du code est conservé. Un numéro inconnu ne reçoit rien, mais l'appelant n'en sait
 * rien : on ne révèle pas quels numéros sont clients. Aucun téléphone n'est journalisé.
 */
@Service
public class ConnexionClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnexionClientService.class);

    private final ClientRepository clients;
    private final CodeConnexionRepository codes;
    private final GenerateurCode generateur;
    private final PasswordEncoder encodeur;
    private final Notification notification;
    private final ParametreService parametres;
    private final MessageSource messages;
    private final Clock clock;

    public ConnexionClientService(ClientRepository clients, CodeConnexionRepository codes, GenerateurCode generateur,
            PasswordEncoder encodeur, Notification notification, ParametreService parametres, MessageSource messages,
            Clock clock) {
        this.clients = clients;
        this.codes = codes;
        this.generateur = generateur;
        this.encodeur = encodeur;
        this.notification = notification;
        this.parametres = parametres;
        this.messages = messages;
        this.clock = clock;
    }

    /**
     * Envoie un code au client qui possède ce téléphone. Si un code envoyé il y a moins de
     * {@code connexion.code.renvoi.secondes} est encore utilisable, aucun nouveau code n'est envoyé.
     *
     * @return le téléphone normalisé, à conserver pour l'étape de saisie du code
     * @throws TelephoneInvalideException si le numéro est mal formé
     */
    @Transactional
    public String demanderCode(String telephoneSaisi, Locale langue) {
        String telephone = normaliser(telephoneSaisi);
        Optional<Client> client = clients.parTelephone(telephone);
        if (client.isEmpty()) {
            LOG.info("Code de connexion demandé pour un numéro inconnu : aucun envoi");
            return telephone;
        }
        Client c = client.get();
        Instant maintenant = clock.instant();
        Duration renvoi = Duration.ofSeconds(parametres.entier(CleParametre.CONNEXION_CODE_RENVOI_SECONDES));
        Optional<CodeConnexion> precedent = codes.parClient(c.id());
        if (precedent.isPresent() && !precedent.get().estInutilisableA(maintenant)
                && maintenant.isBefore(precedent.get().emisLe().plus(renvoi))) {
            LOG.info("Code de connexion redemandé trop tôt pour {} : le code en cours reste valable", c);
            return telephone;
        }
        int validiteMinutes = parametres.entier(CleParametre.CONNEXION_CODE_VALIDITE_MINUTES);
        int essaisMax = parametres.entier(CleParametre.CONNEXION_CODE_ESSAIS_MAX);
        String code = generateur.generer();
        codes.enregistrer(CodeConnexion.emettre(c.id(), encodeur.encode(code), maintenant,
                Duration.ofMinutes(validiteMinutes), essaisMax));
        String texte = messages.getMessage("client.connexion.code.sms", new Object[] {code, validiteMinutes}, langue);
        notification.envoyer(Message.sms(telephone, texte));
        return telephone;
    }

    /** Vérifie le code saisi pour ce téléphone ; chaque essai est décompté. */
    @Transactional
    public ResultatConnexion verifierCode(String telephoneSaisi, String codeSaisi) {
        Objects.requireNonNull(codeSaisi, "codeSaisi");
        String telephone = normaliser(telephoneSaisi);
        Optional<CodeConnexion> code = clients.parTelephone(telephone).flatMap(c -> codes.parClient(c.id()));
        if (code.isEmpty()) {
            // Numéro inconnu ou aucun code demandé : même réponse qu'un code expiré.
            return ResultatConnexion.echec(Verification.EXPIRE, 0);
        }
        CodeConnexion c = code.get();
        Verification verification = c.verifier(codeSaisi.replace(" ", ""), clock.instant(), encodeur::matches);
        codes.enregistrer(c);
        if (verification == Verification.VALIDE) {
            LOG.info("Connexion réussie de {}", c.clientId());
            return ResultatConnexion.valide(c.clientId());
        }
        LOG.info("Code de connexion refusé ({}) pour {}", verification, c.clientId());
        return ResultatConnexion.echec(verification, c.essaisRestants());
    }

    private static String normaliser(String telephone) {
        try {
            return Client.normaliserTelephone(telephone);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TelephoneInvalideException();
        }
    }
}
