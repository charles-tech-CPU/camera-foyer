package com.charles.camerafoyer.web;

import com.charles.camerafoyer.dto.VideoDto;
import com.charles.camerafoyer.service.VideoService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liste et suppression des videos. La lecture des fichiers n'est PAS servie ici : nginx les
 * sert directement sous /videos/AAAA-MM-JJ.mp4 (requetes Range pour avancer dans la video).
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public List<VideoDto> findAll() {
        return videoService.findAll();
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> delete(@PathVariable String date) {
        videoService.delete(date);
        return ResponseEntity.noContent().build();
    }
}
