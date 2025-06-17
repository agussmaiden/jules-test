package com.example.julestest.repository;

import com.example.julestest.domain.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    // Basic CRUD methods are inherited from JpaRepository
    // Custom query methods can be added here if needed later
}
