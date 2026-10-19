package com.seventhray.contractmanager.web;

import com.seventhray.contractmanager.config.CorsConfig;
import com.seventhray.contractmanager.model.Contract;
import com.seventhray.contractmanager.model.ContractStatus;
import com.seventhray.contractmanager.service.ContractService;
import com.seventhray.contractmanager.service.QuestionAnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ContractController.class, HealthController.class})
@Import(CorsConfig.class)
class ContractControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContractService contracts;
    @MockitoBean private QuestionAnswerService questions;
    private final UUID id = UUID.randomUUID();

    @Test
    void healthEndpointResponds() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void contractEndpointsAreAvailableWithoutLogin() throws Exception {
        when(contracts.list()).thenReturn(List.of());
        mvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contracts").isArray());
    }

    @Test
    void corsPreflightAllowsProductionFrontendAndRequiredApiMethodsAndHeaders() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/contracts")
                        .header(HttpHeaders.ORIGIN, "https://seventh-ray-legaltech.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://seventh-ray-legaltech.vercel.app"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("DELETE")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("authorization")));
    }

    @Test
    void unauthenticatedApiResponseIncludesCorsHeaders() throws Exception {
        when(contracts.list()).thenReturn(List.of());
        mvc.perform(get("/api/contracts")
                        .header(HttpHeaders.ORIGIN, "https://seventh-ray-legaltech.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://seventh-ray-legaltech.vercel.app"));
    }

    @Test
    void createsContractFromMultipartUpload() throws Exception {
        when(contracts.create(eq("Sample"), any())).thenReturn(contract(ContractStatus.DRAFT));
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "%PDF-test".getBytes());

        mvc.perform(multipart("/api/contracts").file(file).param("title", "Sample"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contract.file_name").value("sample.pdf"))
                .andExpect(jsonPath("$.contract.status").value("DRAFT"));
    }

    @Test
    void listsSummariesAndRetrievesDocumentText() throws Exception {
        when(contracts.list()).thenReturn(List.of(contract(ContractStatus.DRAFT)));
        when(contracts.get(id)).thenReturn(contract(ContractStatus.DRAFT));

        mvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contracts[0].file_name").value("sample.pdf"))
                .andExpect(jsonPath("$.contracts[0].extracted_text").doesNotExist());
        mvc.perform(get("/api/contracts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.extracted_text").value("Payment is due in thirty days."));
    }

    @Test
    void advancesStatusAndAnswersQuestions() throws Exception {
        when(contracts.advance(id)).thenReturn(contract(ContractStatus.REVIEW));
        when(contracts.get(id)).thenReturn(contract(ContractStatus.DRAFT));
        when(questions.answer(any(), eq("When is payment due?"))).thenReturn("Payment is due in thirty days.");

        mvc.perform(patch("/api/contracts/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.status").value("REVIEW"));
        mvc.perform(post("/api/contracts/{id}/questions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"When is payment due?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Payment is due in thirty days."));
    }

    @Test
    void mapsMissingContractsToNotFound() throws Exception {
        when(contracts.get(id)).thenThrow(new ContractNotFoundException(id));
        mvc.perform(get("/api/contracts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void mapsAnInvalidWorkflowStepToConflict() throws Exception {
        doThrow(new InvalidStatusTransitionException(ContractStatus.APPROVED)).when(contracts).advance(id);
        mvc.perform(patch("/api/contracts/{id}/status", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("A contract in APPROVED cannot move to another status."));
    }

    private Contract contract(ContractStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Contract(id, "Sample", "sample.pdf", "contracts/" + id + ".pdf", status,
                "Payment is due in thirty days.", now, now);
    }
}
