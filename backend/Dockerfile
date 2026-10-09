# Etape 1 : build du jar Spring Boot (les tests tournent deja dans la CI)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Etape 2 : execution avec un simple JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/camera-foyer-backend-*.jar app.jar
# Dossier des videos a monter en volume, en LECTURE-ECRITURE (le backend supprime les fichiers) :
#   docker run -v /var/lib/motion/jours:/var/lib/motion/jours ...
ENV VIDEOS_DIR=/var/lib/motion/jours
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
