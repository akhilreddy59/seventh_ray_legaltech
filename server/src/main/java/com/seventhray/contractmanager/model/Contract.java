package com.seventhray.contractmanager.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Contract(
        UUID id,
        String title,
        String fileName,
        String storagePath,
        ContractStatus status,
        String extractedText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
