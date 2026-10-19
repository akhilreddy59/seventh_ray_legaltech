package com.seventhray.contractmanager.service;

import com.seventhray.contractmanager.model.Contract;
import com.seventhray.contractmanager.model.ContractStatus;
import com.seventhray.contractmanager.repository.ContractRepository;
import com.seventhray.contractmanager.web.ContractNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {
    private final ContractRepository repository;
    private final DocumentTextExtractor extractor;
    private final SupabaseStorageService storage;

    public ContractService(ContractRepository repository, DocumentTextExtractor extractor, SupabaseStorageService storage) {
        this.repository = repository;
        this.extractor = extractor;
        this.storage = storage;
    }

    public List<Contract> list(UUID ownerId) {
        return repository.findAll(ownerId);
    }

    public Contract get(UUID id, UUID ownerId) {
        return repository.findById(id, ownerId).orElseThrow(() -> new ContractNotFoundException(id));
    }

    public Contract create(String title, MultipartFile file, UUID ownerId) {
        if (file == null || file.isEmpty()) throw new com.seventhray.contractmanager.web.InvalidUploadException("Choose a PDF or DOCX file to upload.");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (fileName.length() > 255) throw new com.seventhray.contractmanager.web.InvalidUploadException("The original filename must be 255 characters or fewer.");
        if (file.getSize() > 10 * 1024 * 1024) throw new com.seventhray.contractmanager.web.InvalidUploadException("File exceeds the 10 MB limit.");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new com.seventhray.contractmanager.web.InvalidUploadException("The uploaded file could not be read.", ex);
        }
        DocumentTextExtractor.ExtractedDocument document = extractor.extract(fileName, file.getContentType(), bytes);
        String contractTitle = title == null || title.isBlank() ? titleFrom(fileName) : title.trim();
        if (contractTitle.isBlank() || contractTitle.length() > 200) throw new com.seventhray.contractmanager.web.InvalidUploadException("Title must be between 1 and 200 characters.");

        UUID id = UUID.randomUUID();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        String storagePath = ownerId + "/contracts/" + id + "." + extension;
        storage.upload(storagePath, document.contentType(), bytes);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Contract contract = new Contract(id, contractTitle, fileName, storagePath, ContractStatus.DRAFT, document.text(), now, now);
        try {
            return repository.insert(contract, ownerId);
        } catch (RuntimeException persistenceError) {
            try {
                storage.delete(storagePath);
            } catch (RuntimeException cleanupError) {
                persistenceError.addSuppressed(cleanupError);
            }
            throw persistenceError;
        }
    }

    @Transactional
    public Contract advance(UUID id, UUID ownerId) {
        ContractStatus current = repository.findStatusForUpdate(id, ownerId);
        if (current == null) throw new ContractNotFoundException(id);
        ContractStatus next = current.next();
        repository.updateStatus(id, ownerId, next);
        return get(id, ownerId);
    }

    private static String titleFrom(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }
}
