package edu.upb.tickmaster.httpserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

/**
 * Clase simple para manejar tokens JWT usando HMAC256.
 */
public class JwtUtil {
    private static final String SECRET_KEY = "miguel123";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    // Generar un token con el id_usuario y rol
    public static String createToken(int idUsuario, String username, String rol) {
        return JWT.create()
                .withSubject(username)
                .withClaim("id_usuario", idUsuario)
                .withClaim("rol", rol)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 7200000)) // 2 horas
                .sign(algorithm);
    }

    // Verificar el token y devolver la información
    public static DecodedJWT verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }
}
