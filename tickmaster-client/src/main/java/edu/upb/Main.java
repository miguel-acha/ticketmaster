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

        // 2. Crear los "stubs"
        TicketServiceGrpc.TicketServiceBlockingStub ticketStub = TicketServiceGrpc.newBlockingStub(channel);
        UsuarioServiceGrpc.UsuarioServiceBlockingStub usuarioStub = UsuarioServiceGrpc.newBlockingStub(channel);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("========================================");
        System.out.println("   TickMaster - Cliente gRPC (Full)");
        System.out.println("========================================");

        while (running) {
            System.out.println("\n--- MENÚ PRINCIPAL ---");
            System.out.println("1. Registrar Usuario");
            System.out.println("2. Login de Usuario");
            System.out.println("3. Ver Eventos Disponibles");
            System.out.println("4. Crear Evento (Admin)");
            System.out.println("5. Crear Tipo de Ticket (Admin)");
            System.out.println("6. Comprar Ticket");
            System.out.println("7. Ver Mis/Sus Tickets");
            System.out.println("8. Salir");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    registrar(usuarioStub, scanner);
                    break;
                case "2":
                    login(usuarioStub, scanner);
                    break;
                case "3":
                    verEventos(ticketStub);
                    break;
                case "4":
                    crearEvento(ticketStub, scanner);
                    break;
                case "5":
                    crearTipoTicket(ticketStub, scanner);
                    break;
                case "6":
                    comprarTicket(ticketStub, scanner);
                    break;
                case "7":
                    verTickets(ticketStub, scanner);
                    break;
                case "8":
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

    private static void registrar(UsuarioServiceGrpc.UsuarioServiceBlockingStub stub, Scanner scanner) {
        System.out.println("\n--- Registrar Usuario ---");
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Nombre: ");
        String name = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();
        System.out.print("Rol (admin/cliente): ");
        String rol = scanner.nextLine();

        RegistrarUsuarioRequest req = RegistrarUsuarioRequest.newBuilder()
                .setUsername(user)
                .setNombre(name)
                .setPassword(pass)
                .setRol(rol)
                .build();

        RegistrarUsuarioResponse res = stub.registrarUsuario(req);
        System.out
                .println("Status: " + res.getStatus() + " | ID: " + res.getIdUsuario() + " | Msg: " + res.getMessage());
    }

    private static void login(UsuarioServiceGrpc.UsuarioServiceBlockingStub stub, Scanner scanner) {
        System.out.println("\n--- Login Usuario ---");
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        LoginRequest req = LoginRequest.newBuilder()
                .setUsername(user)
                .setPassword(pass)
                .build();

        LoginResponse res = stub.loginUsuario(req);
        System.out.println("Status: " + res.getStatus());
        if (res.getStatus().equals("OK")) {
            System.out.println("  Token: " + res.getToken());
            System.out.println("  Nombre: " + res.getNombre() + " (" + res.getRol() + ")");
        } else {
            System.out.println("  Error: " + res.getMessage());
        }
    }

    private static void crearEvento(TicketServiceGrpc.TicketServiceBlockingStub stub, Scanner scanner) {
        System.out.println("\n--- Crear Evento ---");
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Fecha (AAAA-MM-DD HH:MM:SS): ");
        String fecha = scanner.nextLine();
        System.out.print("Capacidad: ");
        int cap = Integer.parseInt(scanner.nextLine());

        CrearEventoRequest req = CrearEventoRequest.newBuilder()
                .setNombre(nombre)
                .setFecha(fecha)
                .setCapacidad(cap)
                .build();

        CrearEventoResponse res = stub.crearEvento(req);
        System.out
                .println("Status: " + res.getStatus() + " | ID: " + res.getIdEvento() + " | Msg: " + res.getMessage());
    }

    private static void crearTipoTicket(TicketServiceGrpc.TicketServiceBlockingStub stub, Scanner scanner) {
        System.out.println("\n--- Crear Tipo de Ticket ---");
        System.out.print("ID Evento: ");
        int idEv = Integer.parseInt(scanner.nextLine());
        System.out.print("Tipo (VIP/General): ");
        String tipo = scanner.nextLine();
        System.out.print("Cantidad: ");
        int cant = Integer.parseInt(scanner.nextLine());
        System.out.print("Precio: ");
        double precio = Double.parseDouble(scanner.nextLine());

        CrearTipoTicketRequest req = CrearTipoTicketRequest.newBuilder()
                .setIdEvento(idEv)
                .setTipoAsiento(tipo)
                .setCantidad(cant)
                .setPrecio(precio)
                .build();

        CrearTipoTicketResponse res = stub.crearTipoTicket(req);
        System.out.println(
                "Status: " + res.getStatus() + " | ID: " + res.getIdTipoTicket() + " | Msg: " + res.getMessage());
    }

    private static void verTickets(TicketServiceGrpc.TicketServiceBlockingStub stub, Scanner scanner) {
        System.out.print("ID de usuario: ");
        int userId = Integer.parseInt(scanner.nextLine());
        ListaTicketsResponse res = stub.listarTicketsUsuario(IdIntRequest.newBuilder().setId(userId).build());
        System.out.println("\n--- Tickets del Usuario " + userId + " ---");
        for (Ticket t : res.getTicketsList()) {
            System.out.println("  Ticket ID: " + t.getIdTicket() + " | Evento: " + t.getIdEvento() + " | Precio: "
                    + t.getPrecio());
        }
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

            System.out.print("ID de tipo de ticket: ");
            int ticketTypeId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Numero de asiento: ");
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