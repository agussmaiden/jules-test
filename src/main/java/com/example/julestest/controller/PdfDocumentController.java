package com.example.julestest.controller;

import com.example.julestest.domain.PdfDocument;
import com.example.julestest.service.PdfDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;
import java.security.NoSuchAlgorithmException; // Added
import com.example.julestest.controller.dto.PdfFileNameUpdateDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfDocumentController {

    private final PdfDocumentService pdfDocumentService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdfFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "algorithm", required = false) String algorithm) { // Added algorithm parameter

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }
        // Simple validation for PDF content type (can be more robust)
        if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only PDF files are allowed");
        }

        try {
            // Pass the algorithm to the service method
            PdfDocument savedDocument = pdfDocumentService.storePdfFile(file, algorithm);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);
        } catch (IllegalArgumentException e) { // Catching specific exception from service for bad algorithm
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchAlgorithmException e) { // Should ideally not happen if service validates algorithms
            // Log this as it indicates an issue with algorithm list/logic
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error with hashing algorithm.");
        } catch (Exception e) {
            // Log the exception e.g., e.printStackTrace(); or use a logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Could not upload the file: " + e.getMessage());
        }
    }

    // Add these new endpoint methods:

    @GetMapping
    public ResponseEntity<List<PdfDocument>> getAllPdfDocuments() {
        List<PdfDocument> documents = pdfDocumentService.getAllPdfDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PdfDocument> getPdfDocumentById(@PathVariable Long id) {
        return pdfDocumentService.getPdfDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // For the update, we'll expect a simple DTO or Map for the fileName.
    // Let's use a Map<String, String> for simplicity for now, expecting {"fileName": "new_name.pdf"}
    // A DTO would be better practice for more complex updates.
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePdfFileName(@PathVariable Long id, @RequestBody @Valid PdfFileNameUpdateDto dto) { // Changed here
        String newFileName = dto.getFileName(); // Changed here
        // The @NotBlank on dto.fileName handles the empty/null check.
        try {
            PdfDocument updatedDocument = pdfDocumentService.updatePdfDocumentFileName(id, newFileName);
            return ResponseEntity.ok(updatedDocument);
        } catch (RuntimeException e) { // Catching generic RuntimeException from service
            if (e.getMessage() != null && e.getMessage().contains("not found")) { // Check for null on e.getMessage()
                return ResponseEntity.notFound().build();
            }
            // Log e here
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating document: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePdfDocument(@PathVariable Long id) {
        try {
            pdfDocumentService.deletePdfDocument(id);
            return ResponseEntity.noContent().build(); // 204 No Content is typical for successful DELETE
        } catch (RuntimeException e) { // Catching generic RuntimeException from service
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or a generic error body
        }
    }
}
