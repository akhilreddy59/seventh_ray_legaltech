package com.seventhray.contractmanager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionAnswerServiceTest {
    private final QuestionAnswerService service = new QuestionAnswerService();

    @Test
    void returnsTheSentenceWithMostQuestionKeywords() {
        assertEquals("Payment is due within thirty days.", service.answer(
                "The contract may be renewed. Payment is due within thirty days.", "When is payment due?"));
    }

    @Test
    void reportsWhenTheDocumentHasNoMatchingPassage() {
        assertEquals("I could not find an answer in this document.", service.answer(
                "Payment is due within thirty days.", "Who is the supplier?"));
    }
}
