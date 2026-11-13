# syntax=docker/dockerfile:1
# Image de production de l'application CIC, en deux étapes :
#  1. build : compilation Maven avec le JDK 25 (les tests tournent en CI, pas pendant le build de l'image) ;
#  2. run   : JRE 25 seul, utilisateur non root, couches Spring Boot extraites pour un démarrage rapide.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
# Dépendances d'abord : cette couche reste en cache tant que le pom ne change pas.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Dépôt cloné sous Windows : fin de ligne CRLF ou bit exécutable perdu.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B package -DskipTests \
 && java -Djarmode=tools -jar target/cic-*.jar extract --layers --launcher --destination /build/extrait

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --home-dir /app cic \
 && mkdir -p /app /data/justificatifs /data/autorisations-signees \
 && chown -R cic:cic /data
WORKDIR /app
COPY --from=build /build/extrait/dependencies/ ./
COPY --from=build /build/extrait/spring-boot-loader/ ./
COPY --from=build /build/extrait/snapshot-dependencies/ ./
COPY --from=build /build/extrait/application/ ./
USER cic

# Justificatifs et autorisations signées : hors de l'image, dans un volume.
ENV CIC_DOCUMENTS_REPERTOIRE=/data/justificatifs \
    CIC_AUTORISATIONS_REPERTOIRE=/data/autorisations-signees \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"
VOLUME /data
EXPOSE 8080

# Pas de curl dans l'image JRE : simple requête HTTP via bash.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
