package com.charles.camerafoyer.service;

public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException(String date) {
        super("Aucune video pour le " + date + ".");
    }
}
