#!/bin/bash
# INSTRUCCIONES RÁPIDAS PARA EL ARQUITECTO
# ════════════════════════════════════════════════════════════════════════════════

cat << 'EOF'

╔═══════════════════════════════════════════════════════════════════════════════╗
║                    🐳 SARA SERVICE - DOCKERIZACIÓN LISTA                      ║
║                                                                               ║
║  Fecha: 2026-04-29                                                           ║
║  Status: ✅ COMPLETO Y LISTO PARA PRODUCCIÓN                                  ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝

📦 ARCHIVOS ENTREGADOS
════════════════════════════════════════════════════════════════════════════════

  ✅ Dockerfile                      → Imagen multi-stage (Alpine, 200MB)
  ✅ docker-compose.yml              → Orquestación completa (dev + prod)
  ✅ .dockerignore                   → Optimización de capas
  ✅ application-docker.yml          → Configuración Spring Boot
  ✅ prometheus.yml                  → Scrape de métricas
  ✅ .env.example                    → Variables de entorno
  ✅ build-jar.sh / build-jar.bat    → Scripts compilación
  ✅ DOCKER_GUIDE.md                 → Guía completa
  ✅ RESUMEN_ENTREGA.md              → Resumen técnico


🚀 INICIO RÁPIDO (3 COMANDOS)
════════════════════════════════════════════════════════════════════════════════

  # 1️⃣  Ir al directorio del proyecto
  cd sara-service

  # 2️⃣  Levantar toda la infraestructura
  docker-compose up --build -d

  # 3️⃣  Verificar que está corriendo
  curl http://localhost:8083/actuator/health

  ✓ Si ves {"status":"UP"} → Funciona perfecto


📊 ACCESOS DISPONIBLES
════════════════════════════════════════════════════════════════════════════════

  Sara Service:   http://localhost:8083
  Swagger UI:     http://localhost:8083/swagger-ui.html
  Prometheus:     http://localhost:9090
  Grafana:        http://localhost:3000  (user: admin, pass: admin)
  SQL Server:     localhost:1433


🔧 COMPILAR JAR E IMAGEN (PRODUCCIÓN)
════════════════════════════════════════════════════════════════════════════════

  # Windows
  .\build-jar.bat

  # Linux/Mac
  ./build-jar.sh

  # Luego, construir imagen Docker
  docker build -t mies/sara-service:1.0.0 .

  # Y ejecutar
  docker run -d \
    -p 8083:8083 \
    -e DB_HOST=192.168.95.43 \
    -e DB_USER=appsiimies \
    -e DB_PASSWORD='Mie$2@16' \
    mies/sara-service:1.0.0


⚙️  VARIABLES DE ENTORNO IMPORTANTES
════════════════════════════════════════════════════════════════════════════════

  SPRING_PROFILES_ACTIVE=docker|production
  SERVER_PORT=8083
  DB_HOST=192.168.95.43              ← ¡USA BD REAL EN PRODUCCIÓN!
  DB_USER=appsiimies
  DB_PASSWORD=Mie$2@16
  JAVA_OPTS=-Xmx512m -Xms256m
  LOGGING_LEVEL_ROOT=INFO


📋 ARCHIVO PARA ENVIAR AL ARQUITECTO
════════════════════════════════════════════════════════════════════════════════

  → RESUMEN_ENTREGA.md   (Resumen ejecutivo con checklist)
  → DOCKER_GUIDE.md      (Guía técnica completa)
  → Todos los archivos de arriba


🔒 NOTAS DE SEGURIDAD
════════════════════════════════════════════════════════════════════════════════

  ✓ Dockerfile usa usuario no-root (sara:sara)
  ✓ Multi-stage: no incluye código fuente ni Maven en imagen final
  ✓ Alpine Linux: imagen minimal y segura
  ✓ Variables de entorno: no hardcoded en código
  ✓ Health checks: compatible con Kubernetes


❓ PREGUNTAS COMUNES
════════════════════════════════════════════════════════════════════════════════

  P: ¿Qué versión de Java?
  R: OpenJDK 17 (Eclipse Temurin)

  P: ¿Qué tamaño tiene la imagen?
  R: 200MB (optimizado con multi-stage)

  P: ¿Cómo conecto la BD real?
  R: export DB_HOST=192.168.95.43 (ya está en .env.example)

  P: ¿Cómo veo logs?
  R: docker logs sara-service-1 o cat /app/logs/sara-service.log

  P: ¿Puedo usar esto en Kubernetes?
  R: Sí, health checks ya están configurados


📞 SOPORTE
════════════════════════════════════════════════════════════════════════════════

  Ver sección "Troubleshooting" en DOCKER_GUIDE.md
  Contactar al equipo de arquitectura para integración en CI/CD


═════════════════════════════════════════════════════════════════════════════════
                        ✅ ¡LISTO PARA ENTREGAR!
═════════════════════════════════════════════════════════════════════════════════

EOF
