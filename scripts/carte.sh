#!/usr/bin/env bash
# Génère docs/CARTE.md à partir du code : bash scripts/carte.sh (depuis la racine du dépôt).
set -u
cd "${1:-.}"
B=src/main/java/com/cic
{
echo "# Carte du projet CIC"
echo
echo "Générée automatiquement depuis le code (\`$(git log -1 --format=%h)\`). Sert à se repérer vite ; le code fait foi."
echo
echo "## Modules (\`com.cic.*\`)"
echo
echo "| Module | Couches | Classes principales |"
echo "|---|---|---|"
for m in $(ls -d $B/*/ | xargs -n1 basename); do
  layers=$(cd $B/$m && find . -mindepth 1 -type d | sed 's|^\./||' | grep -E '^(domain|application|adapter/[^/]+/[^/]+|adapter/[^/]+)$' | tr '/' '.' | sort | paste -sd' ' -)
  classes=$(find $B/$m -name '*.java' ! -name package-info.java -printf '%f\n' | sed 's/\.java$//' | grep -E '(Service|Controller|Job|Repository|Port|Adapter)$|^[A-Z][a-z]+$' | sort | head -12 | paste -sd',' - | sed 's/,/, /g')
  n=$(find $B/$m -name '*.java' | wc -l)
  echo "| \`$m\` ($n) | ${layers:--} | ${classes:--} |"
done
echo
echo "## Routes web"
echo
echo "| Contrôleur (module) | Routes |"
echo "|---|---|"
java scripts/Routes.java $B
echo
echo "## Migrations Flyway"
echo
ls src/main/resources/db/migration | sort -V | sed 's/^/- /'
echo
echo "## Profils et configuration"
echo
for y in src/main/resources/application*.yml; do echo "- \`$(basename $y)\`"; done
echo "- Variables d'environnement utilisées : $(grep -rhoE '\$\{[A-Z_]+' src/main/resources/*.yml | sed 's/\${//' | sort -u | paste -sd' ' - | sed 's/ /, /g')"
echo
echo "## Jobs planifiés"
echo
grep -rlE '@Scheduled' $B | while read f; do echo "- \`$(basename $f .java)\` ($(echo $f | sed "s|$B/||; s|/.*||"))"; done
echo
echo "## Tests"
echo
echo "- $(find src/test -name '*Test.java' | wc -l) classes de test. Lancer une seule classe : \`./mvnw -q test -Dtest=NomDuTest\`."
} > docs/CARTE.md
