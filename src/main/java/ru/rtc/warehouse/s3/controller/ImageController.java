package ru.rtc.warehouse.s3.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.s3.service.S3Service;

import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @GetMapping("/upload/{productSku}")
    public ResponseEntity<Resource> getImageBySku(@PathVariable String productSku) {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(s3Service.getImageBySku(productSku));
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
        String key = s3Service.uploadImage(file, productSku);
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
}
