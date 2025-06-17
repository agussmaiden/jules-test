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
import java.util.Arrays; // Added
import java.util.HexFormat; // Preferred for Java 17+
import java.util.HashSet; // Added
import java.util.List;
import java.util.Optional;
import java.util.Set; // Added

@Service
@RequiredArgsConstructor
public class PdfDocumentService {

    private final PdfDocumentRepository pdfDocumentRepository;
    // Define supported algorithms
    private static final Set<String> SUPPORTED_ALGORITHMS =
        new HashSet<>(Arrays.asList("MD5", "SHA-1", "SHA-256", "SHA-512"));
    private static final String DEFAULT_ALGORITHM = "SHA-256";

    public PdfDocument storePdfFile(MultipartFile file, String requestedAlgorithm) throws IOException, NoSuchAlgorithmException {
        String algorithmToUse;
        if (requestedAlgorithm == null || requestedAlgorithm.trim().isEmpty()) {
            algorithmToUse = DEFAULT_ALGORITHM;
        } else if (SUPPORTED_ALGORITHMS.contains(requestedAlgorithm.toUpperCase())) {
            algorithmToUse = requestedAlgorithm.toUpperCase();
        } else {
            throw new IllegalArgumentException("Unsupported hash algorithm: " + requestedAlgorithm +
                                               ". Supported algorithms are: " + SUPPORTED_ALGORITHMS);
        }

        String fileName = file.getOriginalFilename();
        String hashCode;

        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance(algorithmToUse); // Use validated algorithm
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashedBytes = digest.digest();
            hashCode = HexFormat.of().formatHex(hashedBytes);
        }

        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.setFileName(fileName);
        pdfDocument.setHashCode(hashCode);
        pdfDocument.setHashAlgorithm(algorithmToUse); // Store the actual algorithm used
        pdfDocument.setUploadTimestamp(LocalDateTime.now());

        return pdfDocumentRepository.save(pdfDocument);
    }

    // Other service methods (getAll, getById, etc.) remain unchanged.
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
