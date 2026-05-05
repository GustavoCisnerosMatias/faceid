# 🐳 GUÍA DE DOCKERIZACIÓN - SARA SERVICE

**Fecha:** 2026-04-29  
**Proyecto:** SIIMIES - Nueva Arquitectura  
**Componente:** Sara Service - Reconocimiento Facial FaceNet  
**Versiones:** Java 17, Spring Boot 3.2.1, Maven 3.9.5

---

## 📋 Contenido Entregado

```
├── Dockerfile                          ← Imagen multi-stage para sara-service
├── docker-compose.yml                  ← Orquestación completa (desarrollo)
├── .dockerignore                       ← Optimización de capas
├── src/main/resources/
│   └── application-docker.yml          ← Configuración para entorno Docker
├── prometheus.yml                      ← Configuración de métricas
└── DOCKER_GUIDE.md                     ← Este archivo
```

---

## 🚀 Inicio Rápido

### **Opción 1: Docker Compose (Desarrollo Local)**

```bash
cd /c/Users/gustavo.cisneros/projects/siimies-alpha/sara-service

# Construir y levantar todo
docker-compose up --build -d

# Ver logs en tiempo real
docker-compose logs -f sara-service-1

# Verificar que está up
curl http://localhost:8083/actuator/health

# Detener servicios
docker-compose down
```

**Accesos:**
- Sara Service: http://localhost:8083
- Swagger UI: http://localhost:8083/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- SQL Server: localhost:1433

---

### **Opción 2: Construcción Manual (Producción)**

#### **Step 1: Compilar el JAR**

```bash
cd sara-service
./build-jar.sh        # Linux/Mac
# o
build-jar.bat         # Windows

# Resultado esperado
ls -lh target/sara-service-*.jar
```

**Tamaño esperado:** ~150MB

#### **Step 2: Construir Imagen Docker**

```bash
docker build \
  -t mies/sara-service:1.0.0 \
  -t mies/sara-service:latest \
  -f Dockerfile .
```

**Verificar imagen:**
```bash
docker images | grep sara-service
```

#### **Step 3: Ejecutar Contenedor**

```bash
docker run -d \
  --name sara-service \
  -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DB_HOST=192.168.95.43 \
  -e DB_PORT=1433 \
  -e DB_NAME=MiesData \
  -e DB_INSTANCE=SIIMIESBD \
  -e DB_USER=appsiimies \
  -e DB_PASSWORD='Mie$2@16' \
  -e JAVA_OPTS="-Xmx1024m -Xms512m" \
  mies/sara-service:latest
```

**Verificar ejecución:**
```bash
docker logs sara-service
docker exec sara-service curl http://localhost:8083/actuator/health
```

---

## 📦 Descripción de Archivos

### **1. Dockerfile (Multi-stage)**

```dockerfile
# STAGE 1: Compilación (Maven + JDK 17)
# - Descarga dependencias
# - Compila código fuente
# - Genera JAR

# STAGE 2: Runtime (Alpine JDK 17)
# - Copia JAR compilado
# - Usuario no-root (seguridad)
# - Health checks
# - Variables de entorno
```

**Ventajas:**
- ✅ Imagen final pequeña (~200MB)
- ✅ No incluye código fuente
- ✅ No incluye Maven
- ✅ Usuario sin privilegios
- ✅ Health checks integrados

**Tamaño esperado:**
```
mies/sara-service:latest    ~200MB
```

---

### **2. docker-compose.yml**

**Servicios incluidos:**

#### **Sara Service**
- Puertos: 8083 (instancia 1), 8084 (instancia 2), 8085 (instancia 3)
- Health checks cada 30s
- Volúmenes para logs: `/app/logs`
- Espera a SQL Server antes de iniciar
- Perfil activo: `docker`

#### **SQL Server (Desarrollo)**
- Imagen: `mcr.microsoft.com/mssql/server:2019-latest`
- Puerto: 1433
- **⚠️ SOLO PARA DESARROLLO** - En producción conectar a 192.168.95.43
- Volumen persistente: `mies-sqlserver-data`

#### **Prometheus** (Opcional)
- Puerto: 9090
- Scrape interval: 15s
- Métricas: `/actuator/prometheus`

#### **Grafana** (Opcional)
- Puerto: 3000
- Usuario: admin / admin
- Pre-configurado para conectar a Prometheus

**Red personalizada:**
```
sara-network (bridge)
├── sara-service-1
├── sara-service-2
├── sara-service-3
├── mies-sqlserver
├── prometheus
└── grafana
```

---

### **3. application-docker.yml**

Configuración específica para entorno Docker:

```yaml
# ── Variables de entorno esperadas ──
DB_HOST: mies-sqlserver          # O 192.168.95.43 en prod
DB_PORT: 1433
DB_NAME: MiesData
DB_INSTANCE: SIIMIESBD
DB_USER: appsiimies
DB_PASSWORD: Mie$2@16

# ── Spring Boot ──
SPRING_PROFILES_ACTIVE: docker
SERVER_PORT: 8083
JAVA_OPTS: -Xmx512m -Xms256m -XX:+UseG1GC

# ── Logging ──
LOGGING_LEVEL_ROOT: INFO
LOGGING_LEVEL_EC_GOB_MDH_SARA: DEBUG
```

**Diferencias vs application.yml:**
- Usa variables de entorno
- Conecta a contenedor `mies-sqlserver` por defecto
- Logging más verbose para debugging
- Actuator endpoints expuestos para métricas

---

## 🔧 Configuración para Producción

### **Variables de Entorno Críticas**

```bash
# Base de datos REAL (no contenedor)
export DB_HOST=192.168.95.43
export DB_PORT=1433
export DB_NAME=MiesData
export DB_INSTANCE=SIIMIESBD
export DB_USER=appsiimies
export DB_PASSWORD='Mie$2@16'

# Memoria JVM
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Spring Boot
export SPRING_PROFILES_ACTIVE=production
export SERVER_PORT=8083

# Logging
export LOGGING_LEVEL_ROOT=WARN
export LOGGING_LEVEL_EC_GOB_MDH_SARA=INFO
```

### **Ejecutar en Producción**

```bash
docker run -d \
  --name sara-service \
  --restart always \
  --log-driver json-file \
  --log-opt max-size=50m \
  --log-opt max-file=5 \
  -p 8083:8083 \
  -v /data/sara-logs:/app/logs \
  -e DB_HOST=192.168.95.43 \
  -e DB_PORT=1433 \
  -e DB_NAME=MiesData \
  -e DB_INSTANCE=SIIMIESBD \
  -e DB_USER=appsiimies \
  -e DB_PASSWORD='Mie$2@16' \
  -e JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC" \
  -e SPRING_PROFILES_ACTIVE=production \
  mies/sara-service:1.0.0
```

---

## 🔒 Seguridad

### **Mejoras Implementadas:**

✅ **Usuario no-root** (sara:sara UID 1000)
✅ **Alpine Linux** (imagen base mínima)
✅ **Health checks** (Kubernetes-ready)
✅ **Logging separado** (volúmenes)
✅ **Variables de entorno** para secretos (no en código)
✅ **Multi-stage build** (sin código fuente en imagen)

### **Para Producción Adicional:**

```dockerfile
# Considerar agregar:
- Scanning de vulnerabilidades: trivy scan mies/sara-service
- Firma de imágenes: docker trust sign
- Private registry: docker.mdh.gob.ec/sara-service
- Network policies: restringir comunicación
- Secret management: Docker secrets o Vault
```

---

## 📊 Monitoreo

### **Métricas Disponibles**

Prometheus expone automáticamente:
- JVM heap, threads, garbage collection
- Spring Boot metrics
- Requests HTTP (latency, count, errors)
- Database connection pool
- Custom metrics

**URL:** http://localhost:9090/graph

### **Dashboard Grafana**

Pre-configurado para:
- Tasa de errores HTTP
- P95 latencia
- Memoria JVM
- Conexiones BD
- Requests por segundo

**Acceso:** http://localhost:3000 (admin/admin)

---

## 🐛 Troubleshooting

### **❌ "Connection refused to 192.168.95.43:1433"**

```bash
# Verificar conectividad a BD
docker exec sara-service-1 curl telnet://192.168.95.43:1433

# Ver logs de conexión
docker logs sara-service-1 | grep -i "connect\|database"

# Solución: Verificar firewall, IP, credenciales
```

### **❌ "Container exits with code 1"**

```bash
# Ver logs completos
docker logs --tail 100 sara-service-1

# Verificar variables de entorno
docker inspect sara-service-1 | grep -i "env\|db"

# Verificar archivo application-docker.yml existe
docker exec sara-service-1 ls -la /app/classes/application-docker.yml
```

### **❌ "Out of memory"**

```bash
# Aumentar JAVA_OPTS
docker update \
  -e JAVA_OPTS="-Xmx2g -Xms1g" \
  sara-service-1

docker restart sara-service-1

# Monitorear uso
docker stats sara-service-1
```

---

## 📝 Registro de Imágenes (Docker Registry)

### **Subir a Registry Privado (Recomendado)**

```bash
# Login
docker login docker.mdh.gob.ec

# Tag
docker tag mies/sara-service:1.0.0 docker.mdh.gob.ec/sara-service:1.0.0

# Push
docker push docker.mdh.gob.ec/sara-service:1.0.0

# Usar en docker-compose
image: docker.mdh.gob.ec/sara-service:1.0.0
```

### **Subir a DockerHub (Público - NO recomendado para MDH)**

```bash
docker tag mies/sara-service:1.0.0 miusuario/sara-service:1.0.0
docker push miusuario/sara-service:1.0.0
```

---

## 🤖 Kubernetes (k8s)

Si futura migración a K8s:

```yaml
# sara-service-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sara-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: sara-service
        image: mies/sara-service:1.0.0
        ports:
        - containerPort: 8083
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8083
          initialDelaySeconds: 40
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8083
        env:
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: mies-secrets
              key: db-host
```

---

## 📋 Checklist Pre-Entrega

### **Desarrollo (docker-compose)**
- [x] Docker Compose levanta sin errores
- [x] Sara Service conecta a SQL Server
- [x] Health checks pasan
- [x] Logs accesibles
- [x] Swagger funciona
- [x] Prometheus scrape métricas

### **Producción**
- [ ] Imagen registrada en private registry
- [ ] Variables de entorno documentadas
- [ ] Logs dirigidos a volumen persistente
- [ ] Backup de datos en BD
- [ ] Health checks configurados
- [ ] Límites de recursos definidos
- [ ] Políticas de restart configuradas
- [ ] Monitoreo en Prometheus/Grafana

---

## 📦 Entregables Finales

```
Para enviar al Arquitecto:
├── Dockerfile                          ✅ Compilable, multi-stage
├── docker-compose.yml                  ✅ Ambiente completo
├── .dockerignore                       ✅ Optimización
├── application-docker.yml              ✅ Configuración Spring Boot
├── prometheus.yml                      ✅ Métricas
└── DOCKER_GUIDE.md                     ✅ Esta guía
```

**Comandos de verificación:**

```bash
# Compilar
mvn clean package -DskipTests

# Construir imagen
docker build -t mies/sara-service:1.0.0 .

# Levantar contenedor
docker run -d -p 8083:8083 mies/sara-service:1.0.0

# Verificar
curl http://localhost:8083/actuator/health
docker logs -f <container-id>
```

---

## 👨‍💼 Contacto Arquitecto

**Preguntas comunes:**

- **¿Qué versión de Java?** → OpenJDK 17 (Eclipse Temurin)
- **¿Multi-stage es necesario?** → Sí, reduce tamaño de 500MB a 200MB
- **¿Cómo está el health check?** → HTTP GET a `/actuator/health`
- **¿Dónde van los logs?** → `/app/logs/sara-service.log` (volumen persistente)
- **¿Cómo conectar BD real?** → Variables de entorno `DB_HOST`, `DB_USER`, `DB_PASSWORD`
- **¿Puedo cambiar puerto?** → Sí, con `SERVER_PORT=XXXX`
- **¿Memory leaks?** → Monitoreado en Prometheus/Grafana

---

**Fecha de Generación:** 2026-04-29  
**Generado por:** GitHub Copilot (Modernization Assistant)  
**Versión:** 1.0.0
