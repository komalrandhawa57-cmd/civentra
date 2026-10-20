package com.civentra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageStorageService {
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    @Value("${civentra.storage.upload-dir:${user.home}/.civentra/uploads}")
    private String uploadDirectory;

    public String storeIssueImage(InputStream input, String contentType) throws IOException {
        if (input == null) throw new IOException("No image data was received.");
        byte[] bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IOException("Images must be between 1 byte and 5 MB.");
        }

        String extension = detectImageExtension(bytes);
        if (extension == null || !matchesContentType(extension, contentType)) {
            throw new IOException("Upload a valid PNG, JPEG, or WebP image.");
        }

        Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = directory.resolve(filename).normalize();
        if (!destination.getParent().equals(directory)) throw new IOException("Invalid upload path.");
        Files.write(destination, bytes);
        return "/uploads/" + filename;
    }

    public Optional<Path> findStoredImage(String filename) {
        if (filename == null || !filename.matches("(?i)[0-9a-f-]{36}\\.(png|jpg|webp)")) {
            return Optional.empty();
        }
        Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Path image = directory.resolve(filename).normalize();
        if (!directory.equals(image.getParent()) || !Files.isRegularFile(image)) return Optional.empty();
        return Optional.of(image);
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "png";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "jpg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "webp";
        return null;
    }

    private boolean matchesContentType(String extension, String contentType) {
        if (contentType == null) return false;
        return switch (extension) {
            case "png" -> "image/png".equalsIgnoreCase(contentType);
            case "jpg" -> "image/jpeg".equalsIgnoreCase(contentType);
            case "webp" -> "image/webp".equalsIgnoreCase(contentType);
            default -> false;
        };
    }
}
