package tolerancia;

import org.zeromq.ZMQ;

/**
 * HealthChecker que monitorea periódicamente al servidor principal (modo síncrono).
 * Si no obtiene respuesta, activa automáticamente el servidor réplica.
 */
public class HealthChecker {

    private static final String IP_SERVIDOR = "localhost";
    private static final int PUERTO_SERVIDOR = 5555;
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

                // Esperar respuesta (bloqueante pero con timeout)
                socket.setReceiveTimeOut(INTERVALO_MS);
                String respuesta = socket.recvStr();

                if (respuesta != null) {
                    System.out.println("[HealthChecker] ✅ Servidor activo. Respuesta: " + respuesta);
                } else {
                    System.out.println("[HealthChecker] ❌ El servidor no respondió. Activando réplica...");
                    Runtime.getRuntime().exec("java tolerancia.ServidorReplica");
                    break;
                }

            } catch (Exception e) {
                System.out.println("[HealthChecker] ⚠️ Error de conexión: " + e.getMessage());
            } finally {
                socket.close();
                context.term();
            }

            Thread.sleep(INTERVALO_MS);
        }
    }
}
