FROM alpine/java:21-jre
LABEL authors="miguel"
ARG JAR_FILE
#COPY config config
COPY ${JAR_FILE} app.jar
ENV TZ=America/La_Paz
EXPOSE 1914
ENTRYPOINT ["java", "-Duser.language=es","-Duser.region=80","-Duser.country=80", "-Duser.timezone=America/La_Paz", "-jar","/app.jar"]
