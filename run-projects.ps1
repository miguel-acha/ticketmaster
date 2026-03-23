# Script para automatizar el build y ejecución en Docker

Write-Host "--- 1. Limpiando y Empaquetando con Maven ---" -ForegroundColor Cyan
# Ejecutamos maven desde la raíz para construir todos los módulos
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error en la compilación de Maven. Abortando." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "--- 2. Deteniendo Contenedores Anteriores ---" -ForegroundColor Cyan
# Detener sin borrar imágenes externas (evita problemas de red al re-descargar nginx, etc.)
docker-compose down

Write-Host "--- 3. Construyendo e Iniciando Contenedores ---" -ForegroundColor Cyan
# Levantamos Balanceador, Pasarela, Nginx y 2 instancias del Servidor
docker-compose up --build -d --scale server=2

Write-Host "--- ¡Todo listo! ---" -ForegroundColor Green
Write-Host "Acceso General (Frontend + API): http://localhost:8080"
Write-Host "Balanceador de Carga (Java) en: http://localhost:1915"
Write-Host "Pasarela de Pago (Java) en:    http://localhost:1916"
Write-Host "Dashboard de Servidores en:   http://localhost:1914"
Write-Host "Usa 'docker-compose logs -f' para ver los logs en tiempo real."