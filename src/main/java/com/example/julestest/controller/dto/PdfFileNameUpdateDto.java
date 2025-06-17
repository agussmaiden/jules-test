package com.example.julestest.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank; // For validation

@Data
public class PdfFileNameUpdateDto {

    @NotBlank(message = "fileName cannot be blank")
    private String fileName;
}
