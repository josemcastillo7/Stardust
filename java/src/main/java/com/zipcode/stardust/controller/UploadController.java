package com.zipcode.stardust.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /** Accept any image/* or video/* type except SVG (can carry embedded scripts). */
    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        if ("image/svg+xml".equals(contentType)) return false;
        return contentType.startsWith("image/") || contentType.startsWith("video/");
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,
                                     Authentication auth) {
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty."));
        }

        String rawType = file.getContentType();
        if (!isAllowedType(rawType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unsupported file type: " + rawType));
        }
        // isAllowedType returned true, so rawType is non-null here
        String contentType = rawType;

        String ext = getExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + ext;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Could not save file."));
        }

        return ResponseEntity.ok(Map.of(
                "url", "/uploads/" + filename,
                "type", contentType.startsWith("video/") ? "video" : "image"
        ));
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        // Fallback from MIME type
        return switch (contentType) {
            case "image/jpeg"       -> ".jpg";
            case "image/png"        -> ".png";
            case "image/gif"        -> ".gif";
            case "image/webp"       -> ".webp";
            case "image/bmp"        -> ".bmp";
            case "image/svg+xml"    -> ".svg";
            case "image/avif"       -> ".avif";
            case "video/mp4"        -> ".mp4";
            case "video/webm"       -> ".webm";
            case "video/ogg"        -> ".ogv";
            case "video/quicktime"  -> ".mov";
            case "video/x-msvideo"  -> ".avi";
            case "video/x-matroska" -> ".mkv";
            default                 -> ".bin";
        };
    }
}
