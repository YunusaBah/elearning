# Build stage: produces the runnable JAR (no need to run mvn on the host first).
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Render's Docker builders are memory-limited; keep the Maven JVM small.
ENV MAVEN_OPTS="-Xmx384m -XX:+UseParallelGC"
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests -Dmaven.javadoc.skip=true package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV FILE_UPLOAD_DIR=/app/data/uploads
RUN mkdir -p /app/data/uploads

COPY --from=build /app/target/elearning-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
