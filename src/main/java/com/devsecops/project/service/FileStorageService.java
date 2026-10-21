package com.devsecops.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * File storage for the upload demonstration.
 * Files are ALWAYS written inside the local "uploads" folder (the target is checked to stay inside it).
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * VULNERABILITY #21: unrestricted file upload. There is no check of the file type, the extension,
     * the content or the size, and an existing file with the same name is silently overwritten.
     * VULNERABILITY #24 (part): the exception is swallowed and only printed to the console.
     */
    public String save(MultipartFile file) {
        try {
            String name = Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root) || target.equals(root)) {
                return null; // safety guard: never write outside the uploads folder
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> list() {
        try (Stream<Path> files = Files.list(root)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
