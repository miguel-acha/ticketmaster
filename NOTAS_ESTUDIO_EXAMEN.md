# 📝 NOTAS DE ESTUDIO FINAL - PROYECTO TICKMASTER

Este documento resume todos los puntos clave que hemos revisado para tu examen.

---

## 1. El Corazon del Servidor (`ApacheServer.java`)
Es donde se inicia el servidor y se registran las rutas.
*   **Puerto:** 1914.
*   **Hilos:** Usa un `FixedThreadPool(2)`, permitiendo 2 conexiones en paralelo.
*   **Registrar Ruta:** `server.createContext("/nueva-ruta", new MiHandler());`

---

## 2. Creacion y Modificacion de APIs
### Como crear un Handler
1. Crea una clase que implemente `HttpHandler`.
2. Sobreescribe el metodo `handle(HttpExchange exchange)`.

### Como modificar una respuesta (JSON)
Usamos la libreria **Gson** (`JsonObject`).
```java
JsonObject json = new JsonObject();
json.addProperty("status", "OK"); // Campo simple
json.add("detalles", otroObjeto); // Objeto anidado

// Flujo de envio:
String res = json.toString();
byte[] bytes = res.getBytes(StandardCharsets.UTF_8);
exchange.sendResponseHeaders(200, bytes.length);
exchange.getResponseBody().write(bytes);
exchange.getResponseBody().close();
```

---

## 3. Idempotencia (Evitar Duplicados)
**Definicion:** Realizar la misma operacion varias veces produce el mismo resultado.
*   **Uso:** Evitar que un reintento de red cobre dos veces un ticket.
*   **Implementacion:**
    1. El cliente envia un ID unico (`X-Idempotency-Key`) en el Header o en el Body JSON.
    2. El servidor hace un `SELECT` en la DB para ver si esa llave ya existe.
    3. Si **existe**, devuelve el resultado guardado sin procesar nada.
    4. Si **no existe**, procesa el pago e inserta la llave en una columna `UNIQUE`.

---

## 4. Load Balancer (Balanceador de Carga)
*   **Puerto:** 1915.
*   **Rol:** Proxy Inverso. Redirige el trafico al servidor real (puerto 1914).
*   **Beneficios:** Seguridad (oculta el backend) y Escalabilidad.
*   **CORS:** Se configura aqui para permitir que el frontend acceda a los recursos.

---

## 5. Preguntas Tipicas de Examen
1.  **¿Como parsear parametros de URL?**
    Usa `RootHandler.parseQuery(exchange.getRequestURI().getQuery(), Map)`.
2.  **¿Como manejar errores?**
    Usa un bloque `try-catch` y responde con `sendResponseHeaders(500, -1)` si algo falla.
3.  **¿Cual es el patron de DB?**
    **Singleton**: Solo una instancia de conexion para SQLite para evitar bloqueos del archivo.

---
**Tip Final:** Si te piden un cambio rapido, empieza por buscar el **Handler** correspondiente y revisa si es un cambio de JSON (Gson) o de Base de Datos (SQL en el repositorio).

/*
 * Guia para agregar una nueva funcon de API
 * Para agregar una nueva funcionalidad de API en este proyecto, sigue estos
 * tres pasos principales:
 * 
 * 1. Crear el Handler
 * Crea una nueva clase en el paquete edu.upb.tickmaster.httpserver que
 * implemente la interfaz HttpHandler.
 * 
 * Ejemplo (MiNuevaFuncionHandler.java):
 * 
 * java
 * package edu.upb.tickmaster.httpserver;
 * import com.sun.net.httpserver.HttpExchange;
 * import com.sun.net.httpserver.HttpHandler;
 * import java.io.IOException;
 * import java.io.OutputStream;
 * import java.nio.charset.StandardCharsets;
 * public class MiNuevaFuncionHandler implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * // 1. Configurar headers (opcional)
 * exchange.getResponseHeaders().add("Content-Type", "application/json");
 * 
 * // 2. Definir la respuesta
 * String response = "{\"mensaje\": \"Hola desde la nueva API\"}";
 * byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
 * 
 * // 3. Enviar headers de respuesta (Status 200 OK)
 * exchange.sendResponseHeaders(200, bytes.length);
 * 
 * // 4. Escribir el cuerpo y cerrar
 * try (OutputStream os = exchange.getResponseBody()) {
 * os.write(bytes);
 * }
 * }
 * }
 * 2. Registrar el Contexto
 * Abre el archivo
 * ApacheServer.java
 * y agrega la nueva ruta en el método
 * start()
 * .
 * 
 * Cambio en
 * ApacheServer.java
 * :
 * 
 * java
 * this.server.createContext("/mi-nueva-ruta", new MiNuevaFuncionHandler());
 * 3. Reiniciar el Servidor
 * Compila y ejecuta el proyecto nuevamente para que los cambios surtan efecto.
 * 
 * Tips adicionales:
 * Parsear query params: Puedes usar el método estático
 * RootHandler.parseQuery(exchange.getRequestURI().getQuery(), parameters) para
 * obtener los parámetros de la URL.
 * CORS: El servidor ya tiene configurado CORS básico en el contexto raíz /,
 * pero asegúrate de agregarlo en tu handler si el contexto es diferente y lo
 * necesitas.
 * 
 * 
 */