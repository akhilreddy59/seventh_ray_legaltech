package com.seventhray.contractmanager.web;

import com.seventhray.contractmanager.model.ContractStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(ContractStatus status) { super("A contract in " + status + " cannot move to another status."); }
}
