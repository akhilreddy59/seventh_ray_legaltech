package com.seventhray.contractmanager.model;

import com.seventhray.contractmanager.web.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractStatusTest {
    @Test
    void allowsOnlyForwardWorkflowSteps() {
        assertEquals(ContractStatus.REVIEW, ContractStatus.DRAFT.next());
        assertEquals(ContractStatus.APPROVED, ContractStatus.REVIEW.next());
        assertThrows(InvalidStatusTransitionException.class, ContractStatus.APPROVED::next);
    }
}
