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
    void storePdfFile_withEmptyFile_shouldProcessCorrectly() throws IOException, NoSuchAlgorithmException {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        MessageDigest digest = MessageDigest.getInstance(expectedHashAlgorithm); // expectedHashAlgorithm is a field
        byte[] hashedBytes = digest.digest(new byte[0]);
        String emptyContentExpectedHashCode = HexFormat.of().formatHex(hashedBytes);

        // Arrange for the save operation
        when(pdfDocumentRepository.save(any(PdfDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PdfDocument result = pdfDocumentService.storePdfFile(emptyFile);

        // Assert
        assertNotNull(result);
        assertEquals("empty.pdf", result.getFileName());
        assertEquals(emptyContentExpectedHashCode, result.getHashCode());
        assertEquals(expectedHashAlgorithm, result.getHashAlgorithm());
        assertNotNull(result.getUploadTimestamp());

        verify(pdfDocumentRepository, times(1)).save(any(PdfDocument.class));
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
