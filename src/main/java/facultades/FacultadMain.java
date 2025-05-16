package facultades;

import org.zeromq.ZMQ;

public class FacultadMain {

    private static final int PUERTO_RECEPCION = 6000;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java FacultadMain <nombreFacultad> <ipServidor>");
            return;
        }

        String nombreFacultad = args[0];
        String ipServidor = args[1];

        System.out.println("[FacultadMain] 🟢 Ejecutando Facultad: " + nombreFacultad);

        // Crear contexto y socket compartido REP
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket recepcion = context.socket(ZMQ.REP);
        recepcion.bind("tcp://*:" + PUERTO_RECEPCION);

        // Iniciar una sola facultad con el nombre y la IP del servidor
        Thread hiloFacultad = new Thread(() ->
            Facultad.iniciar(ipServidor, nombreFacultad, recepcion, context)
        );
        hiloFacultad.start();
    }
}
