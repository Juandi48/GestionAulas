package servidor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Maneja la escritura persistente de solicitudes y respuestas, organizadas por semestre.
 * Evita duplicados y asegura trazabilidad de asignaciones y rechazos.
 */
public class Persistencia {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Guarda el contenido de una solicitud procesada, clasificando por tipo (asignación o rechazo)
     * y creando un archivo por semestre.
     *
     * @param tipo tipo de resultado: "asignaciones" o "rechazos"
     * @param contenido JSON con la respuesta, incluyendo el semestre
     */
    public void guardar(String tipo, String contenido) {
        Gson gson = new Gson();
        Map<String, Object> datos;

        try {
            datos = gson.fromJson(contenido, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            System.err.println("[Persistencia] ❌ No se pudo interpretar el contenido: " + contenido);
            return;
        }

        // Extraer semestre
        int semestre = ((Double) datos.getOrDefault("semestre", 0)).intValue();
        if (semestre == 0) {
            System.err.println("[Persistencia] ❌ Semestre no especificado. No se puede guardar.");
            return;
        }

        // Ruta por semestre y tipo
        String carpeta = "data/" + tipo;
        String archivo = carpeta + "/semestre_" + semestre + ".txt";

        // Crear carpeta si no existe
        try {
            Files.createDirectories(Paths.get(carpeta));
        } catch (IOException e) {
            System.err.println("[Persistencia] ❌ Error creando directorio: " + e.getMessage());
            return;
        }

        // Evitar duplicados exactos
        try {
            List<String> lineasExistentes = Files.exists(Paths.get(archivo)) ?
                    Files.readAllLines(Paths.get(archivo)) : Arrays.asList();

            if (lineasExistentes.stream().anyMatch(line -> line.contains(contenido))) {
                System.out.println("[Persistencia] ⚠️ Solicitud ya registrada previamente.");
                return;
            }
        } catch (IOException e) {
            System.err.println("[Persistencia] ❌ Error al leer archivo: " + e.getMessage());
        }

        // Escribir al archivo
        try (FileWriter writer = new FileWriter(archivo, true)) {
            writer.write(LocalDateTime.now().format(FORMATO) + " - " + contenido + "\n");
            System.out.println("[Persistencia] 📌 Registro guardado en " + archivo);
        } catch (IOException e) {
            System.err.println("[Persistencia] ❌ Error al escribir en el archivo: " + e.getMessage());
        }
    }
}
