package com.onboarding.service;

import com.onboarding.config.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploaded files on the local filesystem under a configured base directory.
 * Files are namespaced by company to keep tenants isolated on disk as well as in the DB.
 * The returned storage key is the path relative to the base dir and is what we persist
 * on the documents row; ingestion (Card 6) reads the bytes back via {@link #load}.
 */
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${app.storage.dir:./data/uploads}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /** Persists the upload and returns its storage key (relative path). */
    public String store(UUID companyId, UUID documentId, String originalFilename, MultipartFile file) {
        String ext = extensionOf(originalFilename);
        String relativeKey = companyId + "/" + documentId + ext;
        Path target = baseDir.resolve(relativeKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to store uploaded file");
        }
        return relativeKey;
    }

    /** Resolves a storage key back to an absolute path for reading. */
    public Path load(String storageKey) {
        return baseDir.resolve(storageKey).normalize();
    }

    /** Best-effort deletion of a stored file; missing files are ignored. */
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(storageKey).normalize());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR",
                    "Failed to delete stored file");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
