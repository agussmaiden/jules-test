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

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfDocumentController {

    private final PdfDocumentService pdfDocumentService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdfFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }
        // Simple validation for PDF content type (can be more robust)
        if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only PDF files are allowed");
        }

        try {
            PdfDocument savedDocument = pdfDocumentService.storePdfFile(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);
        } catch (Exception e) {
            // Log the exception e.g., e.printStackTrace(); or use a logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Could not upload the file: " + e.getMessage());
        }
    }
}
