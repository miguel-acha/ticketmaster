package edu.upb.tickmaster.grpc;

import edu.upb.tickmaster.httpserver.JwtUtil;
import edu.upb.tickmaster.server.repositories.UsuarioRepository;
import io.grpc.stub.StreamObserver;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioServiceImpl.class);
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();

    @Override
    public void registrarUsuario(RegistrarUsuarioRequest request,
            StreamObserver<RegistrarUsuarioResponse> responseObserver) {
        try {
            int id = usuarioRepository.registrar(
                    request.getUsername(),
                    request.getNombre(),
                    request.getPassword(),
                    request.getRol().isEmpty() ? "cliente" : request.getRol());

            RegistrarUsuarioResponse response = RegistrarUsuarioResponse.newBuilder()
                    .setStatus("OK")
                    .setIdUsuario(id)
                    .setMessage("Usuario registrado exitosamente")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error gRPC al registrar usuario", e);
            responseObserver.onNext(RegistrarUsuarioResponse.newBuilder()
                    .setStatus("NOK")
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void loginUsuario(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();

            JsonObject usuario = usuarioRepository.findByUsername(username);

            if (usuario == null) {
                responseObserver.onNext(LoginResponse.newBuilder()
                        .setStatus("NOK")
                        .setMessage("Usuario no encontrado")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (!BCrypt.checkpw(password, usuario.get("password").getAsString())) {
                responseObserver.onNext(LoginResponse.newBuilder()
                        .setStatus("NOK")
                        .setMessage("Contraseña incorrecta")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            String token = JwtUtil.createToken(
                    usuario.get("id_usuario").getAsInt(),
                    usuario.get("username").getAsString(),
                    usuario.get("rol").getAsString());

            LoginResponse response = LoginResponse.newBuilder()
                    .setStatus("OK")
                    .setToken(token)
                    .setIdUsuario(usuario.get("id_usuario").getAsInt())
                    .setUsername(usuario.get("username").getAsString())
                    .setNombre(usuario.get("nombre").getAsString())
                    .setRol(usuario.get("rol").getAsString())
                    .setMessage("Login exitoso")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error gRPC en login", e);
            responseObserver.onNext(LoginResponse.newBuilder()
                    .setStatus("NOK")
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
