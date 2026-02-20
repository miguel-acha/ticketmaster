package edu.upb;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

public class IdempotencyTest {
    private static final String SERVER_URL = "http://localhost:1915/tickets";

    public static void main(String[] args) {
        String idempotencyKey = UUID.randomUUID().toString();
        String jsonInputString = "{\"event_id\": 1, \"user_name\": \"TestUser\", \"idempotency_key\": \""
                + idempotencyKey + "\"}";

        System.out.println("Enviando primera petición con key: " + idempotencyKey);
        String response1 = sendPost(jsonInputString);
        System.out.println("Respuesta 1: " + response1);

        System.out.println("\nEnviando segunda petición (reintento) con la MISMA key: " + idempotencyKey);
        String response2 = sendPost(jsonInputString);
        System.out.println("Respuesta 2: " + response2);

        if (response1.equals(response2)) {
            System.out.println("\n✅ ÉXITO: Ambas respuestas son iguales. La idempotencia funciona.");
        } else {
            System.out.println("\n❌ ERROR: Las respuestas son diferentes. La idempotencia falló.");
        }

        String newKey = UUID.randomUUID().toString();
        String jsonInputString2 = "{\"event_id\": 1, \"user_name\": \"TestUser\", \"idempotency_key\": \"" + newKey
                + "\"}";
        System.out.println("\nEnviando tercera petición con una NUEVA key: " + newKey);
        String response3 = sendPost(jsonInputString2);
        System.out.println("Respuesta 3: " + response3);

        if (!response3.equals(response1)) {
            System.out.println("\n✅ ÉXITO: La nueva petición generó un resultado diferente (nuevo ticket).");
        } else {
            System.out.println("\n❌ ERROR: La nueva petición devolvió el mismo resultado que la anterior.");
        }
    }

    private static String sendPost(String jsonInputString) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = con.getResponseCode();
            try (Scanner s = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.name())) {
                return s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
