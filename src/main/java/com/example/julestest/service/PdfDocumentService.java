package com.example.julestest.service;

import com.example.julestest.domain.PdfDocument;
import com.example.julestest.repository.PdfDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat; // Preferred for Java 17+
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PdfDocumentService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private static final String HASH_ALGORITHM = "SHA-256";

    public PdfDocument storePdfFile(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        String fileName = file.getOriginalFilename();
        String hashCode;

        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashedBytes = digest.digest();
            hashCode = HexFormat.of().formatHex(hashedBytes); // Using Java 17's HexFormat
        }

        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.setFileName(fileName);
        pdfDocument.setHashCode(hashCode);
        pdfDocument.setHashAlgorithm(HASH_ALGORITHM);
        pdfDocument.setUploadTimestamp(LocalDateTime.now());

        return pdfDocumentRepository.save(pdfDocument);
    }

    // Add these new methods:
    public List<PdfDocument> getAllPdfDocuments() {
        return pdfDocumentRepository.findAll();
    }

    public Optional<PdfDocument> getPdfDocumentById(Long id) {
        return pdfDocumentRepository.findById(id);
    }

    public PdfDocument updatePdfDocumentFileName(Long id, String newFileName) {
        PdfDocument document = pdfDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PdfDocument not found with id " + id)); // Or a custom exception
        document.setFileName(newFileName);
        // Potentially update a 'lastMetadataUpdateTimestamp' field if it existed
        return pdfDocumentRepository.save(document);
    }

    public void deletePdfDocument(Long id) {
        if (!pdfDocumentRepository.existsById(id)) {
            throw new RuntimeException("PdfDocument not found with id " + id); // Or a custom exception
        }
        pdfDocumentRepository.deleteById(id);
    }
}
