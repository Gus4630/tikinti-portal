package az.tikinti.portal.service.file;

import az.tikinti.portal.exception.NonRetryableOcrException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${application.upload.dir:uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + uploadDirPath, e);
        }
    }

    public String store(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target);
            log.info("Stored file: {}", filename);
            return target.toString();
        } catch (IOException e) {
            throw new NonRetryableOcrException("Failed to store file: " + filename, e);
        }
    }

    public byte[] readBytes(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new NonRetryableOcrException("Cannot read file: " + filePath, e);
        }
    }

    public Resource loadAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NonRetryableOcrException("File not found or not readable: " + filePath);
            }
            return resource;
        } catch (Exception e) {
            throw new NonRetryableOcrException("Cannot load file: " + filePath, e);
        }
    }

    public InputStream openStream(String filePath) {
        try {
            return Files.newInputStream(Paths.get(filePath));
        } catch (IOException e) {
            throw new NonRetryableOcrException("Cannot open file stream: " + filePath, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
