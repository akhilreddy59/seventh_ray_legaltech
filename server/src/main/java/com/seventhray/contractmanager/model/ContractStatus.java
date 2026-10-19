package com.seventhray.contractmanager.model;

import com.seventhray.contractmanager.web.InvalidStatusTransitionException;

public enum ContractStatus {
    DRAFT, REVIEW, APPROVED;

    public ContractStatus next() {
        return switch (this) {
            case DRAFT -> REVIEW;
            case REVIEW -> APPROVED;
            case APPROVED -> throw new InvalidStatusTransitionException(this);
        };
    }
}
