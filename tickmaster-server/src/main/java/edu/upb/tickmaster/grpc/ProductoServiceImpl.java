package edu.upb.tickmaster.grpc;

import io.grpc.stub.StreamObserver;

import java.time.Instant;

public class ProductoServiceImpl extends edu.upb.tickmaster.grpc.ProductoServiceGrpc.ProductoServiceImplBase {
    @Override
    public void registrarProducto(edu.upb.tickmaster.grpc.ProductoRequest request,
            StreamObserver<edu.upb.tickmaster.grpc.ProductoResponse> responseObserver) {
        System.out.println(request.toString());

        ProductoResponse response = ProductoResponse.newBuilder()
                .setCodigoRespuesta("201_CREATED")
                .setMensaje("Producto " + request.getNombre() + " registrado con éxito.")
                .setTimestamp(Instant.now().toString())
                .build();

        // 4. Enviar la respuesta al cliente
        responseObserver.onNext(response);

        // 5. Cerrar el flujo (importante: si no llamas a esto, el cliente se queda
        // esperando)
        responseObserver.onCompleted();
    }
}