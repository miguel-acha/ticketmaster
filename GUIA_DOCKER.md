# Guía: Comandos Esenciales de Docker

Esta guía explica cómo desplegar y gestionar los componentes de Tickmaster usando Docker Desktop y la consola.

---

## 1. Ejecución Rápida con Docker Compose

Si tienes el archivo `docker-compose.yml` en la raíz, este es el comando principal:

- **Levantar todo (Server, DB, LB):**
  ```powershell
  docker-compose up --build -d --scale server=2
  ```
- **Detener todo y limpiar:**
  ```powershell
  docker-compose down
  ```

---

## 2. Comandos Manuales (Paso a Paso)

Si prefieres ejecutar los comandos uno a uno para entender el proceso:

### A. Construir la Imagen (Build)
Ejecútalo dentro de la carpeta del módulo (ej: `tickmaster-server`).
```powershell
docker build -t tickmaster-server .
```

### B. Correr Contenedor Individual
```powershell
docker run -p 1914:1914 -p 8081:8081 --name server-running tickmaster-server
```

---

## 3. Ejecución Completa desde Consola (Flujo Manual)

Este es el orden recomendado si no usas scripts automatizados:

1. **Compilar el proyecto:**
   ```powershell
   mvn clean package -DskipTests
   ```
2. **Levantar infraestructura (Docker):**
   ```powershell
   docker-compose up --build -d --scale server=2
   ```
3. **Ejecutar el Cliente (Java):**
   *(En una terminal separada)*
   ```powershell
   mvn exec:java -pl tickmaster-client
   ```

---

## 4. Comandos de Gestión y Limpieza

- **Ver logs en tiempo real:** `docker-compose logs -f`
- **Ver contenedores activos:** `docker ps`
- **Borrar basura de Docker:**
  - Contenedores: `docker container prune`
  - Imágenes: `docker image prune`
