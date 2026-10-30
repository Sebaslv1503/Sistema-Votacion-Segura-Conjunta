package ec.espe.votacion_segura_backend;

import ec.espe.votacion_segura_backend.config.AppProperties;
import ec.espe.votacion_segura_backend.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación.
 *
 * @EnableConfigurationProperties: habilita las propiedades tipadas.
 * @EnableScheduling: necesario para la tarea de limpieza de tokens vencidos.
 */
@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
@EnableScheduling
public class VotacionSeguraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VotacionSeguraBackendApplication.class, args);
    }
}
