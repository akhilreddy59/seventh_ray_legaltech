package com.seventhray.contractmanager.service;

import com.seventhray.contractmanager.web.InvalidUploadException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@Service
public class DocumentTextExtractor {
    public ExtractedDocument extract(String originalName, String contentType, byte[] data) {
        if (data == null || data.length == 0) throw new InvalidUploadException("The uploaded document is empty.");
        String name = originalName == null ? "" : originalName;
        String lowerName = name.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf") && startsWith(data, "%PDF-")) {
                validateContentType(contentType, "application/pdf");
                try (var document = Loader.loadPDF(data)) {
                    return new ExtractedDocument("application/pdf", new PDFTextStripper().getText(document).trim());
                }
            }
            if (lowerName.endsWith(".docx") && data.length >= 4 && data[0] == 'P' && data[1] == 'K') {
                validateContentType(contentType, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                try (var document = new XWPFDocument(new ByteArrayInputStream(data)); var extractor = new XWPFWordExtractor(document)) {
                    return new ExtractedDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document", extractor.getText().trim());
                }
            }
            throw new InvalidUploadException("Upload a valid PDF or DOCX file.");
        } catch (InvalidUploadException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new InvalidUploadException("This document could not be read. Please upload a valid PDF or DOCX file.", ex);
        }
    }

    private static boolean startsWith(byte[] data, String signature) {
        if (data.length < signature.length()) return false;
        for (int i = 0; i < signature.length(); i++) if (data[i] != signature.charAt(i)) return false;
        return true;
    }

    private static void validateContentType(String contentType, String expected) {
        String detected = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        if (!detected.isBlank() && !detected.equalsIgnoreCase("application/octet-stream") && !detected.equalsIgnoreCase(expected)) {
            throw new InvalidUploadException("The document's file type does not match its contents.");
        }
    }

    public record ExtractedDocument(String contentType, String text) {}
}
