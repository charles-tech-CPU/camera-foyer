package com.charles.camerafoyer.service;

import com.charles.camerafoyer.dto.VideoDto;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Videos journalieres de la camera : un fichier AAAA-MM-JJ.mp4 par jour dans le dossier
 * app.videos.dir (variable VIDEOS_DIR). Pas de base de donnees : le dossier fait foi.
 */
@Service
public class VideoService {

    /** Format strict d'une date de video. Seul ce format est accepte pour construire un chemin. */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final String EXTENSION = ".mp4";

    // "uuuu" + STRICT : refuse aussi les dates impossibles (2026-02-30)
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final Path videosDir;

    public VideoService(@Value("${app.videos.dir}") String videosDir) {
        this.videosDir = Path.of(videosDir).toAbsolutePath().normalize();
    }

    /** Videos presentes dans le dossier, de la plus recente a la plus ancienne. Dossier absent : liste vide. */
    public List<VideoDto> findAll() {
        if (!Files.isDirectory(videosDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(videosDir)) {
            return files.filter(Files::isRegularFile)
                    .map(this::toDto)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(VideoDto::date).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Supprime la video du jour donne (AAAA-MM-JJ).
     *
     * @throws IllegalArgumentException si la date n'est pas au format attendu
     * @throws VideoNotFoundException si aucune video n'existe pour cette date
     */
    public void delete(String date) {
        Path file = resolve(date);
        try {
            if (!Files.isRegularFile(file) || !Files.deleteIfExists(file)) {
                throw new VideoNotFoundException(date);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Chemin du fichier d'une date, construit uniquement apres validation stricte : la date ne
     * peut contenir que des chiffres et des tirets, donc ni "/", ni "..", ni "\" (parcours de
     * repertoire impossible). Le controle final sur le dossier parent est une ceinture de securite.
     */
    private Path resolve(String date) {
        if (parseDate(date) == null) {
            throw new IllegalArgumentException("Date invalide, format attendu AAAA-MM-JJ : " + date);
        }
        Path file = videosDir.resolve(date + EXTENSION).normalize();
        if (!videosDir.equals(file.getParent())) {
            throw new IllegalArgumentException("Date invalide : " + date);
        }
        return file;
    }

    /** Video correspondant a un fichier "AAAA-MM-JJ.mp4", ou null pour tout autre fichier. */
    private VideoDto toDto(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(EXTENSION)) {
            return null;
        }
        LocalDate date = parseDate(name.substring(0, name.length() - EXTENSION.length()));
        if (date == null) {
            return null;
        }
        try {
            return new VideoDto(date, Files.size(file));
        } catch (IOException ex) {
            // Fichier supprime entre le listing et la lecture de sa taille (ex: purge automatique)
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || !DATE_PATTERN.matcher(value).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
