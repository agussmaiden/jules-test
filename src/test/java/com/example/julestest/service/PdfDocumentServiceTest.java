package com.example.julestest.service;

import com.example.julestest.domain.PdfDocument;
import com.example.julestest.repository.PdfDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile; // Spring's mock for MultipartFile

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays; // Added
import java.util.HexFormat;
import java.util.List;   // Added
import java.util.Optional; // Added

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfDocumentServiceTest {

    @Mock
    private PdfDocumentRepository pdfDocumentRepository;

    @InjectMocks
    private PdfDocumentService pdfDocumentService;

    private MockMultipartFile mockFile;
    private final String testFileContent = "This is a test PDF content.";
    // Removed: private final String expectedHashAlgorithm = "SHA-256";
    // Removed: private String expectedHashCode;

    @BeforeEach
    void setUp() { // Removed throws NoSuchAlgorithmException as hash is not calculated here anymore
        mockFile = new MockMultipartFile(
                "file", // parameter name
                "test.pdf", // original filename
                "application/pdf", // content type
                testFileContent.getBytes(StandardCharsets.UTF_8) // content
        );
        // Removed global expectedHashCode calculation
    }

    @Test
    void storePdfFile_shouldProcessFileAndSaveDocument_withSha256() throws IOException, NoSuchAlgorithmException {
        String algorithm = "SHA-256";
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest(testFileContent.getBytes(StandardCharsets.UTF_8));
        String currentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        // Arrange
        // The savedDocument can be simplified as the service method creates and populates it.
        // We only need to ensure the save method is called and returns something sensible.
        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> {
            PdfDocument docToSave = invocation.getArgument(0);
            // Simulate setting an ID upon saving
            if (docToSave.getId() == null) { // Check if ID is not set (typical for new entities)
                 // For test predictability, if we need to assert ID, we can set it here.
                 // However, the main focus is on hash and algorithm.
                 // Let's assume an ID is set by the database, so we don't need to mock its exact value here.
            }
            return docToSave;
        });

        // Act
        PdfDocument result = pdfDocumentService.storePdfFile(mockFile, algorithm);

        // Assert
        assertNotNull(result);
        assertEquals(mockFile.getOriginalFilename(), result.getFileName());
        assertEquals(currentExpectedHashCode, result.getHashCode());
        assertEquals(algorithm, result.getHashAlgorithm());
        assertNotNull(result.getUploadTimestamp());

        ArgumentCaptor<PdfDocument> documentCaptor = ArgumentCaptor.forClass(PdfDocument.class);
        verify(pdfDocumentRepository, times(1)).save(documentCaptor.capture());

        PdfDocument capturedDocument = documentCaptor.getValue();
        assertEquals(mockFile.getOriginalFilename(), capturedDocument.getFileName());
        assertEquals(currentExpectedHashCode, capturedDocument.getHashCode());
        assertEquals(algorithm, capturedDocument.getHashAlgorithm());
        assertNotNull(capturedDocument.getUploadTimestamp());
    }

    @Test
    void storePdfFile_withEmptyFile_shouldProcessCorrectly_withDefaultAlgorithm() throws IOException, NoSuchAlgorithmException {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        String defaultAlgorithm = "SHA-256"; // As defined in service

        MessageDigest digest = MessageDigest.getInstance(defaultAlgorithm);
        byte[] hashedBytes = digest.digest(new byte[0]);
        String emptyContentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PdfDocument result = pdfDocumentService.storePdfFile(emptyFile, null); // Test default by passing null

        // Assert
        assertNotNull(result);
        assertEquals("empty.pdf", result.getFileName());
        assertEquals(emptyContentExpectedHashCode, result.getHashCode());
        assertEquals(defaultAlgorithm, result.getHashAlgorithm()); // Verify default algorithm stored
        assertNotNull(result.getUploadTimestamp());
        verify(pdfDocumentRepository, times(1)).save(any(PdfDocument.class));
    }

    // New tests for algorithm handling:
    @Test
    void storePdfFile_withNullAlgorithm_shouldUseDefaultAlgorithm() throws IOException, NoSuchAlgorithmException {
        String defaultAlgorithm = "SHA-256"; // As defined in service
        MessageDigest digest = MessageDigest.getInstance(defaultAlgorithm);
        byte[] hashedBytes = digest.digest(testFileContent.getBytes(StandardCharsets.UTF_8));
        String currentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdfDocument result = pdfDocumentService.storePdfFile(mockFile, null); // Pass null for algorithm

        assertEquals(currentExpectedHashCode, result.getHashCode());
        assertEquals(defaultAlgorithm, result.getHashAlgorithm());
        verify(pdfDocumentRepository, times(1)).save(any(PdfDocument.class));
    }

    @Test
    void storePdfFile_withEmptyStringAlgorithm_shouldUseDefaultAlgorithm() throws IOException, NoSuchAlgorithmException {
        String defaultAlgorithm = "SHA-256"; // As defined in service
        MessageDigest digest = MessageDigest.getInstance(defaultAlgorithm);
        byte[] hashedBytes = digest.digest(testFileContent.getBytes(StandardCharsets.UTF_8));
        String currentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdfDocument result = pdfDocumentService.storePdfFile(mockFile, "   "); // Pass blank string for algorithm

        assertEquals(currentExpectedHashCode, result.getHashCode());
        assertEquals(defaultAlgorithm, result.getHashAlgorithm());
        verify(pdfDocumentRepository, times(1)).save(any(PdfDocument.class));
    }

    @Test
    void storePdfFile_withMD5Algorithm_shouldProcessCorrectly() throws IOException, NoSuchAlgorithmException {
        String algorithm = "MD5";
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest(testFileContent.getBytes(StandardCharsets.UTF_8));
        String currentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PdfDocument result = pdfDocumentService.storePdfFile(mockFile, algorithm);

        assertEquals(currentExpectedHashCode, result.getHashCode());
        assertEquals(algorithm, result.getHashAlgorithm());
        verify(pdfDocumentRepository, times(1)).save(any(PdfDocument.class));
    }

    @Test
    void storePdfFile_withUnsupportedAlgorithm_shouldThrowIllegalArgumentException() {
        String unsupportedAlgorithm = "SHA-3-NOT-SUPPORTED";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pdfDocumentService.storePdfFile(mockFile, unsupportedAlgorithm);
        });
        assertTrue(exception.getMessage().contains("Unsupported hash algorithm"));
        assertTrue(exception.getMessage().contains(unsupportedAlgorithm));
        verify(pdfDocumentRepository, never()).save(any(PdfDocument.class));
    }

    @Test
    void getAllPdfDocuments_shouldReturnListOfDocuments() {
        // Arrange
        PdfDocument doc1 = new PdfDocument(1L, "file1.pdf", "hash1", "SHA-256", LocalDateTime.now());
        PdfDocument doc2 = new PdfDocument(2L, "file2.pdf", "hash2", "SHA-256", LocalDateTime.now());
        when(pdfDocumentRepository.findAll()).thenReturn(Arrays.asList(doc1, doc2));

        // Act
        List<PdfDocument> results = pdfDocumentService.getAllPdfDocuments();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(pdfDocumentRepository, times(1)).findAll();
    }

    @Test
    void getPdfDocumentById_whenDocumentExists_shouldReturnDocument() {
        // Arrange
        Long docId = 1L;
        PdfDocument document = new PdfDocument(docId, "file1.pdf", "hash1", "SHA-256", LocalDateTime.now());
        when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.of(document));

        // Act
        Optional<PdfDocument> result = pdfDocumentService.getPdfDocumentById(docId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(document.getFileName(), result.get().getFileName());
        verify(pdfDocumentRepository, times(1)).findById(docId);
    }

    @Test
    void getPdfDocumentById_whenDocumentDoesNotExist_shouldReturnEmptyOptional() {
        // Arrange
        Long docId = 1L;
        when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act
        Optional<PdfDocument> result = pdfDocumentService.getPdfDocumentById(docId);

        // Assert
        assertFalse(result.isPresent());
        verify(pdfDocumentRepository, times(1)).findById(docId);
    }

    @Test
    void updatePdfDocumentFileName_whenDocumentExists_shouldUpdateAndReturnDocument() {
        // Arrange
        Long docId = 1L;
        String oldFileName = "oldName.pdf";
        String newFileName = "newName.pdf";
        PdfDocument existingDocument = new PdfDocument(docId, oldFileName, "hash1", "SHA-256", LocalDateTime.now());

        // When findById is called, return the existing document
        when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.of(existingDocument));
        // When save is called, capture the argument and return it (or a new object simulating save)
        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PdfDocument updatedDocument = pdfDocumentService.updatePdfDocumentFileName(docId, newFileName);

        // Assert
        assertNotNull(updatedDocument);
        assertEquals(newFileName, updatedDocument.getFileName()); // Verify filename is updated
        assertEquals(existingDocument.getHashCode(), updatedDocument.getHashCode()); // Ensure other fields are unchanged

        ArgumentCaptor<PdfDocument> documentCaptor = ArgumentCaptor.forClass(PdfDocument.class);
        verify(pdfDocumentRepository, times(1)).findById(docId);
        verify(pdfDocumentRepository, times(1)).save(documentCaptor.capture());
        assertEquals(newFileName, documentCaptor.getValue().getFileName());
    }

    @Test
    void updatePdfDocumentFileName_whenDocumentDoesNotExist_shouldThrowException() {
        // Arrange
        Long docId = 1L;
        String newFileName = "newName.pdf";
        when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pdfDocumentService.updatePdfDocumentFileName(docId, newFileName);
        });
        assertEquals("PdfDocument not found with id " + docId, exception.getMessage());
        verify(pdfDocumentRepository, times(1)).findById(docId);
        verify(pdfDocumentRepository, never()).save(any(PdfDocument.class));
    }

    @Test
    void deletePdfDocument_whenDocumentExists_shouldCallDeleteById() {
        // Arrange
        Long docId = 1L;
        when(pdfDocumentRepository.existsById(docId)).thenReturn(true);
        // doNothing().when(pdfDocumentRepository).deleteById(docId); // for void methods

        // Act
        assertDoesNotThrow(() -> pdfDocumentService.deletePdfDocument(docId));

        // Assert
        verify(pdfDocumentRepository, times(1)).existsById(docId);
        verify(pdfDocumentRepository, times(1)).deleteById(docId);
    }

    @Test
    void deletePdfDocument_whenDocumentDoesNotExist_shouldThrowException() {
        // Arrange
        Long docId = 1L;
        when(pdfDocumentRepository.existsById(docId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pdfDocumentService.deletePdfDocument(docId);
        });
        assertEquals("PdfDocument not found with id " + docId, exception.getMessage());
        verify(pdfDocumentRepository, times(1)).existsById(docId);
        verify(pdfDocumentRepository, never()).deleteById(docId);
    }
}
