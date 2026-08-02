# Step 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Step 2: Run — Render Free 512 MB
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-por leptonica

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV TESSDATA_PREFIX=/usr/share/tessdata

# IMPORTANTE: no painel do Render, define JAVA_TOOL_OPTIONS exactamente assim
# (substitui o valor antigo de 220m). Não uses CompressedClassSpaceSize baixo.
#
#   JAVA_TOOL_OPTIONS=-Xms32m -Xmx150m -XX:MaxMetaspaceSize=300m -XX:MetaspaceSize=180m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dspring.jmx.enabled=false
#
# Orçamento ~512 MB:
#   Heap 150 | Metaspace 300 | native/OS ~60
ENTRYPOINT ["sh", "-c", "echo \"[jvm] PORT=$PORT JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:-<vazio>}\"; exec java \
  -Xms32m \
  -Xmx150m \
  -XX:MaxMetaspaceSize=300m \
  -XX:MetaspaceSize=180m \
  -XX:ReservedCodeCacheSize=32m \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.jmx.enabled=false \
  -Dserver.port=${PORT} \
  -jar app.jar"]
