package com.carbonmarketplace.userservice.dto.request;

import com.carbonmarketplace.userservice.entity.KycDocument;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentRequest {

    @NotNull(message = "KYC level is required")
    @Min(value = 1, message = "KYC level must be at least 1")
    @Max(value = 2, message = "KYC level cannot exceed 2")
    private Integer kycLevel;

    @NotNull(message = "ID type is required")
    private KycDocument.IdType idType;

    @NotBlank(message = "ID number is required")
    @Size(max = 100, message = "ID number is too long")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "ID number contains invalid characters")
    private String idNumber;

    // File uploads will be handled separately
    private transient MultipartFile idFrontFile;
    private transient MultipartFile idBackFile;
    private transient MultipartFile selfieFile;
    private transient MultipartFile addressProofFile;
    private transient MultipartFile businessLicenseFile;

    // These will be set after file upload
    private String idFrontUrl;
    private String idBackUrl;
    private String selfieUrl;
    private String addressProofUrl;
    private String businessLicenseUrl;
}
