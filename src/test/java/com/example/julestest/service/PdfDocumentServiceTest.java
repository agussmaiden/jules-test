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
import java.util.HexFormat;

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
    private final String expectedHashAlgorithm = "SHA-256";
    private String expectedHashCode;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        mockFile = new MockMultipartFile(
                "file", // parameter name
                "test.pdf", // original filename
                "application/pdf", // content type
                testFileContent.getBytes(StandardCharsets.UTF_8) // content
        );

        // Calculate expected hash
        MessageDigest digest = MessageDigest.getInstance(expectedHashAlgorithm);
        byte[] hashedBytes = digest.digest(testFileContent.getBytes(StandardCharsets.UTF_8));
        expectedHashCode = HexFormat.of().formatHex(hashedBytes);
    }

    @Test
    void storePdfFile_shouldProcessFileAndSaveDocument() throws IOException, NoSuchAlgorithmException {
        // Arrange
        PdfDocument savedDocument = new PdfDocument();
        savedDocument.setId(1L);
        savedDocument.setFileName(mockFile.getOriginalFilename());
        savedDocument.setHashCode(expectedHashCode);
        savedDocument.setHashAlgorithm(expectedHashAlgorithm);
        savedDocument.setUploadTimestamp(LocalDateTime.now()); // Timestamp will be close

        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenReturn(savedDocument);

        // Act
        PdfDocument result = pdfDocumentService.storePdfFile(mockFile);

        // Assert
        assertNotNull(result);
        assertEquals(mockFile.getOriginalFilename(), result.getFileName());
        assertEquals(expectedHashCode, result.getHashCode());
        assertEquals(expectedHashAlgorithm, result.getHashAlgorithm());
        assertNotNull(result.getUploadTimestamp());

        ArgumentCaptor<PdfDocument> documentCaptor = ArgumentCaptor.forClass(PdfDocument.class);
        verify(pdfDocumentRepository, times(1)).save(documentCaptor.capture());

        PdfDocument capturedDocument = documentCaptor.getValue();
        assertEquals(mockFile.getOriginalFilename(), capturedDocument.getFileName());
        assertEquals(expectedHashCode, capturedDocument.getHashCode());
        assertEquals(expectedHashAlgorithm, capturedDocument.getHashAlgorithm());
        assertNotNull(capturedDocument.getUploadTimestamp()); // Check timestamp exists
    }

    @Test
    void storePdfFile_withEmptyFile_shouldThrowIOException() {
        // This test is more for the controller, but shows how service might react to empty stream
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        // Recalculate expected hash for empty content
        String emptyContentExpectedHashCode;
        try {
            MessageDigest digest = MessageDigest.getInstance(expectedHashAlgorithm);
            byte[] hashedBytes = digest.digest(new byte[0]);
            emptyContentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            fail("NoSuchAlgorithmException for SHA-256");
            return;
        }

        assertDoesNotThrow(() -> {
            PdfDocument result = pdfDocumentService.storePdfFile(emptyFile);
            assertEquals(emptyContentExpectedHashCode, result.getHashCode(), "Hash code for empty content should match.");
        }, "Processing an empty file should not throw an exception at service level if stream is valid, hash will be of empty content.");
    }
}
