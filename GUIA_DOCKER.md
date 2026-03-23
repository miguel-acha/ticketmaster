# Guía: Ejecución Manual y Docker Desktop

Esta guía explica cómo realizar manualmente los pasos que automatiza el archivo `run-projects.ps1` usando la consola y Docker.

---

## 1. El Flujo de Ejecución (Paso a Paso)

Si quieres correr todo el proyecto manualmente desde la consola, este es el orden "clásico":

### A. Limpiar y Construir (Maven)
Desde la carpeta raíz del proyecto, ejecuta esto para generar los archivos `.jar`:
```powershell
mvn clean package -DskipTests
```

### B. Levantar Infraestructura (Docker)
Usa Docker Compose para iniciar la Base de Datos, el Balanceador y los Servidores (esto emula la parte de escalado del script):
```powershell
# Levanta todo y crea 2 instancias del servidor para el balanceo
docker-compose up --build -d --scale server=2
```

### C. Ejecutar el Cliente (Interactiva)
Como el cliente requiere que escribas en la consola, se corre aparte:
```powershell
mvn exec:java -pl tickmaster-client
```

---

## 2. Traducción Manual de `run-projects.ps1`

Si quieres hacer exactamente lo que hace el script pero comando por comando en la consola, estos son los equivalentes:

1. **Paso 1: Limpiar y Empaquetar** (Equivale a la línea 5 del script)
   ```powershell
   mvn clean package -DskipTests
   ```
2. **Paso 2: Limpiar Docker** (Equivale a la línea 13 del script - borra imágenes y contenedores previos)
   ```powershell
   docker-compose down --rmi all
   ```
3. **Paso 3: Levantar todo con escala** (Equivale a la línea 17 del script)
   ```powershell
   docker-compose up --build -d --scale server=2
   ```
4. **Paso 4: Ver qué pasó (Logs)** (Equivale a la línea 22 del script)
   ```powershell
   docker-compose logs -f
   ```

---

## 3. Comandos Manuales de Docker Desktop

- **Construir solo un módulo:** (Ej: entras a `tickmaster-server`)
  ```powershell
  docker build -t mi-servidor .
  ```
- **Ver qué está corriendo:**
  ```powershell
  docker ps
  ```
- **Detener y borrar todo lo de Compose:**
  ```powershell
  docker-compose down
  ```
- **Ver logs (mensajes de error/consola) del servidor en Docker:**
  ```powershell
  docker-compose logs -f server
  ```

---

## 3. Limpieza Rápida
- **Borrar basura de Docker (imágenes huérfanas):** `docker image prune`
- **Borrar contenedores viejos:** `docker container prune`
