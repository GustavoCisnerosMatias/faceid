@echo off
REM ════════════════════════════════════════════════════════════════════════════════
REM Script: Compilar JAR de Sara Service (Windows)
REM Uso: build-jar.bat
REM ════════════════════════════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

echo ═══════════════════════════════════════════════════════════════
echo 🔨 Compilación de Sara Service JAR
echo ═══════════════════════════════════════════════════════════════

REM Verificar Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Maven no está instalado. Instalar desde https://maven.apache.org
    exit /b 1
)

REM Verificar Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Java no está instalado. Instalar JDK 17 desde https://adoptium.net
    exit /b 1
)

REM Directorio actual
echo 📍 Directorio actual: %cd%
echo.

REM Limpiar build anterior
echo 🧹 Limpiando build anterior...
call mvn clean -q

if %errorlevel% neq 0 (
    echo ❌ Error en mvn clean
    exit /b 1
)

REM Descargar dependencias
echo 📥 Descargando dependencias...
call mvn dependency:resolve -q

REM Compilar
echo ⚙️  Compilando código...
call mvn compile -q

if %errorlevel% neq 0 (
    echo ❌ Error en compilación
    exit /b 1
)

REM Empaquetar
echo 📦 Empaquetando JAR...
call mvn package -DskipTests -q

if %errorlevel% neq 0 (
    echo ❌ Error en packaging
    exit /b 1
)

REM Verificar resultado
for /f %%A in ('dir /b target\sara-service-*.jar 2^>nul') do (
    set "JAR_FILE=%%A"
)

if defined JAR_FILE (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo ✅ Compilación exitosa
    echo ═══════════════════════════════════════════════════════════════
    echo 📄 JAR: target\%JAR_FILE%
    echo.
    
    echo 📂 Contenido:
    dir /h "target\%JAR_FILE%"
    echo.
    
    echo 🚀 Pasos siguientes:
    echo    1. Construir imagen Docker: docker build -t mies/sara-service:1.0.0 .
    echo    2. Ejecutar contenedor: docker run -d -p 8083:8083 mies/sara-service:1.0.0
    echo    3. Verificar: curl http://localhost:8083/actuator/health
    echo.
) else (
    echo ❌ Error: JAR no encontrado en target\
    exit /b 1
)

endlocal
