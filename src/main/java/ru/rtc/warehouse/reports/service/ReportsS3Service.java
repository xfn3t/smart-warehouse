package ru.rtc.warehouse.reports.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportsS3Service {

    private final S3Client s3Client;

    @Value("${s3.bucket:warehouse-images}")
    private String bucket;

    private static final String REPORTS_DIRECTORY = "reports";

    public String uploadPdf(byte[] pdfBytes) {
        String uid = UUID.randomUUID().toString();
        String key = REPORTS_DIRECTORY + "/" + uid + ".pdf";
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/pdf")
                .build(),
            RequestBody.fromBytes(pdfBytes)
        );
        log.info("Uploaded report PDF: {}", key);
        return key;
    }

    public ByteArrayResource downloadPdf(String s3Key) {
        byte[] bytes = s3Client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build()
        ).asByteArray();
        return new ByteArrayResource(bytes);
    }
}
