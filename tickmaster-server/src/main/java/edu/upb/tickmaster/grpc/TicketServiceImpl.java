package edu.upb.tickmaster.grpc;

import edu.upb.tickmaster.server.repositories.CompraRepository;
import edu.upb.tickmaster.server.repositories.EventoRepository;
import edu.upb.tickmaster.server.repositories.TicketRepository;
import edu.upb.tickmaster.server.repositories.TipoTicketRepository;
import io.grpc.stub.StreamObserver;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * Implementación del servicio gRPC para compra de tickets.
 * Adaptado al nuevo esquema de BD con usuarios, tipo_ticket y compras.
 */
public class TicketServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {

    private final TicketRepository ticketRepository = new TicketRepository();
    private final TipoTicketRepository tipoTicketRepository = new TipoTicketRepository();
    private final CompraRepository compraRepository = new CompraRepository();
    private final EventoRepository eventoRepository = new EventoRepository();

    /**
     * Compra un ticket y lo guarda en la base de datos.
     * Request: { eventId, userId, seatNumber, ticketTypeId, idempotencyKey }
     */
    @Override
    public void comprarTicket(ComprarTicketRequest request,
            StreamObserver<ComprarTicketResponse> responseObserver) {

        System.out.println("=== Solicitud de compra de ticket recibida ===");
        System.out.println("  Event ID:       " + request.getEventId());
        System.out.println("  Usuario ID:     " + request.getUserId());
        System.out.println("  Asiento:        " + request.getSeatNumber());
        System.out.println("  Tipo ticket ID: " + request.getTicketTypeId());

        try {
            // 1. Verificar idempotencia
            String idempotencyKey = request.getIdempotencyKey();
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                JsonObject existente = ticketRepository.findByIdempotencyKey(idempotencyKey);
                if (existente != null) {
                    System.out.println("  Idempotencia: key ya procesada, devolviendo ticket existente.");
                    ComprarTicketResponse response = ComprarTicketResponse.newBuilder()
                            .setStatus("OK")
                            .setTicketId(existente.get("ticket_id").getAsString())
                            .setMessage("Ticket ya comprado anteriormente")
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    return;
                }
            }

            // 2. Obtener precio del tipo de ticket
            JsonObject tipoTicket = tipoTicketRepository.findById(request.getTicketTypeId());
            if (tipoTicket == null) {
                responseObserver.onNext(ComprarTicketResponse.newBuilder()
                        .setStatus("ERROR")
                        .setTicketId("")
                        .setMessage("Tipo de ticket no encontrado: " + request.getTicketTypeId())
                        .build());
                responseObserver.onCompleted();
                return;
            }
            double precio = tipoTicket.get("precio").getAsDouble();

            // 3. Decrementar disponibilidad
            tipoTicketRepository.decrementarDisponibilidad(request.getTicketTypeId());

            // 4. Generar ID y guardar ticket
            String ticketId = UUID.randomUUID().toString();
            String nroAsiento = request.getSeatNumber().isEmpty() ? "General" : request.getSeatNumber();

            ticketRepository.saveTicket(
                    ticketId,
                    request.getEventId(),
                    request.getUserId(),
                    nroAsiento,
                    precio,
                    idempotencyKey,
                    request.getTicketTypeId());

            // 5. Registrar la compra
            int idCompra = compraRepository.registrarCompra(ticketId, request.getUserId(), precio);

            System.out.println("  Ticket comprado exitosamente: " + ticketId + " | Compra ID: " + idCompra);

            // 6. Construir y enviar respuesta
            ComprarTicketResponse response = ComprarTicketResponse.newBuilder()
                    .setStatus("OK")
                    .setTicketId(ticketId)
                    .setMessage("Ticket comprado exitosamente")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.err.println("  Error al comprar ticket: " + e.getMessage());
            ComprarTicketResponse response = ComprarTicketResponse.newBuilder()
                    .setStatus("ERROR")
                    .setTicketId("")
                    .setMessage("Error al comprar ticket: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Devuelve la lista de todos los eventos disponibles.
     */
    @Override
    public void obtenerEventos(VacioRequest request,
            StreamObserver<ListaEventosResponse> responseObserver) {

        System.out.println("=== Solicitud de lista de eventos recibida ===");

        try {
            JsonArray eventosJson = eventoRepository.listarEventos();
            ListaEventosResponse.Builder listaBuilder = ListaEventosResponse.newBuilder();

            for (int i = 0; i < eventosJson.size(); i++) {
                JsonObject e = eventosJson.get(i).getAsJsonObject();
                Evento evento = Evento.newBuilder()
                        .setId(e.get("id_evento").getAsInt())
                        .setName(e.get("nombre").getAsString())
                        .setTotalTickets(e.get("capacidad").getAsInt())
                        .setSoldTickets(0) // calculado desde tickets si se requiere
                        .setEventDate(e.has("fecha") && !e.get("fecha").isJsonNull()
                                ? e.get("fecha").getAsString()
                                : "Sin fecha")
                        .build();
                listaBuilder.addEventos(evento);
            }

            responseObserver.onNext(listaBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            System.err.println("Error al obtener eventos: " + e.getMessage());
            responseObserver.onNext(ListaEventosResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
