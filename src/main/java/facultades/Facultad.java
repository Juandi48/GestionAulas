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

        try {
            envio.connect("tcp://" + ipServidor + ":" + PUERTO_SERVIDOR);
            System.out.println("[" + nombreFacultad + "] 🔌 Conectado al servidor en " + ipServidor + ":" + PUERTO_SERVIDOR);
        } catch (Exception e) {
            System.err.println("[" + nombreFacultad + "] ❌ No se pudo conectar al servidor: " + e.getMessage());
            return;
        }

        // Envío de inscripción
        boolean inscrito = false;
        int intentos = 0;

        while (!inscrito && intentos < 5) {
            try {
                Map<String, String> inscripcion = new HashMap<>();
                inscripcion.put("tipo", "inscripcion");
                inscripcion.put("facultad", nombreFacultad);

                envio.send("", ZMQ.SNDMORE);
                envio.send(gson.toJson(inscripcion));
                System.out.println("[" + nombreFacultad + "] 📤 Solicitud de inscripción enviada...");

                String respuesta = envio.recvStr();
                System.out.println("[" + nombreFacultad + "] 📥 Respuesta de inscripción: " + respuesta);

                if ("Inscripción exitosa".equals(respuesta)) {
                    System.out.println("[" + nombreFacultad + "] ✅ Inscripción confirmada.");
                    inscrito = true;
                } else {
                    System.out.println("[" + nombreFacultad + "] ⚠️ Inscripción rechazada o inesperada. Reintentando...");
                    Thread.sleep(REINTENTO_MS);
                }
            } catch (Exception e) {
                System.err.println("[" + nombreFacultad + "] ❌ Error durante inscripción: " + e.getMessage());
                try { Thread.sleep(REINTENTO_MS); } catch (InterruptedException ignored) {}
            }
            intentos++;
        }

        if (!inscrito) {
            System.err.println("[" + nombreFacultad + "] ❌ No se pudo inscribir tras varios intentos. Finalizando.");
            envio.close();
            return;
        }

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

                recepcion.send(respuestaServidor);
                System.out.println("[" + nombreFacultad + "] ✅ Respuesta enviada al programa académico.");
            } catch (Exception e) {
                System.err.println("[" + nombreFacultad + "] ❌ Error procesando solicitud: " + e.getMessage());
            }
        }

        envio.close();
    }
}
