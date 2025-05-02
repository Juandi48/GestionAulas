package facultades;

import org.zeromq.ZMQ;

public class FacultadesLauncher {

    private static final int PUERTO_RECEPCION = 6000;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java FacultadesLauncher <Facultad1> <Facultad2> ...");
            return;
        }

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket recepcion = context.socket(ZMQ.REP);
        recepcion.bind("tcp://*:" + PUERTO_RECEPCION);
        System.out.println("[FacultadesLauncher] 🔗 Escuchando en puerto compartido: " + PUERTO_RECEPCION);

        for (String nombreFacultad : args) {
            Thread hilo = new Thread(() -> Facultad.iniciar("localhost", nombreFacultad, recepcion, context));
            hilo.start();
        }
    }
}
