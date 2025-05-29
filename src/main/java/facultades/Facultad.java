package facultades;

import com.google.gson.Gson;
import modelo.Solicitud;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.HashMap;
import java.util.Map;

public class Facultad {

    private static final int PUERTO_SERVIDOR = 5555;
    private static final int PUERTO_RECEPCION = 6000;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Facultad <nombreFacultad> <ipServidor>");
            return;
        }

        String nombreFacultad = args[0];
        String ipServidor = args[1];

        try (ZContext context = new ZContext()) {
            ZMQ.Socket socketServidor = context.createSocket(ZMQ.REQ);
            socketServidor.connect("tcp://" + ipServidor + ":" + PUERTO_SERVIDOR);

            ZMQ.Socket socketRecepcion = context.createSocket(ZMQ.REP);
            socketRecepcion.bind("tcp://*:" + PUERTO_RECEPCION);

            Gson gson = new Gson();

            // Enviar inscripción al servidor
            Map<String, String> mensajeInscripcion = new HashMap<>();
            mensajeInscripcion.put("tipo", "inscripcion");
            mensajeInscripcion.put("facultad", nombreFacultad);

            socketServidor.send(gson.toJson(mensajeInscripcion));
            String respuesta = socketServidor.recvStr();
            System.out.println("✅ Respuesta del servidor: " + respuesta);

            // Esperar solicitudes de programas académicos
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("📥 Esperando solicitud del programa académico en puerto " + PUERTO_RECEPCION + "...");
                String solicitudJson = socketRecepcion.recvStr();
                System.out.println("📨 Solicitud recibida: " + solicitudJson);

                // Reenviar al servidor
                socketServidor.send(solicitudJson);
                String respuestaServidor = socketServidor.recvStr();

                System.out.println("📤 Respuesta del servidor: " + respuestaServidor);
                socketRecepcion.send(respuestaServidor);
            }

            // Cierre
            socketRecepcion.close();
            socketServidor.close();
        }
    }
}
