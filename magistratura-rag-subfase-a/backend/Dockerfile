# Step 1: Build Java Application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Step 2: Run Application — optimizado para Render Free (512 MB RAM)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tesseract nativo + dados de português (necessário para tess4j / OCR de PDFs escaneados)
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-por leptonica

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV JAVA_TOOL_OPTIONS=""

# Heap limitado ao free tier do Render (512 MB total do container).
# SerialGC: melhor para heaps < ~400 MB e poucos cores (menos overhead que G1).
# ExitOnOutOfMemoryError: o processo sai de forma limpa para o Render reiniciar,
# em vez de ficar a servir respostas corrompidas após OOM.
# MaxMetaspaceSize baixo: evita metaspace crescer sem controlo.
# AlwaysPreTouch desligado (default) para arranque mais leve.
ENTRYPOINT ["sh", "-c", "exec java \
  -Xms64m \
  -Xmx320m \
  -XX:MaxMetaspaceSize=96m \
  -XX:ReservedCodeCacheSize=48m \
  -XX:+UseSerialGC \
  -XX:MaxHeapFreeRatio=40 \
  -XX:MinHeapFreeRatio=20 \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dserver.port=${PORT} \
  -jar app.jar"]
