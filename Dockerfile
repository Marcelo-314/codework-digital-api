FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/target/cwd-api-0.0.1-SNAPSHOT.jar /app/cwd-api.jar
COPY --chown=app:app deploy/render/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh

USER app:app
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "/app/cwd-api.jar"]
