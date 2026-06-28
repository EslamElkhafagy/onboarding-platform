package com.onboarding.service;

import com.onboarding.config.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts raw text from stored PDF/DOCX files. Format is chosen by MIME type with a
 * fallback to the filename extension, mirroring the validation in DocumentService.
 */
@Service
public class TextExtractionService {

    private static final String PDF_MIME = "application/pdf";
    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public String extract(Path path, String mimeType, String filename) {
        try {
            if (isPdf(mimeType, filename)) {
                return extractPdf(path);
            }
            if (isDocx(mimeType, filename)) {
                return extractDocx(path);
            }
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_TYPE",
                    "Cannot extract text from this file type");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "EXTRACTION_FAILED",
                    "Failed to extract text: " + e.getMessage());
        }
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             XWPFDocument docx = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
            return extractor.getText();
        }
    }

    private boolean isPdf(String mimeType, String filename) {
        return PDF_MIME.equals(mimeType) || endsWith(filename, ".pdf");
    }

    private boolean isDocx(String mimeType, String filename) {
        return DOCX_MIME.equals(mimeType) || endsWith(filename, ".docx");
    }

    private boolean endsWith(String filename, String ext) {
        return filename != null && filename.toLowerCase().endsWith(ext);
    }
}
