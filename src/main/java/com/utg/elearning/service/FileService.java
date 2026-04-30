package com.utg.elearning.service;

import com.utg.elearning.model.CourseFile;
import com.utg.elearning.repository.CourseFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FileService {

    // ReentrantLock for synchronization — only one upload at a time
    private final ReentrantLock fileLock = new ReentrantLock();

    @Autowired
    private CourseFileRepository fileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public CourseFile uploadFile(MultipartFile file, String department, String uploadedBy) throws IOException {
        fileLock.lock(); // Acquire lock — prevents concurrent writes
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            CourseFile courseFile = new CourseFile();
            courseFile.setFileName(file.getOriginalFilename());
            courseFile.setFilePath(filePath.toString());
            courseFile.setDepartment(department);
            courseFile.setUploadedBy(uploadedBy);
            courseFile.setUploadedAt(LocalDateTime.now());

            return fileRepository.save(courseFile);
        } finally {
            fileLock.unlock(); // Always release lock
        }
    }

    public List<CourseFile> getAllFiles() {
        return fileRepository.findAll();
    }

    public List<CourseFile> getFilesByDepartment(String department) {
        return fileRepository.findByDepartment(department);
    }

    public Path downloadFile(Long fileId) {
        CourseFile courseFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        return Paths.get(courseFile.getFilePath());
    }

    public void deleteFile(Long fileId) throws IOException {
        fileLock.lock();
        try {
            CourseFile courseFile = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            Path filePath = Paths.get(courseFile.getFilePath());
            Files.deleteIfExists(filePath);
            fileRepository.delete(courseFile);
        } finally {
            fileLock.unlock();
        }
    }
}
