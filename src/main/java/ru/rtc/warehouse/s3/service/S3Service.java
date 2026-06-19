package ru.rtc.warehouse.s3.service;

import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final String bucket;
    private final ProductEntityService productEntityService;
    private static final String PRODUCT_DIRECTORY = "products";

    public S3Service(
        S3Client s3Client,
        @Value("${s3.bucket:warehouse-images}") String bucket,
        ProductEntityService productEntityService
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.productEntityService = productEntityService;
    }

    public String uploadImage(
        MultipartFile file,
        String productSku,
        Long userId
    ) {
        String extension = getExtension(file.getOriginalFilename());
        String key = UUID.randomUUID() + extension;
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(PRODUCT_DIRECTORY + "/" + key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );
            log.info("Uploaded image: {}/{}", PRODUCT_DIRECTORY, key);
            Product product = productEntityService.findByUserIdAndSkuCode(
                userId,
                productSku
            );
            product.setImageUrl(getUrl(key));
            productEntityService.save(product);
            log.info(
                "Product SKU={} userId={} image set to {}",
                productSku,
                userId,
                getUrl(key)
            );

            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public ByteArrayResource getImage(UUID imageUid) {
        String uidStr = imageUid.toString();
        String prefix = PRODUCT_DIRECTORY + "/" + uidStr + ".";

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .maxKeys(1)
            .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(
            listRequest
        );

        if (listResponse.contents().isEmpty()) {
            throw new RuntimeException("File not found for uid: " + imageUid);
        }

        String foundKey = listResponse.contents().get(0).key();

        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(foundKey)
            .build();
        byte[] bytes = s3Client.getObjectAsBytes(getRequest).asByteArray();

        return new ByteArrayResource(bytes);
    }

    public ByteArrayResource getImageBySku(String sku, Long userId) {
        Product product = productEntityService.findByUserIdAndSkuCode(
            userId,
            sku
        );
        String productImageUrl = product.getImageUrl();
        if (productImageUrl == null || productImageUrl.isBlank()) {
            throw new RuntimeException("No image for SKU: " + sku);
        }
        UUID imageUid = UUID.fromString(
            productImageUrl.substring(productImageUrl.lastIndexOf('/') + 1)
        );
        return getImage(imageUid);
    }

    public String getUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return "/api/images/" + key.substring(0, key.lastIndexOf('.'));
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
        log.info("Deleted image: {}", key);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
