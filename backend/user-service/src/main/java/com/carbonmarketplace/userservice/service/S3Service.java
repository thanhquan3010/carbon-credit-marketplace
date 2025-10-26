package com.carbonmarketplace.userservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket-name:carbon-marketplace-kyc}")
    private String bucketName;

    @Value("${aws.s3.region:ap-southeast-1}")
    private String region;

    /**
     * Upload file to S3
     */
    public String uploadFile(MultipartFile file, String fileName) {
        try {
            String key = generateUniqueFileName(fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucketName,
                        key,
                        inputStream,
                        metadata).withCannedAcl(CannedAccessControlList.Private);

                s3Client.putObject(putObjectRequest);
            }

            String fileUrl = getFileUrl(key);
            log.info("File uploaded successfully to S3: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Upload file with custom content type
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType, long contentLength) {
        try {
            String key = generateUniqueFileName(fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(contentLength);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    key,
                    inputStream,
                    metadata).withCannedAcl(CannedAccessControlList.Private);

            s3Client.putObject(putObjectRequest);

            String fileUrl = getFileUrl(key);
            log.info("File uploaded successfully to S3: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, key);
            s3Client.deleteObject(deleteObjectRequest);

            log.info("File deleted successfully from S3: {}", key);

        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Generate pre-signed URL for temporary access
     */
    public String generatePresignedUrl(String fileUrl, int expirationMinutes) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000L * 60 * expirationMinutes;
            expiration.setTime(expTimeMillis);

            return s3Client.generatePresignedUrl(bucketName, key, expiration).toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            return s3Client.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            log.error("Error checking file existence", e);
            return false;
        }
    }

    /**
     * Generate unique file name
     */
    private String generateUniqueFileName(String originalFileName) {
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + fileExtension;
    }

    /**
     * Get file URL from key
     */
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * Extract S3 key from URL
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("File URL cannot be empty");
        }

        // Remove the base URL to get the key
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        // If it's already a key, return as is
        return fileUrl;
    }
}
