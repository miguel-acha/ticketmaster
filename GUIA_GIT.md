# Guía: Comandos Esenciales de Git

Esta guía contiene los comandos básicos para gestionar versiones y subir tus cambios al repositorio de Tickmaster.

---

## 1. Flujo Básico: Subir Cambios

Sigue estos pasos cada vez que quieras guardar tus avances en el servidor:

1. **Verificar estado:** Mira qué archivos has modificado.
   ```powershell
   git status
   ```
2. **Preparar archivos (Stage):** Añade los archivos que quieres incluir en el commit.
   ```powershell
   git add .
   ```
   *(El punto `.` añade todos los archivos modificados).*
3. **Confirmar cambios (Commit):** Crea un punto de control con un mensaje explicativo.
   ```powershell
   git commit -m "Mensaje explicando qué cambiaste"
   ```
4. **Subir al servidor (Push):** Envía tus cambios a la rama remota (ej: `grpc`).
   ```powershell
   git push origin grpc
   ```

---

## 2. Otros Comandos Útiles

- **Actualizar repositorio local:** Trae los cambios que otros hayan subido.
  ```powershell
  git pull origin grpc
  ```
- **Ver historial de cambios:**
  ```powershell
  git log --oneline -n 10
  ```
- **Deshacer cambios locales (CUIDADO):** Vuelve un archivo a su estado original.
  ```powershell
  git checkout -- nombre_del_archivo
  ```
