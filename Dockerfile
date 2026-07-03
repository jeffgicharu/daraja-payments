# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Warm the dependency cache before copying sources so code changes don't
# invalidate the download layer
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app
USER app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
