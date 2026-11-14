# Marque Seheli (سهلي)

Les fichiers sont dans `src/main/resources/static/img/logo/`. Ce sont des SVG dessinés à la main : formes simples, aucune image bitmap, aucune police. Les lettres sont des tracés.

| Fichier | Usage |
|---|---|
| `seheli-symbole.svg` | Symbole seul : favicon, icône d'application, avatar, petits espaces |
| `seheli-logo.svg` | Logo horizontal en lettres latines, sur fond clair (version principale) |
| `seheli-logo-arabe.svg` | Logo horizontal en arabe, sur fond clair : symbole à droite, lecture de droite à gauche |
| `seheli-logo-monochrome.svg` | Logo latin d'une seule couleur (`#1d2522`) : fax, tampon, gravure, impression noir et blanc, fond clair peu contrasté |
| `seheli-logo-clair.svg` | Logo latin clair (blanc et or) sur un cartouche vert foncé `#08523b` déjà inclus dans le fichier |

## Sens du logo

« Seheli » (سهلي) évoque ce qui est facile. Le symbole est un anneau découpé en quatre parts :

- **une grande part dorée de 144°, soit 40 % du cercle** : l'apport payé à la commande ;
- **trois parts vertes égales de 72°** : le reste, remboursé en mensualités régulières.

L'idée : un prix découpé en parts claires et égales, connues dès la signature, sans surprise. Il n'y a ni croissant, ni pièce de monnaie, ni flèche. Les parts sont séparées par des espaces à bords parallèles, ce qui laisse le découpage lisible même en petit. La lecture part du haut et suit le sens des aiguilles d'une montre : d'abord l'apport, puis les mensualités.

Le mot-symbole latin `seheli` est écrit en minuscules, d'un trait de même épaisseur, avec des extrémités arrondies : un ton simple et accueillant. Le mot-symbole arabe `سهلي` suit le même dessin géométrique : trois dents pour le س, une boucle pour le ه, une haute hampe pour le ل, et un ي dont la queue revient sous le mot, avec ses deux points.

## Palette

Elle reprend les variables CSS de `static/css/app.css`. **Aucune couleur n'a été ajoutée ni modifiée** : la palette existante suffit. Le vert foncé déjà présent sert de fond à la version claire.

| Rôle | Variable CSS | Hex | Dans le logo |
|---|---|---|---|
| Vert Seheli (primaire) | `--couleur-primaire` | `#0b6e4f` | Parts « mensualités » |
| Vert foncé | `--couleur-primaire-fonce` | `#08523b` | Mot-symbole ; fond de la version claire |
| Or (accent) | `--couleur-accent` | `#f2b705` | Part « apport » |
| Texte | `--couleur-texte` | `#1d2522` | Version monochrome |
| Blanc | `--couleur-fond` | `#ffffff` | Fond ; parts et texte de la version claire |
| Fond doux | `--couleur-fond-doux` | `#f3f7f5` | Fond clair autorisé |

### Contrastes (WCAG 2.x)

| Premier plan / fond | Rapport | Lecture |
|---|---|---|
| `#08523b` sur `#ffffff` | 9,21 : 1 | AAA texte |
| `#08523b` sur `#f3f7f5` | 8,52 : 1 | AAA texte |
| `#0b6e4f` sur `#ffffff` | 6,25 : 1 | AA texte, AAA grand texte |
| `#0b6e4f` sur `#f3f7f5` | 5,79 : 1 | AA texte |
| `#1d2522` sur `#ffffff` | 15,67 : 1 | AAA |
| `#ffffff` sur `#08523b` | 9,21 : 1 | AAA (version claire) |
| `#f2b705` sur `#08523b` | 5,07 : 1 | AA texte (version claire) |
| `#f2b705` sur `#0b6e4f` | 3,44 : 1 | AA grand texte et éléments graphiques seulement |
| `#1d2522` sur `#f2b705` | 8,62 : 1 | AAA (texte sombre sur un bouton or) |
| `#f2b705` sur `#ffffff` | 1,82 : 1 | **Insuffisant** |

À propos de l'or sur fond blanc (1,82 : 1) : les logotypes sont exemptés des critères de contraste (WCAG 1.4.3). Sur fond clair, la forme du symbole est portée par les trois parts vertes (6,25 : 1) et par les espaces qui les séparent. La part dorée y joue un rôle d'accent, pas d'information. En revanche, l'or ne doit jamais servir de couleur de texte ou d'icône seule sur un fond clair. Si le contraste compte vraiment, utiliser la version monochrome.

## Typographies

- **Logo** : lettres dessinées, sans police. Le trait du mot-symbole mesure 6 unités pour un symbole de 64 unités, soit environ 9 % de la hauteur du symbole.
- **Interface, en français** : la pile système de `--police` (`system-ui`, `Segoe UI`, `Roboto`, `Helvetica Neue`, `Arial`). Les titres sont en gras (700).
- **Interface, en arabe** : `--police-arabe` (`Noto Naskh Arabic`, `Noto Sans Arabic`, `Segoe UI`, `Tahoma`, `Geeza Pro`), avec `dir="rtl"`.
- Ne pas recomposer le logo avec une police : toujours utiliser les fichiers SVG.

## Tailles minimales

| Élément | À l'écran | À l'impression |
|---|---|---|
| Symbole seul | 16 px (favicon) ; 24 px conseillés | 6 mm |
| Logo latin | 24 px de haut | 8 mm de haut |
| Logo arabe | 28 px de haut (à cause de la hampe du ل et des points du ي) | 10 mm de haut |
| Version claire (avec cartouche) | 36 px de haut | 12 mm de haut |

Le symbole a été vérifié en rendu à 16 px : l'anneau mesure alors 3 px d'épaisseur et les espaces 1,25 px, ce qui garde les quatre parts visibles. En dessous de 16 px, ne rien afficher plutôt qu'un symbole flou.

## Zone de protection

Unité **P = un quart de la hauteur du symbole**, soit 16 unités pour un symbole de 64. Par exemple, P vaut 8 px quand le symbole mesure 32 px.

- Laisser au moins **P** de vide autour du symbole et du logo, sur les quatre côtés. Aucun texte, bord de page ou autre logo ne doit y entrer.
- La version claire contient déjà cette marge : son cartouche vert dépasse de P de chaque côté.
- Entre le symbole et le mot : environ 20 unités pour 64 (fixé dans les fichiers, ne pas modifier).

## Usages interdits

- Déformer le logo (étirement, compression), le pencher, le faire pivoter, ou le mettre en miroir. Le logo arabe a son propre fichier : ne pas retourner le logo latin.
- Changer les couleurs, ajouter un dégradé, une ombre, un contour ou un effet 3D.
- Changer la proportion des parts : la part dorée représente toujours 40 %, suivie de trois parts égales. Ne pas ajouter ni retirer de parts. Ne pas transformer le symbole en compteur ou en graphique animé qui laisserait croire à un autre découpage.
- Placer le logo en couleurs sur un fond moyen, chargé ou photographique. Choisir plutôt la version claire sur un fond foncé ou la version monochrome sur un fond clair.
- Mettre la version claire sur un fond clair, ou la version en couleurs sur un fond foncé.
- Recomposer le mot `seheli` ou `سهلي` avec une police, ou changer la casse (`Seheli`, `SEHELI`) dans le logo. Dans un texte courant, la marque s'écrit **Seheli**.
- Ajouter un croissant, une pièce de monnaie, un slogan collé au logo, ou associer le symbole à un autre logo sans respecter la zone de protection.
- Descendre sous les tailles minimales.
- Utiliser l'ancien nom CIC avec ce logo.
