package com.seventhray.contractmanager.web;

import java.util.UUID;

public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException(UUID id) { super("Contract " + id + " was not found."); }
}
