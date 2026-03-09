# Guía: Comandos Esenciales de Git y Docker

Esta guía contiene los comandos básicos para subir tus cambios a Git y desplegar tu aplicación en Docker Desktop.

---

## 1. Git: Subir Cambios al Repositorio

Si ya has realizado cambios en el código, sigue este flujo para subirlos:

1. **Verificar cambios:** Mira qué archivos has tocado.
   ```powershell
   git status
   ```
2. **Preparar archivos (Stage):** Añade los archivos que quieres subir.
   ```powershell
   git add .
   ```
   *(El punto `.` añade todos los archivos modificados).*
3. **Confirmar cambios (Commit):** Crea una "foto" de tus cambios con un mensaje descriptivo.
   ```powershell
   git commit -m "Mensaje explicando qué cambiaste"
   ```
4. **Subir al servidor (Push):** Envía tus cambios a la rama en la que estás trabajando (ej: `grpc`).
   ```powershell
   git push origin grpc
   ```

---

## 2. Docker: Desplegar en Docker Desktop

Para usar Docker por primera vez en el proyecto, necesitas tener un archivo llamado `Dockerfile` en cada módulo (server, client, load-balancer).

### Pasos principales para Docker Desktop:

1. **Construir la imagen:** Esto empaqueta tu código en un "contenedor". Ejecútalo en la carpeta del proyecto (ej: `tickmaster-server`).
   ```powershell
   docker build -t tickmaster-server .
   ```
2. **Ver tus imágenes:** Comprueba que la imagen se creó correctamente.
   ```powershell
   docker images
   ```
3. **Correr el contenedor:** Esto inicia tu aplicación dentro de Docker.
   ```powershell
   docker run -p 1914:1914 -p 8081:8081 --name server-running tickmaster-server
   ```
   *Nota: `-p` mapea los puertos del contenedor a tu PC.*

---

## 3. Docker Compose (Recomendado)

Si tienes un archivo `docker-compose.yml` en la raíz, puedes levantar todo (Server, DB, Load Balancer) con un solo comando:

1. **Levantar todo:**
   ```powershell
   docker-compose up --build
   ```
2. **Detener todo:**
   ```powershell
   docker-compose down
   ```

---

## 4. Ejecución desde Consola (Manual)

Si no quieres usar el script `run-projects.ps1` y prefieres hacer todo manualmente por consola (recomendado para entender el flujo), sigue este orden:

### A. Limpiar y Compilar (Desde la Raíz)
Esto genera los archivos `.jar` necesarios.
```powershell
mvn clean package -DskipTests
```

### B. Levantar con Docker Compose (Recomendado)
Es lo que hace el script automáticamente para levantar base de datos, balanceador y servidores.
```powershell
# Levanta todo y escala a 2 servidores
docker-compose up --build -d --scale server=2
```

### C. Ejecutar el Cliente (En otra terminal)
El cliente no suele estar en Docker Compose porque es interactivo. Úsalo así:
```powershell
mvn exec:java -pl tickmaster-client
```

---

## Comandos de limpieza rápidos:
- **Borrar contenedores detenidos:** `docker container prune`
- **Borrar imágenes sin usar:** `docker image prune`
- **Ver logs de un contenedor:** `docker logs <nombre_contenedor>`
- **Ver logs en tiempo real (Compose):** `docker-compose logs -f`
