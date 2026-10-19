package com.seventhray.contractmanager.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContractSummaryResponse(UUID id, String title, String fileName, ContractStatus status, OffsetDateTime createdAt) {
    public static ContractSummaryResponse from(Contract contract) {
        return new ContractSummaryResponse(contract.id(), contract.title(), contract.fileName(), contract.status(), contract.createdAt());
    }
}
