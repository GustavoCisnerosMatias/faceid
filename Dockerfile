FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="MDH - Ministerio de Desarrollo Humano"
LABEL description="Sara Service - Reconocimiento Facial FaceNet"
LABEL version="1.0.0"

WORKDIR /app

# Instalar dependencias nativas requeridas por OpenCV (glibc ya incluido en jammy)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN groupadd -g 1000 sara && useradd -u 1000 -g sara -M -s /bin/sh sara

# Copiar JAR desde la carpeta local target
COPY --chown=sara:sara target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher -version || exit 1

USER sara

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
ENV SERVER_PORT=8083
ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--server.port=${SERVER_PORT}"]
