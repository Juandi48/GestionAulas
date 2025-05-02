package facultades;

import org.zeromq.ZMQ;

public class FacultadMain {

    private static final int PUERTO_RECEPCION = 6000;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java FacultadMain <nombreFacultad>");
            return;
        }

        String nombreFacultad = args[0];
        System.out.println("[FacultadMain] 🟢 Ejecutando Facultad: " + nombreFacultad);

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket recepcion = context.socket(ZMQ.REP);
        recepcion.bind("tcp://*:" + PUERTO_RECEPCION);

        Facultad.iniciar("localhost", nombreFacultad, recepcion, context);
    }
}
