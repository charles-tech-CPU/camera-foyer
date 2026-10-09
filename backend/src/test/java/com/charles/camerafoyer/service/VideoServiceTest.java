package com.charles.camerafoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.charles.camerafoyer.dto.VideoDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoServiceTest {

    @TempDir
    Path root;

    private Path videos;
    private VideoService service;

    @BeforeEach
    void setUp() throws IOException {
        videos = Files.createDirectory(root.resolve("jours"));
        service = new VideoService(videos.toString());
    }

    private void video(String name, int size) throws IOException {
        Files.write(videos.resolve(name), new byte[size]);
    }

    @Test
    void listeLesVideosDeLaPlusRecenteALaPlusAncienne() throws IOException {
        video("2026-10-07.mp4", 10);
        video("2026-10-09.mp4", 30);
        video("2026-10-08.mp4", 20);

        assertThat(service.findAll())
                .containsExactly(
                        new VideoDto(LocalDate.of(2026, 10, 9), 30),
                        new VideoDto(LocalDate.of(2026, 10, 8), 20),
                        new VideoDto(LocalDate.of(2026, 10, 7), 10));
    }

    @Test
    void ignoreLesFichiersQuiNeSontPasDesVideosDuJour() throws IOException {
        video("2026-10-09.mp4", 5);
        video("2026-10-08.mp4.part", 5);
        video("2026-10-08.avi", 5);
        video("2026-02-30.mp4", 5);
        video("notes.mp4", 5);
        video("26-10-08.mp4", 5);
        Files.createDirectory(videos.resolve("2026-10-01.mp4"));

        assertThat(service.findAll()).extracting(VideoDto::date).containsExactly(LocalDate.of(2026, 10, 9));
    }

    @Test
    void dossierAbsentDonneUneListeVide() {
        assertThat(new VideoService(root.resolve("absent").toString()).findAll())
                .isEmpty();
    }

    @Test
    void supprimeLaVideoDuJour() throws IOException {
        video("2026-10-09.mp4", 5);
        video("2026-10-08.mp4", 5);

        service.delete("2026-10-09");

        assertThat(videos.resolve("2026-10-09.mp4")).doesNotExist();
        assertThat(videos.resolve("2026-10-08.mp4")).exists();
    }

    @Test
    void supprimerUneVideoAbsenteEstUneErreur404() {
        assertThatThrownBy(() -> service.delete("2026-10-09")).isInstanceOf(VideoNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "../secret",
                "..",
                "2026-10-09/../../secret",
                "..\\secret",
                "/etc/passwd",
                "2026-10-9",
                "2026-10-09.mp4",
                "2026-13-01",
                "2026-02-30",
                " 2026-10-09",
                ""
            })
    void refuseToutParametreQuiNEstPasUneDateStricte(String date) throws IOException {
        // Un fichier hors du dossier des videos ne doit jamais pouvoir etre atteint
        Path secret = Files.write(root.resolve("secret.mp4"), new byte[1]);

        assertThatThrownBy(() -> service.delete(date)).isInstanceOf(IllegalArgumentException.class);
        assertThat(secret).exists();
    }
}
