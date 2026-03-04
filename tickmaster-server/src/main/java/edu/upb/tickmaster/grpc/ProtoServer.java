package edu.upb.tickmaster.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

public class ProtoServer {

    public ProtoServer() {
    }

    public void start() throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(8081)
                .addService(new ProductoServiceImpl())
                .addService(new TicketServiceImpl()) // Servicio de compra de tickets
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("Iniciando servidor gRPC en el puerto 8081...");
        server.start();

        // 3. Mantener vivo (bloquear el main para que no se cierre)
        System.out.println("Servidor escuchando...");
        server.awaitTermination();
    }
}