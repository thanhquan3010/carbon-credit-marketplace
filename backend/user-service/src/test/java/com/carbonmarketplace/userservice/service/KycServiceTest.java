package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.KycDocumentRequest;
import com.carbonmarketplace.userservice.entity.KycDocument;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.KycDocumentNotFoundException;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.repository.KycDocumentRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycService Unit Tests")
class KycServiceTest {

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private KycService kycService;

    private User testUser;
    private User testReviewer;
    private UUID testUserId;
    private UUID reviewerId;
    private UUID kycDocId;
    private KycDocument testKycDocument;
    private KycDocumentRequest kycRequest;
    private MultipartFile idFrontFile;
    private MultipartFile selfieFile;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        kycDocId = UUID.randomUUID();

        testUser = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .fullName("Test User")
                .role(User.UserRole.EVOWNER)
                .kycStatus(User.KycStatus.PENDING)
                .kycLevel(0)
                .build();

        testReviewer = User.builder()
                .userId(reviewerId)
                .email("reviewer@example.com")
                .fullName("Test Reviewer")
                .role(User.UserRole.VERIFIER)
                .build();

        testKycDocument = KycDocument.builder()
                .kycDocId(kycDocId)
                .user(testUser)
                .kycLevel(1)
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("AB123456")
                .idFrontUrl("https://s3.com/id-front.jpg")
                .selfieUrl("https://s3.com/selfie.jpg")
                .status(KycDocument.VerificationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        // Mock MultipartFile
        idFrontFile = mock(MultipartFile.class);
        selfieFile = mock(MultipartFile.class);

        kycRequest = KycDocumentRequest.builder()
                .kycLevel(1)
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("AB123456")
                .idFrontFile(idFrontFile)
                .selfieFile(selfieFile)
                .build();
    }

    // ===== Positive Test Cases =====

    @Test
    @DisplayName("submitKycDocument - Should submit KYC document successfully")
    void submitKycDocument_ShouldSubmitSuccessfully_WithValidData() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());

        when(idFrontFile.isEmpty()).thenReturn(false);
        when(idFrontFile.getSize()).thenReturn(1024L * 1024L); // 1MB
        when(idFrontFile.getContentType()).thenReturn("image/jpeg");
        when(idFrontFile.getOriginalFilename()).thenReturn("id-front.jpg");

        when(selfieFile.isEmpty()).thenReturn(false);
        when(selfieFile.getSize()).thenReturn(1024L * 1024L); // 1MB
        when(selfieFile.getContentType()).thenReturn("image/png");
        when(selfieFile.getOriginalFilename()).thenReturn("selfie.png");

        when(s3Service.uploadFile(any(MultipartFile.class), anyString())).thenReturn("https://s3.com/file.jpg");
        when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testKycDocument);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        KycDocument result = kycService.submitKycDocument(testUserId, kycRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testKycDocument.getKycDocId(), result.getKycDocId());
        verify(userRepository, times(1)).findById(testUserId);
        verify(s3Service, times(2)).uploadFile(any(MultipartFile.class), anyString());
        verify(kycDocumentRepository, times(1)).save(any(KycDocument.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("submitKycDocument - Should submit with optional files")
    void submitKycDocument_ShouldSubmitSuccessfully_WithOptionalFiles() {
        // Arrange
        MultipartFile idBackFile = mock(MultipartFile.class);
        MultipartFile addressProofFile = mock(MultipartFile.class);
        kycRequest.setIdBackFile(idBackFile);
        kycRequest.setAddressProofFile(addressProofFile);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());

        // Setup all file mocks
        when(idFrontFile.isEmpty()).thenReturn(false);
        when(idFrontFile.getSize()).thenReturn(1024L * 1024L);
        when(idFrontFile.getContentType()).thenReturn("image/jpeg");
        when(idFrontFile.getOriginalFilename()).thenReturn("id-front.jpg");

        when(idBackFile.isEmpty()).thenReturn(false);
        when(idBackFile.getSize()).thenReturn(1024L * 1024L);
        when(idBackFile.getContentType()).thenReturn("image/jpeg");
        when(idBackFile.getOriginalFilename()).thenReturn("id-back.jpg");

        when(selfieFile.isEmpty()).thenReturn(false);
        when(selfieFile.getSize()).thenReturn(1024L * 1024L);
        when(selfieFile.getContentType()).thenReturn("image/png");
        when(selfieFile.getOriginalFilename()).thenReturn("selfie.png");

        when(addressProofFile.isEmpty()).thenReturn(false);
        when(addressProofFile.getSize()).thenReturn(1024L * 1024L);
        when(addressProofFile.getContentType()).thenReturn("image/jpeg");
        when(addressProofFile.getOriginalFilename()).thenReturn("address.jpg");

        when(s3Service.uploadFile(any(MultipartFile.class), anyString())).thenReturn("https://s3.com/file.jpg");
        when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(testKycDocument);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        KycDocument result = kycService.submitKycDocument(testUserId, kycRequest);

        // Assert
        assertNotNull(result);
        verify(s3Service, times(4)).uploadFile(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("approveKycDocument - Should approve document successfully")
    void approveKycDocument_ShouldApproveSuccessfully() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.of(testKycDocument));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(testReviewer));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            assertEquals(KycDocument.VerificationStatus.APPROVED, doc.getStatus());
            assertNotNull(doc.getReviewedAt());
            assertNotNull(doc.getExpiresAt());
            return doc;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(User.KycStatus.APPROVED, user.getKycStatus());
            assertEquals(1, user.getKycLevel());
            return user;
        });

        // Act
        kycService.approveKycDocument(kycDocId, reviewerId, 1);

        // Assert
        verify(kycDocumentRepository, times(1)).save(any(KycDocument.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("rejectKycDocument - Should reject document successfully")
    void rejectKycDocument_ShouldRejectSuccessfully() {
        // Arrange
        String rejectionReason = "Document is blurry";
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.of(testKycDocument));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(testReviewer));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            assertEquals(KycDocument.VerificationStatus.REJECTED, doc.getStatus());
            assertEquals(rejectionReason, doc.getRejectionReason());
            assertNotNull(doc.getReviewedAt());
            return doc;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(User.KycStatus.REJECTED, user.getKycStatus());
            return user;
        });

        // Act
        kycService.rejectKycDocument(kycDocId, reviewerId, rejectionReason);

        // Assert
        verify(kycDocumentRepository, times(1)).save(any(KycDocument.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("getUserKycDocuments - Should return user's KYC documents")
    void getUserKycDocuments_ShouldReturnDocuments() {
        // Arrange
        List<KycDocument> documents = Arrays.asList(testKycDocument);
        when(kycDocumentRepository.findByUserUserId(testUserId)).thenReturn(documents);

        // Act
        List<KycDocument> result = kycService.getUserKycDocuments(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(kycDocumentRepository, times(1)).findByUserUserId(testUserId);
    }

    @Test
    @DisplayName("getKycDocument - Should return KYC document by ID")
    void getKycDocument_ShouldReturnDocument() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.of(testKycDocument));

        // Act
        KycDocument result = kycService.getKycDocument(kycDocId);

        // Assert
        assertNotNull(result);
        assertEquals(kycDocId, result.getKycDocId());
        verify(kycDocumentRepository, times(1)).findById(kycDocId);
    }

    @Test
    @DisplayName("getPendingKycDocuments - Should return pending documents")
    void getPendingKycDocuments_ShouldReturnPendingDocuments() {
        // Arrange
        List<KycDocument> documents = Arrays.asList(testKycDocument);
        when(kycDocumentRepository.findPendingKycDocuments()).thenReturn(documents);

        // Act
        List<KycDocument> result = kycService.getPendingKycDocuments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(kycDocumentRepository, times(1)).findPendingKycDocuments();
    }

    @Test
    @DisplayName("hasApprovedKyc - Should return true when user has approved KYC")
    void hasApprovedKyc_ShouldReturnTrue_WhenApproved() {
        // Arrange
        when(kycDocumentRepository.hasApprovedKycDocument(testUserId)).thenReturn(true);

        // Act
        boolean result = kycService.hasApprovedKyc(testUserId);

        // Assert
        assertTrue(result);
        verify(kycDocumentRepository, times(1)).hasApprovedKycDocument(testUserId);
    }

    @Test
    @DisplayName("updateKycLevel - Should update KYC level successfully")
    void updateKycLevel_ShouldUpdateSuccessfully() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(2, user.getKycLevel());
            return user;
        });

        // Act
        kycService.updateKycLevel(testUserId, 2);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ===== Negative Test Cases =====

    @Test
    @DisplayName("submitKycDocument - Should throw exception when user not found")
    void submitKycDocument_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> kycService.submitKycDocument(testUserId, kycRequest));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception when KYC already approved")
    void submitKycDocument_ShouldThrowException_WhenAlreadyApproved() {
        // Arrange
        KycDocument approvedDoc = KycDocument.builder()
                .kycLevel(1)
                .status(KycDocument.VerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.of(approvedDoc));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("already approved"));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception when file is null")
    void submitKycDocument_ShouldThrowException_WhenFileIsNull() {
        // Arrange
        kycRequest.setIdFrontFile(null);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("required"));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception when file is empty")
    void submitKycDocument_ShouldThrowException_WhenFileIsEmpty() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());
        when(idFrontFile.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("required"));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception when file size exceeds limit")
    void submitKycDocument_ShouldThrowException_WhenFileSizeExceeds() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());
        when(idFrontFile.isEmpty()).thenReturn(false);
        when(idFrontFile.getSize()).thenReturn(6L * 1024L * 1024L); // 6MB - exceeds 5MB limit

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("5MB"));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception with invalid file type")
    void submitKycDocument_ShouldThrowException_WithInvalidFileType() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());
        when(idFrontFile.isEmpty()).thenReturn(false);
        when(idFrontFile.getSize()).thenReturn(1024L * 1024L);
        when(idFrontFile.getContentType()).thenReturn("application/pdf"); // Invalid type

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("JPG or PNG"));
    }

    @Test
    @DisplayName("submitKycDocument - Should throw exception when content type is null")
    void submitKycDocument_ShouldThrowException_WhenContentTypeIsNull() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.findByUserIdAndKycLevel(testUserId, 1)).thenReturn(Optional.empty());
        when(idFrontFile.isEmpty()).thenReturn(false);
        when(idFrontFile.getSize()).thenReturn(1024L * 1024L);
        when(idFrontFile.getContentType()).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> kycService.submitKycDocument(testUserId, kycRequest));
        assertTrue(exception.getMessage().contains("JPG or PNG"));
    }

    @Test
    @DisplayName("approveKycDocument - Should throw exception when document not found")
    void approveKycDocument_ShouldThrowException_WhenDocumentNotFound() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(KycDocumentNotFoundException.class,
                () -> kycService.approveKycDocument(kycDocId, reviewerId, 1));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("approveKycDocument - Should throw exception when reviewer not found")
    void approveKycDocument_ShouldThrowException_WhenReviewerNotFound() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.of(testKycDocument));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> kycService.approveKycDocument(kycDocId, reviewerId, 1));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("rejectKycDocument - Should throw exception when document not found")
    void rejectKycDocument_ShouldThrowException_WhenDocumentNotFound() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(KycDocumentNotFoundException.class,
                () -> kycService.rejectKycDocument(kycDocId, reviewerId, "Reason"));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("rejectKycDocument - Should throw exception when reviewer not found")
    void rejectKycDocument_ShouldThrowException_WhenReviewerNotFound() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.of(testKycDocument));
        when(userRepository.findById(reviewerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> kycService.rejectKycDocument(kycDocId, reviewerId, "Reason"));
        verify(kycDocumentRepository, never()).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("getKycDocument - Should throw exception when document not found")
    void getKycDocument_ShouldThrowException_WhenDocumentNotFound() {
        // Arrange
        when(kycDocumentRepository.findById(kycDocId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(KycDocumentNotFoundException.class, () -> kycService.getKycDocument(kycDocId));
        verify(kycDocumentRepository, times(1)).findById(kycDocId);
    }

    @Test
    @DisplayName("updateKycLevel - Should throw exception when user not found")
    void updateKycLevel_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> kycService.updateKycLevel(testUserId, 2));
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("getUserKycDocuments - Should return empty list when no documents")
    void getUserKycDocuments_ShouldReturnEmptyList_WhenNoDocuments() {
        // Arrange
        when(kycDocumentRepository.findByUserUserId(testUserId)).thenReturn(Arrays.asList());

        // Act
        List<KycDocument> result = kycService.getUserKycDocuments(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("getPendingKycDocuments - Should return empty list when no pending documents")
    void getPendingKycDocuments_ShouldReturnEmptyList_WhenNoPending() {
        // Arrange
        when(kycDocumentRepository.findPendingKycDocuments()).thenReturn(Arrays.asList());

        // Act
        List<KycDocument> result = kycService.getPendingKycDocuments();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("hasApprovedKyc - Should return false when no approved KYC")
    void hasApprovedKyc_ShouldReturnFalse_WhenNoApproved() {
        // Arrange
        when(kycDocumentRepository.hasApprovedKycDocument(testUserId)).thenReturn(false);

        // Act
        boolean result = kycService.hasApprovedKyc(testUserId);

        // Assert
        assertFalse(result);
    }
}
