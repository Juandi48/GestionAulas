package tolerancia;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;

/**
 * HealthChecker que monitorea periódicamente al servidor principal (modo async).
 * Si no obtiene respuesta, activa automáticamente el servidor réplica.
 */
public class HealthChecker {

    private static final String IP_SERVIDOR = "localhost";
    private static final int PUERTO_SERVIDOR = 5555;
    private static final int INTERVALO_MS = 5000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("[HealthChecker][async] Iniciando monitoreo al servidor en modo asíncrono...");

        while (true) {
            ZMQ.Context context = ZMQ.context(1);
            ZMQ.Socket socket = context.socket(ZMQ.DEALER);
            socket.setIdentity("HEALTH".getBytes(ZMQ.CHARSET));
            socket.connect("tcp://" + IP_SERVIDOR + ":" + PUERTO_SERVIDOR);

            // Enviar ping vacío
            socket.send("PING");

            // Usar ZMQ.Poller para esperar respuesta
            Poller poller = context.poller(1);
            poller.register(socket, Poller.POLLIN);
            int polled = poller.poll(3000);  // espera hasta 3 segundos

            if (polled > 0 && poller.pollin(0)) {
                String respuesta = socket.recvStr();
                System.out.println("[HealthChecker] ✅ Respuesta del servidor: " + respuesta);
            } else {
                System.out.println("[HealthChecker] ❌ No hubo respuesta del servidor, considerar failover.");
            }

            socket.close();
            context.term();
            Thread.sleep(INTERVALO_MS);
        }
    }
}
