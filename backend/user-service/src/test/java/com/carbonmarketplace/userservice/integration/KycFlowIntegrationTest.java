package com.carbonmarketplace.userservice.integration;

import com.carbonmarketplace.userservice.dto.request.KycDocumentRequest;
import com.carbonmarketplace.userservice.entity.KycDocument;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.repository.KycDocumentRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import com.carbonmarketplace.userservice.service.KycService;
import com.carbonmarketplace.userservice.service.S3Service;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for KYC submission and verification flow:
 * - KYC document submission
 * - Document approval flow
 * - Document rejection flow
 * - Multi-level KYC verification
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("KYC Flow Integration Tests")
class KycFlowIntegrationTest {

    @Autowired
    private KycService kycService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private S3Service s3Service;

    private static User testDriver;
    private static User testVerifier;
    private static UUID driverId;
    private static UUID verifierId;
    private static UUID kycDocId;

    @BeforeAll
    static void setupUsers(@Autowired UserRepository userRepository,
            @Autowired PasswordEncoder passwordEncoder) {
        // Create test driver
        testDriver = User.builder()
                .email("kyc.driver." + System.currentTimeMillis() + "@example.com")
                .phone("+1111111111")
                .fullName("KYC Test Driver")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(User.UserRole.EVOWNER)
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
        testDriver = userRepository.save(testDriver);
        driverId = testDriver.getUserId();

        // Create test verifier
        testVerifier = User.builder()
                .email("kyc.verifier." + System.currentTimeMillis() + "@example.com")
                .fullName("KYC Test Verifier")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(User.UserRole.VERIFIER)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        testVerifier = userRepository.save(testVerifier);
        verifierId = testVerifier.getUserId();
    }

    @BeforeEach
    void setUp() {
        // Mock S3 service to avoid actual file uploads
        when(s3Service.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("https://s3.amazonaws.com/bucket/file.jpg");
    }

    // ===== KYC Submission Tests =====

    @Test
    @Order(1)
    @DisplayName("KYC Submission - Should submit Level 1 KYC successfully")
    @Transactional
    void testSubmitLevel1Kyc() {
        // Arrange
        MockMultipartFile idFrontFile = new MockMultipartFile(
                "idFront", "id-front.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(1)
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("AB123456")
                .idFrontFile(idFrontFile)
                .selfieFile(selfieFile)
                .build();

        // Act
        KycDocument result = kycService.submitKycDocument(driverId, request);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getKycDocId());
        assertEquals(1, result.getKycLevel());
        assertEquals(KycDocument.IdType.PASSPORT, result.getIdType());
        assertEquals("AB123456", result.getIdNumber());
        assertEquals(KycDocument.VerificationStatus.PENDING, result.getStatus());
        assertNotNull(result.getIdFrontUrl());
        assertNotNull(result.getSelfieUrl());
        assertNotNull(result.getSubmittedAt());

        // Verify user status was updated
        User user = userRepository.findById(driverId).orElseThrow();
        assertEquals(User.KycStatus.PENDING, user.getKycStatus());
        assertNotNull(user.getKycSubmittedAt());

        // Save kycDocId for subsequent tests
        kycDocId = result.getKycDocId();
    }

    @Test
    @Order(2)
    @DisplayName("KYC Submission - Should submit with all optional documents")
    @Transactional
    void testSubmitKycWithAllDocuments() {
        // Arrange
        MockMultipartFile idFrontFile = new MockMultipartFile(
                "idFront", "id-front.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile idBackFile = new MockMultipartFile(
                "idBack", "id-back.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile addressProofFile = new MockMultipartFile(
                "addressProof", "address.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(2)
                .idType(KycDocument.IdType.DRIVER_LICENSE)
                .idNumber("DL987654")
                .idFrontFile(idFrontFile)
                .idBackFile(idBackFile)
                .selfieFile(selfieFile)
                .addressProofFile(addressProofFile)
                .build();

        // Act
        KycDocument result = kycService.submitKycDocument(driverId, request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getKycLevel());
        assertNotNull(result.getIdFrontUrl());
        assertNotNull(result.getIdBackUrl());
        assertNotNull(result.getSelfieUrl());
        assertNotNull(result.getAddressProofUrl());
    }

    @Test
    @Order(3)
    @DisplayName("KYC Submission - Should prevent resubmission of approved KYC")
    @Transactional
    void testPreventResubmissionOfApprovedKyc() {
        // Arrange - Approve the Level 1 KYC first
        kycService.approveKycDocument(kycDocId, verifierId, 1);

        MockMultipartFile idFrontFile = new MockMultipartFile(
                "idFront", "id-front.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(1) // Same level as approved
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("CD789012")
                .idFrontFile(idFrontFile)
                .selfieFile(selfieFile)
                .build();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> kycService.submitKycDocument(driverId, request));
        assertTrue(exception.getMessage().contains("already approved"));
    }

    @Test
    @Order(4)
    @DisplayName("KYC Submission - Should validate file size")
    @Transactional
    void testValidateFileSize() {
        // Arrange - Create a file exceeding 5MB limit
        byte[] largeData = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "idFront", "large.jpg", "image/jpeg", largeData);
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(3)
                .idType(KycDocument.IdType.NATIONAL_ID)
                .idNumber("ID123456")
                .idFrontFile(largeFile)
                .selfieFile(selfieFile)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(driverId, request));
        assertTrue(exception.getMessage().contains("5MB"));
    }

    @Test
    @Order(5)
    @DisplayName("KYC Submission - Should validate file type")
    @Transactional
    void testValidateFileType() {
        // Arrange - Create a PDF file (not allowed)
        MockMultipartFile pdfFile = new MockMultipartFile(
                "idFront", "document.pdf", "application/pdf", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(3)
                .idType(KycDocument.IdType.NATIONAL_ID)
                .idNumber("ID123456")
                .idFrontFile(pdfFile)
                .selfieFile(selfieFile)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(driverId, request));
        assertTrue(exception.getMessage().contains("JPG or PNG"));
    }

    // ===== KYC Approval Tests =====

    @Test
    @Order(6)
    @DisplayName("KYC Approval - Should get pending KYC documents")
    @Transactional
    void testGetPendingKycDocuments() {
        // Act
        List<KycDocument> pendingDocs = kycService.getPendingKycDocuments();

        // Assert
        assertNotNull(pendingDocs);
        assertTrue(pendingDocs.size() > 0);
        assertTrue(pendingDocs.stream()
                .allMatch(doc -> doc.getStatus() == KycDocument.VerificationStatus.PENDING));
    }

    @Test
    @Order(7)
    @DisplayName("KYC Approval - Verifier should approve KYC document")
    @Transactional
    void testApproveKycDocument() {
        // Arrange - Get a pending document
        List<KycDocument> pendingDocs = kycService.getPendingKycDocuments();
        KycDocument docToApprove = pendingDocs.stream()
                .filter(doc -> doc.getKycLevel() == 2)
                .findFirst()
                .orElseThrow();

        // Act
        kycService.approveKycDocument(docToApprove.getKycDocId(), verifierId, 2);

        // Assert - Verify document was approved
        KycDocument approvedDoc = kycService.getKycDocument(docToApprove.getKycDocId());
        assertEquals(KycDocument.VerificationStatus.APPROVED, approvedDoc.getStatus());
        assertNotNull(approvedDoc.getReviewedAt());
        assertNotNull(approvedDoc.getReviewedBy());
        assertNotNull(approvedDoc.getExpiresAt());
        assertEquals(verifierId, approvedDoc.getReviewedBy().getUserId());

        // Verify user status was updated
        User user = userRepository.findById(driverId).orElseThrow();
        assertEquals(User.KycStatus.APPROVED, user.getKycStatus());
        assertEquals(2, user.getKycLevel());
        assertNotNull(user.getKycApprovedAt());
    }

    @Test
    @Order(8)
    @DisplayName("KYC Approval - Should verify KYC expiration is set")
    @Transactional
    void testKycExpiration() {
        // Arrange - Get an approved document
        List<KycDocument> userDocs = kycService.getUserKycDocuments(driverId);
        KycDocument approvedDoc = userDocs.stream()
                .filter(doc -> doc.getStatus() == KycDocument.VerificationStatus.APPROVED)
                .findFirst()
                .orElseThrow();

        // Assert
        assertNotNull(approvedDoc.getExpiresAt());
        assertTrue(approvedDoc.getExpiresAt().isAfter(LocalDateTime.now()));
        // Should be approximately 1 year from review date
        assertTrue(approvedDoc.getExpiresAt().isAfter(LocalDateTime.now().plusMonths(11)));
    }

    // ===== KYC Rejection Tests =====

    @Test
    @Order(9)
    @DisplayName("KYC Rejection - Verifier should reject KYC document")
    @Transactional
    void testRejectKycDocument() {
        // Arrange - Submit a new KYC for rejection
        MockMultipartFile idFrontFile = new MockMultipartFile(
                "idFront", "id-front.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(3)
                .idType(KycDocument.IdType.NATIONAL_ID)
                .idNumber("ID999999")
                .idFrontFile(idFrontFile)
                .selfieFile(selfieFile)
                .build();

        KycDocument submittedDoc = kycService.submitKycDocument(driverId, request);
        String rejectionReason = "Document image is blurry and unreadable";

        // Act
        kycService.rejectKycDocument(submittedDoc.getKycDocId(), verifierId, rejectionReason);

        // Assert - Verify document was rejected
        KycDocument rejectedDoc = kycService.getKycDocument(submittedDoc.getKycDocId());
        assertEquals(KycDocument.VerificationStatus.REJECTED, rejectedDoc.getStatus());
        assertEquals(rejectionReason, rejectedDoc.getRejectionReason());
        assertNotNull(rejectedDoc.getReviewedAt());
        assertNotNull(rejectedDoc.getReviewedBy());
        assertEquals(verifierId, rejectedDoc.getReviewedBy().getUserId());

        // Verify user status was updated
        User user = userRepository.findById(driverId).orElseThrow();
        assertEquals(User.KycStatus.REJECTED, user.getKycStatus());
    }

    @Test
    @Order(10)
    @DisplayName("KYC Rejection - Should allow resubmission after rejection")
    @Transactional
    void testResubmitAfterRejection() {
        // Arrange - The previous test rejected level 3, now resubmit
        MockMultipartFile idFrontFile = new MockMultipartFile(
                "idFront", "id-front-improved.jpg", "image/jpeg", "test data".getBytes());
        MockMultipartFile selfieFile = new MockMultipartFile(
                "selfie", "selfie-improved.jpg", "image/jpeg", "test data".getBytes());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(3)
                .idType(KycDocument.IdType.NATIONAL_ID)
                .idNumber("ID999999")
                .idFrontFile(idFrontFile)
                .selfieFile(selfieFile)
                .build();

        // Act - Should succeed (resubmission allowed after rejection)
        KycDocument result = kycService.submitKycDocument(driverId, request);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getKycLevel());
        assertEquals(KycDocument.VerificationStatus.PENDING, result.getStatus());
    }

    // ===== Multi-level KYC Tests =====

    @Test
    @Order(11)
    @DisplayName("Multi-level KYC - Should support multiple KYC levels")
    @Transactional
    void testMultipleLevelKyc() {
        // Act - Get all KYC documents for the user
        List<KycDocument> userDocs = kycService.getUserKycDocuments(driverId);

        // Assert - Should have documents for multiple levels
        assertNotNull(userDocs);
        assertTrue(userDocs.size() >= 3); // Levels 1, 2, and 3

        // Verify different levels exist
        boolean hasLevel1 = userDocs.stream().anyMatch(doc -> doc.getKycLevel() == 1);
        boolean hasLevel2 = userDocs.stream().anyMatch(doc -> doc.getKycLevel() == 2);
        boolean hasLevel3 = userDocs.stream().anyMatch(doc -> doc.getKycLevel() == 3);

        assertTrue(hasLevel1);
        assertTrue(hasLevel2);
        assertTrue(hasLevel3);
    }

    @Test
    @Order(12)
    @DisplayName("Multi-level KYC - Should track KYC level progression")
    @Transactional
    void testKycLevelProgression() {
        // Arrange & Act - User currently at level 2 (approved earlier)
        User user = userRepository.findById(driverId).orElseThrow();

        // Assert
        assertEquals(2, user.getKycLevel());
        assertEquals(User.KycStatus.REJECTED, user.getKycStatus()); // Rejected due to level 3 rejection

        // Verify has approved KYC
        boolean hasApprovedKyc = kycService.hasApprovedKyc(driverId);
        assertTrue(hasApprovedKyc);
    }

    @Test
    @Order(13)
    @DisplayName("KYC Query - Should get user's KYC documents")
    @Transactional
    void testGetUserKycDocuments() {
        // Act
        List<KycDocument> userDocs = kycService.getUserKycDocuments(driverId);

        // Assert
        assertNotNull(userDocs);
        assertTrue(userDocs.size() > 0);
        assertTrue(userDocs.stream().allMatch(doc -> doc.getUser().getUserId().equals(driverId)));
    }

    @Test
    @Order(14)
    @DisplayName("KYC Query - Should get specific KYC document")
    @Transactional
    void testGetSpecificKycDocument() {
        // Act
        KycDocument document = kycService.getKycDocument(kycDocId);

        // Assert
        assertNotNull(document);
        assertEquals(kycDocId, document.getKycDocId());
        assertEquals(driverId, document.getUser().getUserId());
    }

    @Test
    @Order(15)
    @DisplayName("KYC Management - Should update KYC level manually")
    @Transactional
    void testUpdateKycLevel() {
        // Act
        kycService.updateKycLevel(driverId, 4);

        // Assert
        User user = userRepository.findById(driverId).orElseThrow();
        assertEquals(4, user.getKycLevel());
    }

    // ===== Complete KYC Journey Test =====

    @Test
    @Order(16)
    @DisplayName("Complete KYC Journey - From submission to approval")
    @Transactional
    void testCompleteKycJourney() {
        // Create a new user for complete journey
        User journeyUser = User.builder()
                .email("journey.kyc." + System.currentTimeMillis() + "@example.com")
                .fullName("Journey KYC User")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(User.UserRole.EVOWNER)
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        journeyUser = userRepository.save(journeyUser);
        UUID journeyUserId = journeyUser.getUserId();

        // Step 1: Submit Level 1 KYC
        MockMultipartFile idFront1 = new MockMultipartFile(
                "idFront", "id.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile selfie1 = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", "data".getBytes());

        KycDocumentRequest request1 = KycDocumentRequest.builder()
                .kycLevel(1)
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("JN123456")
                .idFrontFile(idFront1)
                .selfieFile(selfie1)
                .build();

        KycDocument doc1 = kycService.submitKycDocument(journeyUserId, request1);
        assertEquals(KycDocument.VerificationStatus.PENDING, doc1.getStatus());

        // Step 2: Approve Level 1
        kycService.approveKycDocument(doc1.getKycDocId(), verifierId, 1);
        journeyUser = userRepository.findById(journeyUserId).orElseThrow();
        assertEquals(1, journeyUser.getKycLevel());
        assertEquals(User.KycStatus.APPROVED, journeyUser.getKycStatus());

        // Step 3: Submit Level 2 KYC with address proof
        MockMultipartFile idFront2 = new MockMultipartFile(
                "idFront", "id2.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile selfie2 = new MockMultipartFile(
                "selfie", "selfie2.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile address = new MockMultipartFile(
                "address", "address.jpg", "image/jpeg", "data".getBytes());

        KycDocumentRequest request2 = KycDocumentRequest.builder()
                .kycLevel(2)
                .idType(KycDocument.IdType.DRIVER_LICENSE)
                .idNumber("DL789012")
                .idFrontFile(idFront2)
                .selfieFile(selfie2)
                .addressProofFile(address)
                .build();

        KycDocument doc2 = kycService.submitKycDocument(journeyUserId, request2);
        assertEquals(KycDocument.VerificationStatus.PENDING, doc2.getStatus());

        // Step 4: Approve Level 2
        kycService.approveKycDocument(doc2.getKycDocId(), verifierId, 2);
        journeyUser = userRepository.findById(journeyUserId).orElseThrow();
        assertEquals(2, journeyUser.getKycLevel());

        // Verify complete journey
        List<KycDocument> allDocs = kycService.getUserKycDocuments(journeyUserId);
        assertEquals(2, allDocs.size());
        assertEquals(2, allDocs.stream()
                .filter(doc -> doc.getStatus() == KycDocument.VerificationStatus.APPROVED)
                .count());
    }

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepository,
            @Autowired KycDocumentRepository kycDocumentRepository) {
        // Cleanup test data
        try {
            if (driverId != null) {
                kycDocumentRepository.findByUserUserId(driverId)
                        .forEach(kycDocumentRepository::delete);
                userRepository.deleteById(driverId);
            }
            if (verifierId != null) {
                userRepository.deleteById(verifierId);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
