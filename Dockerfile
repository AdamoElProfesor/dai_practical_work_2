FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/java-intellij-idea-and-maven-1.0-SNAPSHOT.jar app.jar

EXPOSE 1234

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["server", "-p", "1234"]