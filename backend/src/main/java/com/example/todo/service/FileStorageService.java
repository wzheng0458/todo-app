package com.example.todo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "pdf", "txt", "doc", "docx", "xls", "xlsx", "csv", "zip");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Upload directory: {}", root);
    }

    /** Saves the file under a random name and returns that stored name. */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed: " + ALLOWED_EXTENSIONS);
        }
        String stored = UUID.randomUUID() + "." + ext.toLowerCase();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        log.debug("Stored file {} as {}", original, stored);
        return stored;
    }

    public Resource load(String storedName) {
        try {
            Path file = root.resolve(storedName).normalize();
            if (!file.startsWith(root)) {
                throw new IllegalArgumentException("Invalid path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("File not found on disk");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not read file", e);
        }
    }

    public void delete(String storedName) {
        if (storedName == null) return;
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
            log.debug("Deleted file {}", storedName);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", storedName, e.getMessage());
        }
    }
}
