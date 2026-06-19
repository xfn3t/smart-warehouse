package ru.rtc.warehouse.s3.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.s3.service.S3Service;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @GetMapping("/upload/{productSku}")
    public ResponseEntity<Resource> getImageBySku(
        @PathVariable String productSku
    ) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(s3Service.getImageBySku(productSku, userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload/{productSku}")
    public ResponseEntity<String> upload(
        @RequestParam("file") MultipartFile file,
        @PathVariable String productSku
    ) {
        Long userId = getCurrentUserId();
        String key = s3Service.uploadImage(file, productSku, userId);
        return ResponseEntity.ok(s3Service.getUrl(key));
    }

    @GetMapping("/{key}")
    public ResponseEntity<Resource> getImage(@PathVariable String key) {
        try {
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(s3Service.getImage(UUID.fromString(key)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /** Delete image */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        s3Service.delete(key);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
