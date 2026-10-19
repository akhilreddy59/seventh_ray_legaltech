package com.seventhray.contractmanager.web;

import com.seventhray.contractmanager.model.ContractResponse;
import com.seventhray.contractmanager.model.ContractSummaryResponse;
import com.seventhray.contractmanager.service.ContractService;
import com.seventhray.contractmanager.service.QuestionAnswerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contracts;
    private final QuestionAnswerService questions;

    public ContractController(ContractService contracts, QuestionAnswerService questions) {
        this.contracts = contracts;
        this.questions = questions;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@RequestParam(required = false) String title, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("contract", ContractResponse.from(contracts.create(title, file))));
    }

    @GetMapping
    public Map<String, ?> list() {
        return Map.of("contracts", contracts.list().stream().map(ContractSummaryResponse::from).toList());
    }

    @GetMapping("/{id}")
    public Map<String, ContractResponse> get(@PathVariable UUID id) {
        return Map.of("contract", ContractResponse.from(contracts.get(id)));
    }

    @PatchMapping("/{id}/status")
    public Map<String, ContractResponse> advance(@PathVariable UUID id) {
        return Map.of("contract", ContractResponse.from(contracts.advance(id)));
    }

    @PostMapping("/{id}/questions")
    public Map<String, String> ask(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return Map.of("answer", questions.answer(contracts.get(id).extractedText(), request.question().trim()));
    }

    public record QuestionRequest(@NotBlank @Size(max = 500) String question) {}
}
