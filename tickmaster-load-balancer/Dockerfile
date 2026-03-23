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

# Copiar el código fuente del balanceador
COPY tickmaster-load-balancer/src ./tickmaster-load-balancer/src

# Compilar solo el módulo del balanceador
RUN mvn clean package -pl tickmaster-load-balancer -am -DskipTests

# Etapa 2: Imagen de ejecución
FROM alpine/java:21-jre
LABEL authors="miguel"
WORKDIR /app

# Configurar Zona Horaria
ENV TZ=America/La_Paz

# Copiar el JAR generado
COPY --from=build /app/tickmaster-load-balancer/target/tickmaster-load-balancer-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar

# Exponer el puerto del balanceador
EXPOSE 1915

# Comando para ejecutar la aplicación con configuración regional
ENTRYPOINT ["java", "-Duser.language=es", "-Duser.region=80", "-Duser.country=80", "-Duser.timezone=America/La_Paz", "-jar", "/app.jar"]

