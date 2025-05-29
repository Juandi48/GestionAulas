package tolerancia;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

import org.zeromq.ZMQ;

/**
 * HealthChecker que monitorea periódicamente al servidor principal (modo
 * síncrono). Si no obtiene respuesta, activa automáticamente el servidor
 * réplica y escribe su IP y puerto en replica_config.txt
 */
public class HealthChecker {

    private static final String IP_SERVIDOR = "localhost";
    private static final int PUERTO_SERVIDOR = 5555;
    private static final int PUERTO_REPLICA = 5556;
    private static final int INTERVALO_MS = 10000;  // 10 segundos entre chequeos

    public static void main(String[] args) throws InterruptedException {
        String ip = args.length > 0 ? args[0] : IP_SERVIDOR;
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : PUERTO_SERVIDOR;

        System.out.println("[HealthChecker][sync] Iniciando monitoreo al servidor en modo síncrono...");

        while (true) {
            ZMQ.Context context = ZMQ.context(1);
            ZMQ.Socket socket = context.socket(ZMQ.REQ);

            try {
                socket.connect("tcp://" + ip + ":" + puerto);

                // Enviar mensaje dummy (tipo JSON válido)
                String mensajeSalud = "{\"tipo\":\"salud\"}";
                socket.send(mensajeSalud);

                // Esperar respuesta con timeout
                socket.setReceiveTimeOut(INTERVALO_MS);
                String respuesta = socket.recvStr();

                if (respuesta != null) {
                    System.out.println("[HealthChecker] ✅ Servidor activo. Respuesta: " + respuesta);
                } else {
                    activarReplica();
                    break;
                }

            } catch (Exception e) {
                System.out.println("[HealthChecker] ⚠️ Error de conexión: " + e.getMessage());
                activarReplica();
                break;
            } finally {
                socket.close();
                context.term();
            }

            Thread.sleep(INTERVALO_MS);
        }
    }

    private static void activarReplica() {
        System.out.println("[HealthChecker] ❌ El servidor no respondió. Activando réplica...");

        // Obtener IP local para registrar en archivo
        String ipLocal = "localhost";
        try {
            ipLocal = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            System.out.println("[HealthChecker] ⚠️ No se pudo obtener la IP local: " + ex.getMessage());
        }

        // Escribir archivo de configuración
        String rutaConfig = "src/main/replica_config.txt";
        try (PrintWriter writer = new PrintWriter(rutaConfig)) {
            writer.println("IP=" + ipLocal);
            writer.println("PUERTO=" + PUERTO_REPLICA);
            System.out.println("[HealthChecker] 📄 Configuración de réplica escrita en replica_config.txt");
        } catch (IOException ioEx) {
            System.out.println("[HealthChecker] ❌ No se pudo escribir replica_config.txt: " + ioEx.getMessage());
        }

        // Lanzar réplica
        tolerancia.ServidorReplica.main(new String[]{String.valueOf(PUERTO_REPLICA)});
    }
}
