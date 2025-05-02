package servidor;

import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import modelo.Solicitud;

public class Servidor {

    private static final int PUERTO = 5555;

    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.ROUTER);
        socket.bind("tcp://*:5555");

        Map<String, String> facultades = new HashMap<>();
        AsignadorAulas asignador = new AsignadorAulas();
        Persistencia persistencia = new Persistencia();
        Gson gson = new Gson();

        System.out.println("[Servidor][async] 🟢 Escuchando en el puerto " + PUERTO);

        while (true) {
            try {
                byte[] identidad = socket.recv();
                socket.recv(); // frame vacío
                String mensaje = socket.recvStr();

                System.out.println("[Servidor] 🧾 Identidad: " + new String(identidad));
                System.out.println("[Servidor] 📨 Contenido: " + mensaje);

                Map<String, Object> datos = gson.fromJson(mensaje, new TypeToken<Map<String, Object>>() {}.getType());
                String tipo = (String) datos.get("tipo");

                if ("inscripcion".equals(tipo)) {
                    String facultad = (String) datos.get("facultad");
                    facultades.put(facultad, new String(identidad));
                    socket.send(identidad, ZMQ.SNDMORE);
                    socket.send("", ZMQ.SNDMORE);
                    socket.send("Inscripción exitosa");
                    System.out.println("✅ Facultad '" + facultad + "' registrada correctamente.");
                    continue;
                }

                Solicitud solicitud = gson.fromJson(mensaje, Solicitud.class);
                boolean ok = asignador.asignarAulas(solicitud);

                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("estado", ok ? "asignado" : "rechazado");
                respuesta.put("programa", solicitud.getPrograma());
                respuesta.put("facultad", solicitud.getFacultad());
                respuesta.put("semestre", solicitud.getSemestre());
                respuesta.put("salonesAsignados", ok ? solicitud.getSalones() : 0);
                respuesta.put("laboratoriosAsignados", ok ? solicitud.getLaboratorios() : 0);
                respuesta.put("motivo", ok ? "" : "⚠️ No hay suficientes aulas disponibles.");

                String respuestaJson = gson.toJson(respuesta);
                persistencia.guardar(ok ? "asignaciones" : "rechazos", respuestaJson);

                socket.send(identidad, ZMQ.SNDMORE);
                socket.send("", ZMQ.SNDMORE);
                socket.send(respuestaJson);
                System.out.println("📤 Respuesta enviada a facultad '" + solicitud.getFacultad() + "'");
            } catch (Exception e) {
                System.err.println("❌ Error procesando mensaje: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
