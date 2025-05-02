package facultades;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.zeromq.ZMQ;

import com.google.gson.Gson;

public class Facultad {

    private static final int PUERTO_SERVIDOR = 5555;
    private static final int REINTENTO_MS = 3000;

    public static void iniciar(String ipServidor, String nombreFacultad, ZMQ.Socket recepcion, ZMQ.Context context) {
        Gson gson = new Gson();

        ZMQ.Socket envio = context.socket(ZMQ.DEALER);
        envio.setIdentity(("FAC-" + UUID.randomUUID()).getBytes(ZMQ.CHARSET));
        envio.connect("tcp://" + ipServidor + ":" + PUERTO_SERVIDOR);

        // Inscripción al servidor
        Map<String, String> inscripcion = new HashMap<>();
        inscripcion.put("tipo", "inscripcion");
        inscripcion.put("facultad", nombreFacultad);

        envio.send("", ZMQ.SNDMORE);
        envio.send(gson.toJson(inscripcion));
        String respuestaInscripcion = envio.recvStr();

        System.out.println("[" + nombreFacultad + "] ✅ Inscripción: " + respuestaInscripcion);
        System.out.println("[" + nombreFacultad + "] 🟢 Esperando solicitudes del ProgramaAcadémico...");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("[" + nombreFacultad + "] ⏳ Esperando solicitud...");
                String solicitud = recepcion.recvStr();
                System.out.println("[" + nombreFacultad + "] 📥 Recibido de programa: " + solicitud);

                envio.send("", ZMQ.SNDMORE);
                envio.send(solicitud);

                String respuestaServidor = envio.recvStr();
                System.out.println("[" + nombreFacultad + "] 📤 Respuesta del servidor: " + respuestaServidor);

                recepcion.send(respuestaServidor); // Enviar de vuelta al Programa Académico
                System.out.println("[" + nombreFacultad + "] ✅ Respuesta enviada al programa.");
            } catch (Exception e) {
                System.err.println("[" + nombreFacultad + "] ❌ Error procesando solicitud: " + e.getMessage());
            }
        }

        envio.close();
    }
}
