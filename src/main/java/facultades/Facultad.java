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
            socketServidor.setReceiveTimeOut(3000); // 3 segundos

            boolean conectado = false;
            String rutaConfig = "replica_config.txt";

            Gson gson = new Gson();
            Map<String, String> mensajeInscripcion = new HashMap<>();
            mensajeInscripcion.put("tipo", "inscripcion");
            mensajeInscripcion.put("facultad", nombreFacultad);

            // Intento con el servidor principal
            try {
                socketServidor.connect("tcp://" + ipServidor + ":" + PUERTO_SERVIDOR);
                socketServidor.send(gson.toJson(mensajeInscripcion));
                String respuesta = socketServidor.recvStr();

                if (respuesta != null) {
                    System.out.println("🔗 Conectado al servidor principal en " + ipServidor + ":" + PUERTO_SERVIDOR);
                    System.out.println("✅ Respuesta del servidor: " + respuesta);
                    conectado = true;
                } else {
                    System.out.println("⏳ Sin respuesta del servidor principal. Buscando réplica...");
                }

            } catch (Exception e) {
                System.out.println("❌ Error al intentar conectar al servidor principal. Buscando réplica...");
            }

            // Si no hay conexión, buscar réplica
            if (!conectado) {
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

                    socketServidor = context.createSocket(ZMQ.REQ);
                    socketServidor.setReceiveTimeOut(3000);
                    socketServidor.connect("tcp://" + ip + ":" + puerto);
                    System.out.println("🔁 Conectado a réplica en " + ip + ":" + puerto);

                    socketServidor.send(gson.toJson(mensajeInscripcion));
                    String respuestaReplica = socketServidor.recvStr();

                    if (respuestaReplica != null) {
                        System.out.println("✅ Respuesta del servidor réplica: " + respuestaReplica);
                        conectado = true;
                    } else {
                        System.out.println("❌ No hubo respuesta del servidor réplica.");
                    }

                } catch (IOException ioEx) {
                    System.out.println("❌ No se pudo leer replica_config.txt: " + ioEx.getMessage());
                }
            }

            if (!conectado) {
                System.out.println("🚫 No se pudo establecer conexión con ningún servidor.");
                return;
            }

            // Abrir socket de recepción
            ZMQ.Socket socketRecepcion = context.createSocket(ZMQ.REP);
            socketRecepcion.bind("tcp://*:" + PUERTO_RECEPCION);

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
