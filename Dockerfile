# ---- Stage 1: Build the WAR with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Run on Tomcat 9 ----
FROM tomcat:9.0-jdk17-temurin-jammy

# Remove default Tomcat apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the WAR as ROOT.war so the app is served at /
COPY --from=build /app/target/book-exchange.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
