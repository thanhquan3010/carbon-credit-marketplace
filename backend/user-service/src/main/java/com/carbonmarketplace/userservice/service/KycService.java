package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.KycDocumentRequest;
import com.carbonmarketplace.userservice.entity.KycDocument;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.KycDocumentNotFoundException;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.repository.KycDocumentRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/jpg", "image/png");

    public KycDocument submitKycDocument(UUID userId, KycDocumentRequest request) {
        log.info("Submitting KYC document for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if KYC document already exists for this level
        kycDocumentRepository.findByUserIdAndKycLevel(userId, request.getKycLevel())
                .ifPresent(doc -> {
                    if (doc.getStatus() == KycDocument.VerificationStatus.APPROVED) {
                        throw new IllegalStateException("KYC level " + request.getKycLevel() + " already approved");
                    }
                });

        // Upload files to S3
        String idFrontUrl = uploadFile(request.getIdFrontFile(), userId, "id-front");
        String idBackUrl = request.getIdBackFile() != null ? uploadFile(request.getIdBackFile(), userId, "id-back")
                : null;
        String selfieUrl = uploadFile(request.getSelfieFile(), userId, "selfie");
        String addressProofUrl = request.getAddressProofFile() != null
                ? uploadFile(request.getAddressProofFile(), userId, "address-proof")
                : null;
        String businessLicenseUrl = request.getBusinessLicenseFile() != null
                ? uploadFile(request.getBusinessLicenseFile(), userId, "business-license")
                : null;

        // Create KYC document
        KycDocument kycDocument = KycDocument.builder()
                .user(user)
                .kycLevel(request.getKycLevel())
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .idFrontUrl(idFrontUrl)
                .idBackUrl(idBackUrl)
                .selfieUrl(selfieUrl)
                .addressProofUrl(addressProofUrl)
                .businessLicenseUrl(businessLicenseUrl)
                .status(KycDocument.VerificationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        KycDocument savedDocument = kycDocumentRepository.save(kycDocument);

        // Update user KYC status
        user.setKycStatus(User.KycStatus.PENDING);
        user.setKycSubmittedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("KYC document submitted successfully with ID: {}", savedDocument.getKycDocId());
        return savedDocument;
    }

    public void approveKycDocument(UUID kycDocId, UUID reviewerId, Integer approvedLevel) {
        log.info("Approving KYC document: {}", kycDocId);

        KycDocument document = kycDocumentRepository.findById(kycDocId)
                .orElseThrow(() -> new KycDocumentNotFoundException("KYC document not found"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new UserNotFoundException("Reviewer not found"));

        document.setStatus(KycDocument.VerificationStatus.APPROVED);
        document.setReviewedBy(reviewer);
        document.setReviewedAt(LocalDateTime.now());
        document.setExpiresAt(LocalDateTime.now().plusYears(1)); // KYC valid for 1 year

        kycDocumentRepository.save(document);

        // Update user KYC status
        User user = document.getUser();
        user.setKycStatus(User.KycStatus.APPROVED);
        user.setKycLevel(approvedLevel);
        user.setKycApprovedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("KYC document approved for user: {}", user.getUserId());
    }

    public void rejectKycDocument(UUID kycDocId, UUID reviewerId, String rejectionReason) {
        log.info("Rejecting KYC document: {}", kycDocId);

        KycDocument document = kycDocumentRepository.findById(kycDocId)
                .orElseThrow(() -> new KycDocumentNotFoundException("KYC document not found"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new UserNotFoundException("Reviewer not found"));

        document.setStatus(KycDocument.VerificationStatus.REJECTED);
        document.setReviewedBy(reviewer);
        document.setReviewedAt(LocalDateTime.now());
        document.setRejectionReason(rejectionReason);

        kycDocumentRepository.save(document);

        // Update user KYC status
        User user = document.getUser();
        user.setKycStatus(User.KycStatus.REJECTED);
        userRepository.save(user);

        log.info("KYC document rejected for user: {}", user.getUserId());
    }

    public List<KycDocument> getUserKycDocuments(UUID userId) {
        return kycDocumentRepository.findByUserUserId(userId);
    }

    public KycDocument getKycDocument(UUID kycDocId) {
        return kycDocumentRepository.findById(kycDocId)
                .orElseThrow(() -> new KycDocumentNotFoundException("KYC document not found"));
    }

    public List<KycDocument> getPendingKycDocuments() {
        return kycDocumentRepository.findPendingKycDocuments();
    }

    private String uploadFile(MultipartFile file, UUID userId, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fileType + " file is required");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File must be JPG or PNG format");
        }

        // Generate S3 key
        String fileName = String.format("kyc/%s/%s-%s-%s",
                userId, fileType, System.currentTimeMillis(), file.getOriginalFilename());

        // Upload to S3
        return s3Service.uploadFile(file, fileName);
    }

    public boolean hasApprovedKyc(UUID userId) {
        return kycDocumentRepository.hasApprovedKycDocument(userId);
    }

    public void updateKycLevel(UUID userId, Integer kycLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setKycLevel(kycLevel);
        userRepository.save(user);
        log.info("KYC level updated to {} for user: {}", kycLevel, userId);
    }
}
