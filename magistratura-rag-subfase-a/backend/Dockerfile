# Step 1: Build Java Application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Step 2: Run Application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Tesseract nativo + dados de português (necessário para tess4j / OCR de PDFs escaneados)
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-por leptonica

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV TESSDATA_PREFIX=/usr/share/tessdata

# -Xmx limitado ao free tier do Render (512MB) para evitar OOM-kill do container
ENTRYPOINT ["sh", "-c", "java -Xmx350m -XX:MaxMetaspaceSize=128m -Dserver.port=${PORT} -jar app.jar"]