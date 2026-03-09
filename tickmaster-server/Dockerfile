# Etapa 1: Compilación con Maven
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app

# Copiar el POM principal y los módulos necesarios para resolver dependencias
COPY pom.xml .
COPY tickmaster-server/pom.xml tickmaster-server/
COPY tickmaster-load-balancer/pom.xml tickmaster-load-balancer/
COPY tickmaster-client/pom.xml tickmaster-client/

# Descargar dependencias (cache)
RUN mvn dependency:go-offline -B

# Copiar el código fuente del servidor
COPY tickmaster-server/src ./tickmaster-server/src

# Compilar solo el módulo del servidor
RUN mvn clean package -pl tickmaster-server -am -DskipTests

# Etapa 2: Imagen de ejecución ligera
FROM alpine/java:21-jre
LABEL authors="miguel"
WORKDIR /app

# Configurar Zona Horaria
ENV TZ=America/La_Paz

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /app/tickmaster-server/target/tickmaster-server-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar

# Exponer el puerto del servidor
EXPOSE 1914

# Comando para ejecutar la aplicación con configuración regional
ENTRYPOINT ["java", "-Duser.language=es", "-Duser.region=80", "-Duser.country=80", "-Duser.timezone=America/La_Paz", "-jar", "/app.jar"]

