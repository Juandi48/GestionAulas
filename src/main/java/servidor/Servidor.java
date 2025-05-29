package servidor;

import java.util.HashSet;
import java.util.Set;

import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import modelo.Solicitud;

public class Servidor {

    private static final int PUERTO = 5555;

    // Registro de facultades inscritas
    private static final Set<String> facultades = new HashSet<>();

    public static boolean esFacultadInscrita(String nombreFacultad) {
        return facultades.contains(nombreFacultad);
    }

    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.bind("tcp://0.0.0.0:" + PUERTO);

        AsignadorAulas asignador = new AsignadorAulas();
        Persistencia persistencia = new Persistencia();
        Gson gson = new Gson();

        System.out.println("Servidor escuchando en el puerto " + PUERTO + "...");

        while (!Thread.currentThread().isInterrupted()) {
            String mensaje = socket.recvStr();
            System.out.println("📩 Mensaje recibido: " + mensaje);

            String respuesta;

            try {
                JsonObject objeto = JsonParser.parseString(mensaje).getAsJsonObject();
                String tipo = objeto.get("tipo").getAsString();

                switch (tipo) {
                    case "inscripcion":
                        String facultad = objeto.get("facultad").getAsString();
                        if (facultades.add(facultad)) {
                            respuesta = "✅ Facultad '" + facultad + "' inscrita correctamente.";
                        } else {
                            respuesta = "ℹ️ Facultad '" + facultad + "' ya estaba inscrita.";
                        }
                        break;

                    case "solicitud":
                        String contenido = objeto.get("contenido").getAsString();
                        Solicitud solicitud = gson.fromJson(contenido, Solicitud.class);
                        boolean exito = asignador.asignarAulas(solicitud);
                        respuesta = exito
                                ? "✅ Aula asignada correctamente a " + solicitud.getPrograma()
                                : "❌ No se pudo asignar el aula. Recursos insuficientes.";
                        break;

                    case "salud":
                        respuesta = "🟢 OK - Servidor activo";
                        break;

                    default:
                        respuesta = "❌ Tipo de mensaje no reconocido: " + tipo;
                }

            } catch (Exception e) {
                respuesta = "❌ Error al procesar el mensaje: " + e.getMessage();
                e.printStackTrace();
            }

            socket.send(respuesta);
        }

        socket.close();
        context.term();
    }
}
