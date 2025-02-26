FROM openjdk:17-jdk-slim
WORKDIR /app
# Создаем папку для загруженных файлов
RUN mkdir -p /app/uploads
# Права доступа
RUN chmod -R 777 /app/uploads
COPY target/cloud-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
