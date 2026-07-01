package com.chaoslab.infrastructure.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Catálogo de topologías de ejemplo que el dashboard puede correr. Las lee del classpath y las
 * materializa a un archivo temporal para que el caso de uso (que trabaja con {@code Path}) las cargue.
 */
@Component
public class TopologyCatalog {

    private static final Map<String, String> RESOURCES = new LinkedHashMap<>();

    static {
        RESOURCES.put("order-api", "topologies/order-api.yaml");
        RESOURCES.put("resilient-order-api", "topologies/resilient-order-api.yaml");
    }

    /** Nombres de las topologías disponibles. */
    public List<String> names() {
        return List.copyOf(RESOURCES.keySet());
    }

    /**
     * Copia la topología a un archivo temporal y devuelve su ruta.
     *
     * @throws IllegalArgumentException si el nombre no existe
     * @throws IOException              si falla la lectura del recurso
     */
    public Path materialize(String name) throws IOException {
        String resource = RESOURCES.get(name);
        if (resource == null) {
            throw new IllegalArgumentException("topología desconocida: '" + name + "'");
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("no se encontró el recurso: " + resource);
            }
            Path temp = Files.createTempFile("chaoslab-", ".yaml");
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}
