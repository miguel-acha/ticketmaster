package edu.upb;

import edu.upb.tickmaster.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;
import java.util.UUID;

/**
 * Cliente gRPC para comprar tickets.
 * 
 * Se conecta al servidor gRPC en localhost:8081
 * y llama a los métodos ComprarTicket y ObtenerEventos.
 */
public class Main {

    public static void main(String[] args) {

        // 1. Crear conexión al servidor gRPC
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 8081)
                .usePlaintext()
                .build();

        // 2. Crear el "stub"
        TicketServiceGrpc.TicketServiceBlockingStub stub = TicketServiceGrpc.newBlockingStub(channel);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("========================================");
        System.out.println("   TickMaster - Cliente gRPC");
        System.out.println("========================================");

        while (running) {
            System.out.println("\n--- MENÚ ---");
            System.out.println("1. Ver eventos disponibles");
            System.out.println("2. Comprar ticket");
            System.out.println("3. Salir");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    verEventos(stub);
                    break;
                case "2":
                    comprarTicket(stub, scanner);
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        }

        // 3. Cerrar la conexión
        channel.shutdown();
        System.out.println("Conexion cerrada. Adios!");
    }

    /**
     * Llama al servidor para obtener la lista de eventos.
     */
    private static void verEventos(TicketServiceGrpc.TicketServiceBlockingStub stub) {
        System.out.println("\n--- Eventos Disponibles ---");
        try {
            // Llamada gRPC: enviamos un VacioRequest y recibimos ListaEventosResponse
            ListaEventosResponse response = stub.obtenerEventos(
                    VacioRequest.newBuilder().build());

            if (response.getEventosList().isEmpty()) {
                System.out.println("No hay eventos registrados.");
                return;
            }

            for (Evento evento : response.getEventosList()) {
                System.out.println("  ID: " + evento.getId()
                        + " | Nombre: " + evento.getName()
                        + " | Tickets: " + evento.getSoldTickets() + "/" + evento.getTotalTickets()
                        + " | Fecha: " + evento.getEventDate());
            }
        } catch (Exception e) {
            System.err.println("Error al obtener eventos: " + e.getMessage());
        }
    }

    /**
     * Pide datos al usuario y compra un ticket vía gRPC.
     */
    private static void comprarTicket(TicketServiceGrpc.TicketServiceBlockingStub stub, Scanner scanner) {
        System.out.println("\n--- Comprar Ticket ---");
        try {
            System.out.print("ID del evento: ");
            int eventId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("ID de usuario: ");
            int userId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("ID de tipo de ticket (VIP, General, etc.): ");
            int ticketTypeId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Numero de asiento (o Enter para 'General'): ");
            String seat = scanner.nextLine().trim();
            if (seat.isEmpty())
                seat = "General";

            // Generamos una llave de idempotencia única
            String idempotencyKey = UUID.randomUUID().toString();

            // Construimos el mensaje de solicitud
            ComprarTicketRequest request = ComprarTicketRequest.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setSeatNumber(seat)
                    .setIdempotencyKey(idempotencyKey)
                    .setTicketTypeId(ticketTypeId)
                    .build();

            // Llamada gRPC al servidor
            ComprarTicketResponse response = stub.comprarTicket(request);

            // Mostramos la respuesta
            System.out.println("\n  Status: " + response.getStatus());
            System.out.println("  Ticket ID: " + response.getTicketId());
            System.out.println("  Mensaje: " + response.getMessage());

        } catch (NumberFormatException e) {
            System.err.println("Los IDs deben ser números enteros.");
        } catch (Exception e) {
            System.err.println("Error al comprar ticket: " + e.getMessage());
        }
    }
}

// Para ejecutar: mvn exec:java -pl tickmaster-client