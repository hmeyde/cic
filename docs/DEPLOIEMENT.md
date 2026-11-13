# Mettre CIC en ligne — guide pas à pas

Ce guide part de zéro : rien n'est acheté, rien n'est installé. À la fin, l'application tourne sur
`https://<votre-domaine>` en HTTPS, avec les données de démonstration pour le jury.

Durée : environ **1 h 30** la première fois, dont une bonne partie d'attente (activation du serveur,
propagation du domaine, première compilation).

---

## 0. Ce qu'il faut acheter (mon choix pour vous)

| Quoi | Fournisseur et offre | Lien | Prix indicatif* |
|---|---|---|---|
| **Serveur (VPS)** | OVHcloud — **VPS-1** : 2 vCores, 4 Go de RAM, 40 Go SSD NVMe, trafic illimité, sauvegarde quotidienne incluse | https://www.ovhcloud.com/fr/vps/ | ~4 € HT / mois |
| **Nom de domaine** | OVHcloud — un `.com` (par exemple `cic-credit.com`) | https://www.ovhcloud.com/fr/domains/ | ~10 à 15 € / an |
| **Dépôt du code** | GitHub — dépôt **privé** | https://github.com/new | gratuit |

\* Prix relevés en novembre 2026 sur les pages publiques ; **vérifiez le montant exact au moment de la commande**.
Budget de départ : environ **20 €** (domaine pour un an + premier mois de serveur).

**Pourquoi ces choix**

- **OVHcloud** : hébergeur français, interface et support en français, serveur et domaine sur le même compte
  (une seule facture, réglage DNS au même endroit). Paiement par carte bancaire internationale.
- **VPS-1 avec 4 Go de RAM** : le serveur compile lui-même l'application (Maven dans Docker) ; avec moins de
  4 Go, la compilation peut échouer. C'est aussi largement suffisant pour la démonstration.
- **GitHub privé** : le serveur y récupère le code ; privé, car le dépôt contient le code complet du projet.

> **VPS-1 indisponible ?** OVHcloud affiche parfois « VPS-1 est actuellement indisponible ». Essayez d'abord
> une **autre localisation en Europe** (Gravelines, Roubaix, Strasbourg, Francfort, Varsovie, Londres…) ;
> sinon le **VPS-2** (4 vCores, 8 Go de RAM, 75 Go NVMe, ~7 € HT / mois) : le reste du guide est identique.
> Évitez tout modèle à moins de 4 Go de RAM et les localisations hors d'Europe.
>
> **Si toutes les offres VPS d'OVHcloud sont désactivées**, prenez le serveur chez **Contabo** (section 1 bis)
> et gardez **le domaine chez OVHcloud** : seule l'étape de commande du serveur change.

> Ne prenez **pas** d'option payante supplémentaire (Plesk, cPanel, sauvegarde avancée, IP additionnelle) :
> l'application n'en a pas besoin.

---

## 1. Créer le compte OVHcloud et commander

1. Créez un compte sur https://www.ovhcloud.com/fr/ (bouton « Mon compte » → « Créer un compte »).
2. **Commandez le VPS** sur https://www.ovhcloud.com/fr/vps/ :
   - modèle : **VPS-1** ;
   - localisation : **France** (Gravelines, Roubaix ou Strasbourg — la plus proche de la Mauritanie parmi
     les centres européens) ;
   - système : **Ubuntu 24.04** (sans application préinstallée) ;
   - clé SSH : facultatif mais conseillé, voir l'encadré ci-dessous ;
   - durée : **sans engagement** (mensuel).
3. **Commandez le domaine** sur https://www.ovhcloud.com/fr/domains/ : cherchez le nom voulu, ajoutez le
   `.com` au panier, sans option (pas d'hébergement web ni d'e-mail).
4. Payez. Le VPS est livré en quelques minutes à une heure : OVHcloud envoie un **e-mail avec l'adresse IP**
   du serveur, l'utilisateur (**`ubuntu`**) et un lien pour définir le mot de passe.

> **Clé SSH (conseillé)** — dans PowerShell sur votre PC Windows :
> ```powershell
> ssh-keygen -t ed25519          # Entrée trois fois
> Get-Content $HOME\.ssh\id_ed25519.pub
> ```
> Copiez la ligne affichée (`ssh-ed25519 AAAA…`) dans le champ « clé SSH » de la commande du VPS.
> Vous vous connecterez ensuite sans mot de passe.

Notez l'**adresse IP** du serveur, par exemple `51.75.12.34` : elle sert plusieurs fois ensuite.

---

## 1 bis. Serveur de remplacement : Contabo (si OVHcloud est en rupture)

| Quoi | Offre | Lien | Prix indicatif* |
|---|---|---|---|
| **Serveur** | Contabo — **Cloud VPS 4** : 4 vCPU, 8 Go de RAM, 100 Go SSD, trafic illimité | https://contabo.com/fr/vps/ | ~5,50 € TTC / mois, plus des frais d'installation uniques en paiement mensuel (de l'ordre de 5 €, à vérifier) |

Plus fiable mais plus cher : **DigitalOcean**, Droplet 4 Go (2 vCPU, 80 Go) à 24 $ / mois, facturé à l'heure,
régions Francfort, Amsterdam ou Londres — https://www.digitalocean.com/pricing/droplets.

**Commande Contabo**

1. https://contabo.com/fr/vps/ → **Cloud VPS 4** → « Sélectionner ».
2. Durée **1 mois** ; région **European Union** ; image **Ubuntu 24.04** ; un **mot de passe root** solide ;
   aucune option payante.
3. Créez le compte et payez. L'e-mail de livraison donne l'**adresse IP** (souvent sous quelques heures ;
   une vérification d'identité peut être demandée pour une première commande).

**Créer l'utilisateur `ubuntu`** (le reste du guide l'utilise ; chez Contabo on reçoit seulement `root`).
Depuis PowerShell :

```powershell
ssh root@<IP-du-serveur>
```

puis, sur le serveur :

```bash
adduser --gecos "" ubuntu            # choisir un mot de passe
usermod -aG sudo ubuntu
mkdir -p /home/ubuntu/.ssh
cp ~/.ssh/authorized_keys /home/ubuntu/.ssh/ 2>/dev/null || true
chown -R ubuntu:ubuntu /home/ubuntu/.ssh
exit
```

Ensuite, suivez le guide normalement à partir de l'étape 2 (le domaine reste chez OVHcloud : l'enregistrement
**A** pointe simplement vers l'IP Contabo), en vous connectant avec `ssh ubuntu@<IP-du-serveur>`.

---

## 2. Faire pointer le domaine vers le serveur

Dans l'espace client OVHcloud : **Web Cloud → Noms de domaine → votre domaine → Zone DNS**.

1. Supprimez les enregistrements **A** et **AAAA** existants pour le domaine nu (champ « sous-domaine » vide)
   et pour `www`, s'il y en a (OVHcloud en crée vers une page d'attente).
2. **Ajouter une entrée → A** :
   - sous-domaine : *vide* (pour `cic-credit.com`) ;
   - cible : **l'adresse IP du VPS** ;
   - TTL : par défaut.
3. Attendez la propagation (de quelques minutes à quelques heures). Vérification depuis PowerShell :
   ```powershell
   nslookup cic-credit.com
   ```
   La réponse doit afficher l'IP du VPS.

> Vous préférez un sous-domaine (`demo.cic-credit.com`) ? Mettez `demo` dans le champ « sous-domaine » et
> utilisez `demo.cic-credit.com` partout où ce guide écrit « votre domaine ».

**Ne passez pas à l'étape 5 tant que `nslookup` ne renvoie pas la bonne IP** : sans cela, le certificat
HTTPS ne peut pas être obtenu.

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
ssh ubuntu@51.75.12.34
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

Reconnectez-vous (`ssh ubuntu@51.75.12.34`) pour que le groupe `docker` soit pris en compte, puis
vérifiez : `docker ps` doit répondre sans erreur.

### 4.2 Autoriser le serveur à lire le dépôt GitHub (clé de déploiement)

Le dépôt est privé : on donne au serveur une clé **en lecture seule**, limitée à ce dépôt.

```bash
ssh-keygen -t ed25519 -f ~/.ssh/github_cic -N ""
cat ~/.ssh/github_cic.pub
```

Sur GitHub : votre dépôt → **Settings → Deploy keys → Add deploy key**. Titre : `serveur OVH` ; collez la
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
| `CIC_DOMAINE` | votre domaine, par exemple `cic-credit.com` |
| `DB_PASSWORD` | mot de passe n° 1 |
| `CIC_BO_ADMIN_MOT_DE_PASSE` | mot de passe n° 2 |
| `CIC_BO_ANALYSTE_MOT_DE_PASSE` | mot de passe n° 3 |
| `CIC_BO_RECOUVREMENT_MOT_DE_PASSE` | mot de passe n° 4 |
| `CIC_BO_ASSOCIE_MOT_DE_PASSE` | mot de passe n° 5 |

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

Attendez `Started CicApplication` côté `app`, et `certificate obtained successfully` côté `caddy`, puis
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

### Sauvegardes

Une sauvegarde de la base est faite **chaque nuit à 02:00 UTC** et gardée 14 jours, **sur le serveur**.
Avant une présentation importante, faites-en une à la main et rapatriez-la sur votre PC :

```bash
docker compose exec sauvegarde bash /usr/local/bin/sauvegarde-base.sh
docker compose cp sauvegarde:/sauvegardes ./sauvegardes
```

```powershell
# sur votre PC
scp -r ubuntu@51.75.12.34:cic/sauvegardes .
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
- **Après le challenge**, arrêtez le site (`docker compose down`) ou résiliez le VPS dans l'espace client
  OVHcloud pour ne plus être facturé. Le domaine, lui, se renouvelle chaque année si le renouvellement
  automatique est actif.
- Pour une exploitation réelle (profil `prod`), voir la section « Profil `prod` » du README : comptes du
  back-office à créer en base, sauvegardes à copier hors du serveur.

---

*Sources des prix : [OVHcloud VPS](https://www.ovhcloud.com/fr/vps/),
[blog OVHcloud — gamme VPS 2027](https://blog.ovhcloud.com/en/posts/vps-2027/),
[OVHcloud noms de domaine](https://www.ovhcloud.com/fr/domains/). Hetzner a été écarté : ses offres
cloud étaient indiquées « indisponibles » au moment de la rédaction
([page Hetzner](https://www.hetzner.com/cloud/regular-performance/)).*
