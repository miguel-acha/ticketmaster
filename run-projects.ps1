# Script para automatizar el build y ejecución en Docker

Write-Host "--- 1. Limpiando y Empaquetando con Maven ---" -ForegroundColor Cyan
# Ejecutamos maven desde la raíz para construir todos los módulos
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error en la compilación de Maven. Abortando." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "--- 2. Limpiando Contenedores e Imágenes Anteriores ---" -ForegroundColor Cyan
docker-compose down --rmi all

Write-Host "--- 3. Construyendo e Iniciando Contenedores ---" -ForegroundColor Cyan
# docker-compose up --build construye las imágenes y levanta 2 servidores
docker-compose up --build -d --scale server=2

Write-Host "--- ¡Todo listo! ---" -ForegroundColor Green
Write-Host "Balanceador en: http://localhost:1915"
Write-Host "Servidor en: http://localhost:1914"
Write-Host "Usa 'docker-compose logs -f' para ver los logs en tiempo real."