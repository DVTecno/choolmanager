FROM amazoncorretto:17-alpine-jdk

LABEL authors="Diego"

COPY target/No-Country-simulation-0.0.1-SNAPSHOT.jar schoolmanager.jar

ENTRYPOINT ["java", "-jar", "/schoolmanager.jar"]