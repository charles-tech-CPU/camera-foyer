package com.charles.camerafoyer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.charles.camerafoyer.service.VideoService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Routes HTTP de bout en bout sur un vrai dossier temporaire (codes de retour, JSON). */
class VideoControllerTest {

    @TempDir
    Path videos;

    private MockMvc mvc;

    @BeforeEach
    void setUp() throws IOException {
        Files.write(videos.resolve("2026-10-08.mp4"), new byte[1024]);
        Files.write(videos.resolve("2026-10-09.mp4"), new byte[2048]);
        mvc = MockMvcBuilders.standaloneSetup(new VideoController(new VideoService(videos.toString())))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getRenvoieDateEtTailleDuPlusRecentAuPlusAncien() throws Exception {
        mvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-10-09"))
                .andExpect(jsonPath("$[0].size").value(2048))
                .andExpect(jsonPath("$[1].date").value("2026-10-08"))
                .andExpect(jsonPath("$[1].size").value(1024));
    }

    @Test
    void deleteRenvoie204PuisLeFichierAdisparu() throws Exception {
        mvc.perform(delete("/api/videos/2026-10-09")).andExpect(status().isNoContent());

        assertThat(videos.resolve("2026-10-09.mp4")).doesNotExist();
    }

    @Test
    void deleteDUneVideoAbsenteRenvoie404() throws Exception {
        mvc.perform(delete("/api/videos/2026-10-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void deleteAvecUneDateInvalideRenvoie400() throws Exception {
        mvc.perform(delete("/api/videos/{date}", "..%2F2026-10-09")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/videos/2026-10-9")).andExpect(status().isBadRequest());

        assertThat(videos.resolve("2026-10-09.mp4")).exists();
    }
}
