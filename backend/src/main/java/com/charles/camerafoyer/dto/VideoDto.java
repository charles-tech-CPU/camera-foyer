package com.charles.camerafoyer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/** Video d'une journee : sa date (tiree du nom AAAA-MM-JJ.mp4) et sa taille en octets. */
public record VideoDto(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate date, long size) {}
