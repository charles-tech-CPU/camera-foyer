package com.charles.camerafoyer.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

    private static final String[] DEFAULTS = {"http://*:5179", "https://*.ts.net:[*]"};

    /** Expose la configuration CORS enregistree (methode protegee de CorsRegistry). */
    private static final class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    private static CorsConfiguration apiCors(String allowedOrigins) {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        new WebConfig(allowedOrigins).addCorsMappings(registry);
        return registry.configurations().get("/api/**");
    }

    @Test
    void chaqueMotifDUneListeEstAutorise() {
        CorsConfiguration api = apiCors("http://*:8186, https://*:8286");

        assertThat(api.getAllowedOriginPatterns()).containsExactly("http://*:8186", "https://*:8286");
        for (String origin : new String[] {
            "http://192.168.1.20:8186",
            "http://localhost:8186",
            "https://serveur-foyer.tail0af124.ts.net:8286",
            "https://localhost:8286"
        }) {
            assertThat(api.checkOrigin(origin)).as(origin).isEqualTo(origin);
        }
    }

    @Test
    void uneOrigineHorsDeLaListeEstRefusee() {
        CorsConfiguration api = apiCors("http://*:8186,https://*:8286");

        // bon port mais mauvais protocole, et motifs par defaut remplaces par la liste
        assertThat(api.checkOrigin("https://192.168.1.20:8186")).isNull();
        assertThat(api.checkOrigin("http://serveur-foyer.tail0af124.ts.net:8286"))
                .isNull();
        assertThat(api.checkOrigin("http://localhost:5179")).isNull();
    }

    @Test
    void sansVariableLeFrontendViteEtTailscaleSontAutorises() {
        CorsConfiguration api = apiCors("");

        assertThat(api.getAllowedOriginPatterns()).containsExactly(DEFAULTS);
        assertThat(api.checkOrigin("http://localhost:5179")).isEqualTo("http://localhost:5179");
        assertThat(api.checkOrigin("http://192.168.1.20:5179")).isEqualTo("http://192.168.1.20:5179");
        assertThat(api.checkOrigin("https://serveur-foyer.tail0af124.ts.net:8443"))
                .isEqualTo("https://serveur-foyer.tail0af124.ts.net:8443");
        assertThat(api.checkOrigin("http://autre-site.example")).isNull();
        assertThat(api.getAllowedMethods()).contains("GET", "DELETE");
    }

    @Test
    void decoupageDeLaListe() {
        assertThat(WebConfig.parseOrigins("http://*:8186")).containsExactly("http://*:8186");
        assertThat(WebConfig.parseOrigins(" http://*:8186 , ,https://*:8286, "))
                .containsExactly("http://*:8186", "https://*:8286");
        assertThat(WebConfig.parseOrigins(null)).containsExactly(DEFAULTS);
        assertThat(WebConfig.parseOrigins(" , ")).containsExactly(DEFAULTS);
    }
}
