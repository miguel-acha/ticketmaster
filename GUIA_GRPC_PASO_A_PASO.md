# Guía Paso a Paso: Implementación de gRPC en Java (Maven)

Esta guía explica cómo implementar gRPC en un proyecto como Tickmaster, cubriendo desde la definición del contrato hasta la ejecución del cliente.

---

## Paso 1: Definir el "Contrato" (.proto)
gRPC se basa en un contrato. Debes definir tus servicios y mensajes en un archivo `.proto`.

- **Ubicación típica:** `src/main/proto/mi_servicio.proto`
- **Contenido:**
  ```proto
  syntax = "proto3";
  option java_package = "edu.upb.tickmaster.grpc";
  option java_multiple_files = true;

  service MiServicio {
    rpc Saludar (SaludoRequest) returns (SaludoResponse);
  }

  message SaludoRequest { string nombre = 1; }
  message SaludoResponse { string mensaje = 1; }
  ```

---

## Paso 2: Configurar Maven (pom.xml)
Necesitas plugins y dependencias para que Maven genere el código Java a partir del `.proto`.

1. **Dependencias:** `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`.
2. **Plugins:** `protobuf-maven-plugin` (para compilar protos) y `os-maven-plugin` (para detectar el SO, vital en Windows).

> [!IMPORTANT]
> En Windows, asegúrate de tener el `<protoSourceRoot>` bien configurado en el plugin para evitar errores de "directorio no encontrado".

---

## Paso 3: Generar el Código
Ejecuta el siguiente comando para generar las clases Java (Stubs):
```powershell
mvn clean compile
```
Esto creará archivos en `target/generated-sources/protobuf/`. No edites estos archivos manualmente.

---

## Paso 4: Implementar el Servicio (Servidor)
Crea una clase que extienda la base generada:

```java
public class MiServicioImpl extends MiServicioGrpc.MiServicioImplBase {
    @Override
    public void saludar(SaludoRequest request, StreamObserver<SaludoResponse> response) {
        String texto = "Hola " + request.getNombre();
        SaludoResponse res = SaludoResponse.newBuilder().setMensaje(texto).build();
        response.onNext(res);
        response.onCompleted();
    }
}
```

---

## Paso 5: Iniciar el Servidor gRPC
Debes registrar tu servicio en un servidor gRPC (usualmente puerto 8081 o similar):

```java
Server server = ServerBuilder.forPort(8081)
    .addService(new MiServicioImpl())
    .build();
server.start();
```

---

## Paso 6: Crear el Cliente
El cliente se conecta al canal y usa un "Stub" para llamar a los métodos como si fueran locales:

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8081)
    .usePlaintext().build();

MiServicioGrpc.MiServicioBlockingStub stub = MiServicioGrpc.newBlockingStub(channel);

SaludoResponse res = stub.saludar(SaludoRequest.newBuilder().setNombre("Miguel").build());
System.out.println(res.getMensaje());
```

---

## Resumen del Flujo de Trabajo
1. **Modificar `.proto`** (Añadir un nuevo método).
2. **`mvn compile`** (Generar nuevas interfaces Java).
3. **Actualizar `ServiceImpl`** (Escribir la lógica del nuevo método).
4. **Reiniciar Servidor**.
5. **Llamar desde el Cliente**.
