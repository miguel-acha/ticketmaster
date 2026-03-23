# Etapa 1: Compilación con Maven
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app

# Copiar el POM principal y los módulos necesarios
COPY pom.xml .
COPY tickmaster-server/pom.xml tickmaster-server/
COPY tickmaster-load-balancer/pom.xml tickmaster-load-balancer/
COPY tickmaster-client/pom.xml tickmaster-client/
COPY tickmaster-pasarela/pom.xml tickmaster-pasarela/

# Descargar dependencias
RUN mvn dependency:go-offline -B

# Copiar el código fuente de la pasarela
COPY tickmaster-pasarela/src ./tickmaster-pasarela/src

# Compilar solo el módulo de la pasarela
RUN mvn clean package -pl tickmaster-pasarela -am -DskipTests

# Etapa 2: Imagen de ejecución
FROM alpine/java:21-jre
LABEL authors="miguel"
WORKDIR /app

# Configurar Zona Horaria
ENV TZ=America/La_Paz

# Copiar el JAR generado con dependencias
COPY --from=build /app/tickmaster-pasarela/target/tickmaster-pasarela-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar

# Exponer el puerto de la pasarela
EXPOSE 1916

# Comando para ejecutar con manifest
ENTRYPOINT ["java", "-Duser.language=es", "-Duser.region=80", "-Duser.country=80", "-jar", "/app.jar"]
