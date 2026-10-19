package com.seventhray.contractmanager.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContractResponse(
        UUID id,
        String title,
        String fileName,
        ContractStatus status,
        OffsetDateTime createdAt,
        String extractedText) {
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(contract.id(), contract.title(), contract.fileName(), contract.status(), contract.createdAt(), contract.extractedText());
    }
}
