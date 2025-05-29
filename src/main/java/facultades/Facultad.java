package facultades;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

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

            // Lógica de conexión con tolerancia a fallos
            boolean conectado = false;
            String rutaConfig = "src/main/replica_config.txt";

            try {
                socketServidor.connect("tcp://" + ipServidor + ":" + PUERTO_SERVIDOR);
                conectado = true;
                System.out.println("🔗 Conectado al servidor principal en " + ipServidor + ":" + PUERTO_SERVIDOR);
            } catch (Exception e) {
                System.out.println("❌ No se pudo conectar al servidor principal. Buscando réplica...");

                try (BufferedReader reader = new BufferedReader(new FileReader(rutaConfig))) {
                    String ip = "localhost";
                    int puerto = 5556;

                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        if (linea.startsWith("IP=")) {
                            ip = linea.substring(3);
                        } else if (linea.startsWith("PUERTO=")) {
                            puerto = Integer.parseInt(linea.substring(7));
                        }
                    }

                    System.out.println("🔁 Conectando a réplica en " + ip + ":" + puerto + "...");
                    socketServidor.connect("tcp://" + ip + ":" + puerto);
                    conectado = true;

                } catch (IOException ex) {
                    System.out.println("❌ No se pudo leer la configuración de réplica.");
                }
            }

            if (!conectado) {
                System.out.println("🚫 No se pudo establecer conexión con ningún servidor.");
                return;
            }

            // Crear socket de recepción para programas académicos
            ZMQ.Socket socketRecepcion = context.createSocket(ZMQ.REP);
            socketRecepcion.bind("tcp://*:" + PUERTO_RECEPCION);

            Gson gson = new Gson();

            // Enviar inscripción al servidor conectado
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

            socketRecepcion.close();
            socketServidor.close();
        }
    }
}
