# Mettre Seheli en ligne — guide pas à pas

Ce guide part de zéro : rien n'est acheté, rien n'est installé. À la fin, l'application tourne sur
`https://<votre-domaine>` en HTTPS, avec les données de démonstration pour le jury.

Durée : environ **1 h 30** la première fois, dont une bonne partie d'attente (livraison du serveur,
propagation du domaine, première compilation).

---

## 0. Ce qu'il faut acheter (mon choix pour vous)

Tout chez **Contabo** : serveur, domaine et DNS sur **un seul compte**, une seule facture, un seul espace client.

| Quoi | Offre | Lien | Prix indicatif* |
|---|---|---|---|
| **Serveur (VPS)** | **Cloud VPS 4** : 4 vCPU, 8 Go de RAM, 100 Go SSD, trafic illimité, Europe (Allemagne) | https://contabo.com/fr/vps/ | ~5,50 € TTC / mois + frais d'installation uniques en paiement mensuel (de l'ordre de 5 €) |
| **Nom de domaine** | un `.com` (par exemple `cic-credit.com`), DNS gratuit inclus | https://contabo.com/fr/domains/ | ~1,99 € / mois (~24 € / an) |
| **Dépôt du code** | GitHub — dépôt **privé** | https://github.com/new | gratuit |

\* Prix relevés en novembre 2026 sur les pages publiques ; **vérifiez le montant exact au moment de la commande**.
Budget du premier mois : environ **15 €**.

**Pourquoi ces choix**

- **Contabo** : le moins cher pour 8 Go de RAM, centre de données en Europe, offres disponibles. Le domaine
  ne peut s'y acheter qu'avec un autre produit Contabo : on commande donc le serveur d'abord.
- **8 Go de RAM** : le serveur compile lui-même l'application (Maven dans Docker) ; en dessous de 4 Go,
  la compilation peut échouer.
- **GitHub privé** : le serveur y récupère le code ; privé, car le dépôt contient le code complet du projet.

Limites de Contabo, sans gravité pour une démonstration : processeur partagé (parfois plus lent aux heures
de pointe) et support moins réactif que les grands hébergeurs.

> Plus fiable mais plus cher : **DigitalOcean**, Droplet 4 Go (2 vCPU, 80 Go) à 24 $ / mois, **facturé à
> l'heure** (on peut ne l'allumer que pendant le challenge), régions Francfort, Amsterdam ou Londres —
> https://www.digitalocean.com/pricing/droplets. Le reste du guide s'applique aussi.

> Ne prenez **pas** d'option payante supplémentaire (panneau de contrôle, IP additionnelle, sauvegarde
> automatique, stockage objet) : l'application n'en a pas besoin.

---

## 1. Créer le compte Contabo et commander

### 1.1 Le serveur

1. Allez sur https://contabo.com/fr/vps/ → **Cloud VPS 4** → **« Sélectionner »**.
2. Choisissez :
   - durée : **1 mois** ;
   - région : **European Union** ;
   - image : **Ubuntu 24.04** ;
   - **mot de passe root** : un mot de passe solide, à garder dans un gestionnaire de mots de passe ;
   - aucune option payante.
3. Créez le compte (e-mail, coordonnées, **pays : Mauritanie**) et payez par carte bancaire internationale.
4. Contabo envoie un **e-mail de livraison avec l'adresse IP** du serveur, souvent sous quelques heures.
   Pour une première commande, une **vérification d'identité** peut être demandée : répondez vite, le serveur
   n'est livré qu'après.

Notez l'**adresse IP** (dans l'e-mail Contabo ou sur https://my.contabo.com) : partout dans ce guide, remplacez `VOTRE_IP` par cette adresse (par exemple `ssh root@185.123.45.67`).

### 1.2 Le domaine

Une fois le serveur commandé : https://contabo.com/fr/domains/ (ou, dans l'espace client, la rubrique
**Domaines**). Cherchez le nom voulu, commandez le `.com`, sans option.

### 1.3 Créer l'utilisateur `ubuntu` sur le serveur

Contabo ne fournit que le compte `root` ; la suite du guide utilise un utilisateur `ubuntu`. Depuis
PowerShell sur votre PC :

```powershell
ssh root@VOTRE_IP
```

(Répondez `yes` à la question sur l'empreinte, puis saisissez le mot de passe root.) Sur le serveur :

```bash
adduser --gecos "" ubuntu            # choisir un mot de passe
usermod -aG sudo ubuntu
exit
```

> **Clé SSH (conseillé, pour ne plus taper de mot de passe)** — dans PowerShell :
> ```powershell
> ssh-keygen -t ed25519          # Entrée trois fois
> type $HOME\.ssh\id_ed25519.pub | ssh ubuntu@VOTRE_IP "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys"
> ```

---

## 2. Faire pointer le domaine vers le serveur

Dans l'espace client Contabo (https://my.contabo.com) : rubrique **DNS** (gestion des zones DNS).

1. **Créez une zone DNS** pour votre domaine si elle n'existe pas encore.
2. Ajoutez (ou modifiez) un enregistrement de type **A** :
   - nom : *vide* ou `@` (pour `cic-credit.com`) ;
   - valeur : **l'adresse IP du serveur** ;
   - TTL : par défaut.
3. Supprimez tout autre enregistrement **A** ou **AAAA** du domaine nu qui pointerait ailleurs.
4. Attendez la propagation (de quelques minutes à quelques heures). Vérification depuis PowerShell :
   ```powershell
   nslookup cic-credit.com
   ```
   La réponse doit afficher l'IP du serveur.

> Vous préférez un sous-domaine (`demo.cic-credit.com`) ? Mettez `demo` comme nom de l'enregistrement et
> utilisez `demo.cic-credit.com` partout où ce guide écrit « votre domaine ».

**Ne passez pas à l'étape 5 tant que `nslookup` ne renvoie pas la bonne IP** : sans cela, le certificat
HTTPS ne peut pas être obtenu.

### 2.1 Mise en ligne sans nom de domaine (sslip.io)

Pas encore de domaine ? Le service gratuit [sslip.io](https://sslip.io) fait pointer tout nom
`<IP>.sslip.io` vers l'adresse `<IP>` : Caddy obtient alors un vrai certificat Let's Encrypt sans aucun
réglage DNS.

1. Dans `.env` (étape 5.1), mettez l'IP du serveur suivie de `.sslip.io` :
   ```bash
   SEHELI_DOMAINE=VOTRE_IP.sslip.io        # par exemple 203.0.113.10.sslip.io
   ```
2. Les ports **80** et **443** doivent être ouverts vers le serveur (pare-feu `ufw` de l'étape 4.1, et
   pare-feu de l'hébergeur s'il y en a un) : Let's Encrypt s'y connecte pour valider le nom.
3. Si l'application tourne déjà, recréez seulement Caddy pour qu'il lise la nouvelle valeur :
   ```bash
   docker compose up -d --force-recreate caddy
   docker compose logs -f caddy              # attendre « certificate obtained successfully »
   ```
4. Ouvrez `https://VOTRE_IP.sslip.io`.

Le jour où vous achetez un domaine, remplacez simplement `SEHELI_DOMAINE` dans `.env` et relancez la même
commande `--force-recreate caddy`.

---

## 3. Mettre le code sur GitHub

1. Créez un compte sur https://github.com si besoin, puis un dépôt sur https://github.com/new :
   - nom : `cic` ;
   - visibilité : **Private** ;
   - **ne cochez rien** (pas de README, pas de .gitignore, pas de licence).
2. Envoyez le code. Le plus simple depuis **IntelliJ** (sur Windows) :
   **Git → Manage Remotes → +** et collez l'URL du dépôt (`https://github.com/<vous>/cic.git`), puis
   **Git → Push** (IntelliJ ouvre le navigateur pour vous connecter à GitHub la première fois).

   Ou en ligne de commande, à la racine du projet :
   ```bash
   git remote add origin https://github.com/<vous>/cic.git
   git push -u origin master
   ```
   (GitHub demande un *personal access token* comme mot de passe :
   https://github.com/settings/tokens → « Generate new token (classic) », case `repo`.)
3. Rafraîchissez la page du dépôt sur GitHub : les fichiers doivent apparaître.

> Vérifiez qu'**aucun fichier `.env`** n'apparaît sur GitHub. Il est ignoré par `.gitignore`, mais un
> coup d'œil ne coûte rien : ce fichier contiendra les mots de passe du serveur.

---

## 4. Préparer le serveur

Connectez-vous depuis PowerShell (remplacez par votre IP) :

```powershell
ssh ubuntu@VOTRE_IP
```

Toutes les commandes suivantes se tapent **sur le serveur**.

### 4.1 Mises à jour, Docker, pare-feu

```bash
sudo apt-get update && sudo apt-get -y upgrade
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
sudo apt-get install -y git

sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw --force enable

exit
```

Reconnectez-vous (`ssh ubuntu@VOTRE_IP`) pour que le groupe `docker` soit pris en compte, puis
vérifiez : `docker ps` doit répondre sans erreur.

### 4.2 Autoriser le serveur à lire le dépôt GitHub (clé de déploiement)

Le dépôt est privé : on donne au serveur une clé **en lecture seule**, limitée à ce dépôt.

```bash
ssh-keygen -t ed25519 -f ~/.ssh/github_cic -N ""
cat ~/.ssh/github_cic.pub
```

Sur GitHub : votre dépôt → **Settings → Deploy keys → Add deploy key**. Titre : `serveur CIC` ; collez la
ligne affichée ; **laissez « Allow write access » décoché** ; Add key.

Puis, sur le serveur :

```bash
cat >> ~/.ssh/config <<'EOF'
Host github.com
    IdentityFile ~/.ssh/github_cic
    IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config

git clone git@github.com:<vous>/cic.git
cd cic
```

(Répondez `yes` à la question sur l'empreinte de github.com.)

---

## 5. Configurer et lancer l'application

### 5.1 Le fichier `.env`

```bash
cp .env.example .env
chmod 600 .env
for i in 1 2 3 4 5; do openssl rand -base64 18; done
```

La dernière commande affiche **5 mots de passe** : un pour la base, un pour chaque compte du back-office.
Notez-les dans un gestionnaire de mots de passe, puis éditez le fichier :

```bash
nano .env
```

Changez **seulement** ces lignes :

| Ligne | Valeur |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `demo` (démonstration pour le jury) |
| `SEHELI_DOMAINE` | votre domaine, par exemple `cic-credit.com` |
| `DB_PASSWORD` | mot de passe n° 1 |
| `SEHELI_BO_ADMIN_MOT_DE_PASSE` | mot de passe n° 2 |
| `SEHELI_BO_ANALYSTE_MOT_DE_PASSE` | mot de passe n° 3 |
| `SEHELI_BO_RECOUVREMENT_MOT_DE_PASSE` | mot de passe n° 4 |
| `SEHELI_BO_ASSOCIE_MOT_DE_PASSE` | mot de passe n° 5 |

Enregistrez avec **Ctrl+O**, Entrée, puis quittez avec **Ctrl+X**. Il ne doit plus rester aucun `A_CHANGER` :

```bash
grep A_CHANGER .env || echo "OK, plus rien à changer"
```

### 5.2 Démarrer

```bash
docker compose up -d --build
```

La première fois, la construction prend **5 à 15 minutes** (téléchargement de Maven, des dépendances et
compilation). Suivez le démarrage :

```bash
docker compose logs -f app caddy
```

Attendez `Started SeheliApplication` côté `app`, et `certificate obtained successfully` côté `caddy`, puis
**Ctrl+C** pour quitter l'affichage (l'application continue de tourner).

---

## 6. Vérifier

```bash
docker compose ps
```

Les quatre services `db`, `app`, `sauvegarde` et `caddy` doivent être `running`, et `app` doit être
`(healthy)`.

```bash
curl -s https://cic-credit.com/actuator/health
```

Réponse attendue : `{"status":"UP"}`.

Puis, depuis votre téléphone :

1. Ouvrez `https://cic-credit.com` : le cadenas HTTPS doit être présent.
2. Changez la langue en arabe : la page passe de droite à gauche.
3. **Espace client** : téléphone `22100006`, code `000000` : le dossier « commandé » s'affiche.
4. **Back-office** (`/bo/login`) : identifiant `analyste` avec le mot de passe n° 3.
5. Le navigateur propose « Installer l'application » ou « Ajouter à l'écran d'accueil » (PWA).

---

## 7. Accès à remettre au jury

| Espace | Adresse | Accès |
|---|---|---|
| Accueil | `https://cic-credit.com` | — |
| Client | `https://cic-credit.com/client` | téléphones `22000001` à `22100010` (liste dans le README, section 1), code **`000000`** |
| Back-office | `https://cic-credit.com/bo/login` | `admin`, `analyste`, `recouvrement`, `associe` avec les mots de passe 2 à 5 |
| Pré-inscription | `https://cic-credit.com/inscription` | sans compte |

Pour un parcours complet en direct (de la demande au remboursement), utilisez le client **`22000001`**,
qui n'a aucun dossier. Le bouton **« Réinitialiser la démo »** (compte `admin`) remet tout à l'état initial
entre deux passages.

---

## 8. Après la mise en ligne

### Mettre à jour après une modification du code

Sur votre PC : commit puis push vers GitHub. Puis sur le serveur :

```bash
cd ~/cic
git pull
docker compose up -d --build
```

Les migrations de base s'appliquent toutes seules ; les données sont conservées.

### Passer de CIC à Seheli sur un serveur existant

Les variables d'environnement `CIC_*` s'appellent désormais `SEHELI_*`, et l'image Docker `cic:latest`
devient `seheli:latest`. Le nom du projet Compose reste figé à `cic` (clé `name: cic` de
`docker-compose.prod.yml`) : les volumes `cic_pgdata`, `cic_justificatifs`, `cic_sauvegardes` et
`cic_caddy_data` sont repris tels quels, **aucune donnée n'est perdue** et le certificat HTTPS est conservé.

Une seule fois, sur le serveur :

```bash
cd ~/cic
cp .env .env.avant-seheli                 # copie de secours (contient des secrets : à supprimer ensuite)
sed -i 's/^CIC_/SEHELI_/' .env            # CIC_DOMAINE -> SEHELI_DOMAINE, CIC_BO_* -> SEHELI_BO_*
grep -c '^SEHELI_' .env                   # 9 lignes attendues
git pull
docker compose up -d --build
```

Sans le `sed`, `docker compose` s'arrête sur `SEHELI_BO_ADMIN_MOT_DE_PASSE manquant dans .env` : rien
n'est modifié, il suffit de lancer le `sed` puis de recommencer. Une fois `app` revenu en `(healthy)` :
`rm .env.avant-seheli` et, pour libérer de la place, `docker image rm cic:latest`.

### Sauvegardes

Une sauvegarde de la base est faite **chaque nuit à 02:00 UTC** et gardée 14 jours, **sur le serveur**.
Avant une présentation importante, faites-en une à la main et rapatriez-la sur votre PC :

```bash
docker compose exec sauvegarde bash /usr/local/bin/sauvegarde-base.sh
docker compose cp sauvegarde:/sauvegardes ./sauvegardes
```

```powershell
# sur votre PC
scp -r ubuntu@VOTRE_IP:cic/sauvegardes .
```

### Commandes utiles

| Besoin | Commande (dans `~/cic`) |
|---|---|
| État | `docker compose ps` |
| Journaux | `docker compose logs -f app` |
| Redémarrer l'application | `docker compose restart app` |
| Tout arrêter | `docker compose down` |
| Tout relancer | `docker compose up -d` |

---

## 9. Problèmes fréquents

| Symptôme | Cause probable | Que faire |
|---|---|---|
| Le navigateur affiche « connexion non sécurisée » | DNS pas encore propagé quand Caddy a démarré | Vérifier `nslookup`, puis `docker compose restart caddy` |
| `caddy` journalise des erreurs `acme` / `challenge` | Ports 80/443 fermés ou domaine qui ne pointe pas vers le VPS | `sudo ufw status` ; revoir l'étape 2 |
| La construction s'arrête (`killed`, `exit code 137`) | Manque de mémoire | Vérifier que le VPS a bien **4 Go** ; relancer `docker compose up -d --build` |
| `app` reste `unhealthy` | Erreur au démarrage | `docker compose logs app \| tail -50` |
| `DB_PASSWORD manquant dans .env` | Ligne oubliée dans `.env` | Revoir l'étape 5.1 |
| `git clone` : `Permission denied (publickey)` | Clé de déploiement absente ou mal configurée | Revoir l'étape 4.2 |

---

## 10. À savoir avant d'ouvrir le site au public

- **Le profil `demo` est fait pour la démonstration, pas pour de vrais clients.** N'importe qui peut entrer
  dans un compte client fictif avec le code `000000`, et le bouton de réinitialisation efface tout. N'y
  saisissez **jamais de vraies données personnelles**.
- **Pas de vrais SMS** : l'application journalise les messages au lieu de les envoyer. Un passage en profil
  `prod` avec de vrais clients demande d'abord de brancher un fournisseur SMS.
- **Après le challenge**, arrêtez le site (`docker compose down`) puis, si vous n'en avez plus besoin,
  **résiliez le serveur et le domaine** dans l'espace client Contabo (https://my.contabo.com) avant la fin
  du mois payé, pour ne pas être facturé le mois suivant.
- Pour une exploitation réelle (profil `prod`), voir la section « Profil `prod` » du README : comptes du
  back-office à créer en base, sauvegardes à copier hors du serveur.

---

*Sources des prix : [Contabo VPS](https://contabo.com/fr/vps/),
[Contabo domaines](https://contabo.com/en/domains/),
[DigitalOcean Droplets](https://www.digitalocean.com/pricing/droplets). Écartés au moment de la rédaction
(novembre 2026) : OVHcloud (VPS-1 et VPS-2 indisponibles dans toutes les localisations) et Hetzner (offres
cloud indiquées « indisponibles », [page Hetzner](https://www.hetzner.com/cloud/regular-performance/)).*
