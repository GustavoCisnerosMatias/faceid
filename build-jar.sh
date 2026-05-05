#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════════
# Script: Compilar JAR de Sara Service
# Uso: ./build-jar.sh
# ════════════════════════════════════════════════════════════════════════════════

set -e

echo "═══════════════════════════════════════════════════════════════"
echo "🔨 Compilación de Sara Service JAR"
echo "═══════════════════════════════════════════════════════════════"

# Verificar Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven no está instalado. Instalar desde https://maven.apache.org"
    exit 1
fi

# Verificar Java 17
JAVA_VERSION=$(java -version 2>&1 | grep -oP '(?<=")\d+' | head -1)
if [ "$JAVA_VERSION" != "17" ]; then
    echo "⚠️  Se recomienda Java 17 (detectado: $JAVA_VERSION)"
fi

# Directorio actual
echo "📍 Directorio actual: $(pwd)"
echo ""

# Limpiar build anterior
echo "🧹 Limpiando build anterior..."
mvn clean -q

# Descargar dependencias
echo "📥 Descargando dependencias..."
mvn dependency:resolve -q

# Compilar
echo "⚙️  Compilando código..."
mvn compile -q

# Ejecutar tests (opcional)
# mvn test

# Empaquetar
echo "📦 Empaquetando JAR..."
mvn package -DskipTests -q

# Verificar resultado
if [ -f "target/sara-service-*.jar" ]; then
    JAR_FILE=$(ls -1 target/sara-service-*.jar)
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "✅ Compilación exitosa"
    echo "═══════════════════════════════════════════════════════════════"
    echo "📄 JAR: $JAR_FILE"
    echo "📊 Tamaño: $JAR_SIZE"
    echo "📅 Fecha: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    # Mostrar información del JAR
    echo "ℹ️  Información del JAR:"
    ls -lh "$JAR_FILE"
    echo ""
    
    echo "🚀 Pasos siguientes:"
    echo "   1. Construir imagen Docker: docker build -t mies/sara-service:1.0.0 ."
    echo "   2. Ejecutar contenedor: docker run -d -p 8083:8083 mies/sara-service:1.0.0"
    echo "   3. Verificar: curl http://localhost:8083/actuator/health"
    echo ""
else
    echo "❌ Error: JAR no encontrado en target/"
    exit 1
fi
