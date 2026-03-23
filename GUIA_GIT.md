# Guía: Comandos Esenciales de Git

Esta guía básica te servirá para manejar las versiones de tu proyecto y cambiar entre ramas.

---

## 1. Cambiar de Rama (Branch) ⚠️ IMPORTANTE
Para moverte entre diferentes versiones del proyecto (ej: de `main` a `grpc`):

- **Ver en qué rama estás:**
  ```powershell
  git branch
  ```
- **Cambiar a una rama existente:**
  ```powershell
  git checkout nombre-de-la-rama
  ```
  *Ejemplo:* `git checkout grpc` o `git checkout main`.
- **Crear y cambiar a una rama nueva:**
  ```powershell
  git checkout -b nombre-nueva-rama
  ```

---

## 2. Flujo Típico: Subir Cambios

1. **Añadir cambios:**
   ```powershell
   git add .
   ```
2. **Confirmar (Commit):**
   ```powershell
   git commit -m "Descripción de lo que hiciste"
   ```
3. **Subir (Push):** Envía tus cambios a la rama en la que estás.
   ```powershell
   git push origin nombre-de-tu-rama
   ```

---

## 3. Otros Comandos de Consola
- **Ver cambios actuales:** `git status`
- **Bajar cambios del servidor:** `git pull origin nombre-rama`
