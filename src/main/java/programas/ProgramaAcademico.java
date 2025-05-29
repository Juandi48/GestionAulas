package programas;

import org.zeromq.ZMQ;
import com.google.gson.Gson;
import modelo.Solicitud;

import java.util.HashMap;
import java.util.Map;

public class ProgramaAcademico {

    private static final int PUERTO_FACULTAD = 6000;

    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.println("❌ Número incorrecto de argumentos.");
            System.out.println("Uso: java ProgramaAcademico <nombrePrograma> <nombreFacultad> <semestre> <salones> <laboratorios> <ipFacultad>");
            return;
        }

        String nombrePrograma = args[0];
        String nombreFacultad = args[1];
        int semestre = Integer.parseInt(args[2]);
        int salones = Integer.parseInt(args[3]);
        int laboratorios = Integer.parseInt(args[4]);
        String ipFacultad = args[5];

        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://" + ipFacultad + ":" + PUERTO_FACULTAD);

        Solicitud solicitud = new Solicitud(nombrePrograma, nombreFacultad, semestre, salones, laboratorios);
        Gson gson = new Gson();
        String jsonSolicitud = gson.toJson(solicitud);

        // Crear el mensaje envolvente con tipo y contenido
        Map<String, String> mensaje = new HashMap<>();
        mensaje.put("tipo", "solicitud");
        mensaje.put("contenido", jsonSolicitud);

        socket.send(gson.toJson(mensaje));
        String respuesta = socket.recvStr();

        System.out.println("Respuesta de la facultad: " + respuesta);

        socket.close();
        context.term();
    }
}
