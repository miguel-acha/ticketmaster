package edu.upb.tickmaster.httpserver;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad para validar la integridad de los mensajes usando HMAC-SHA256.
 */
public class IntegrityUtil {
    private static final Logger logger = LoggerFactory.getLogger(IntegrityUtil.class);
    private static final String SECRET_KEY = "miguel123";
    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Calcula el HMAC-SHA256 de un cuerpo de mensaje.
     */
    public static String calcularHMAC(byte[] data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            logger.error("Error calculando HMAC: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si una firma recibida coincide con el contenido del mensaje.
     */
    public static boolean verificarFirma(byte[] cuerpo, String firmaRecibida) {
        if (firmaRecibida == null || cuerpo == null) {
            return false;
        }
        String firmaCalculada = calcularHMAC(cuerpo);
        boolean esValida = firmaRecibida.equals(firmaCalculada);

        if (!esValida) {
            logger.warn("¡Falla de Integridad! Firma recibida: {}, Firma calculada: {}", firmaRecibida, firmaCalculada);
        }

        return esValida;
    }
}
