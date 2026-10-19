package com.seventhray.contractmanager.web;

public class InvalidUploadException extends RuntimeException {
    public InvalidUploadException(String message) { super(message); }
    public InvalidUploadException(String message, Throwable cause) { super(message, cause); }
}
