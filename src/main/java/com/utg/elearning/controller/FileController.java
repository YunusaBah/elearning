package com.utg.elearning.controller;

import com.utg.elearning.model.CourseFile;
import com.utg.elearning.model.Role;
import com.utg.elearning.model.User;
import com.utg.elearning.repository.UserRepository;
import com.utg.elearning.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    // LECTURERS ONLY: upload a file
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("department") String department,
            Authentication auth) throws IOException {

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only lecturers can upload files.");
        }

        CourseFile saved = fileService.uploadFile(file, department, auth.getName());
        return ResponseEntity.ok(saved);
    }

    // ALL USERS: list all files
    @GetMapping
    public ResponseEntity<List<CourseFile>> listFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }

    // ALL USERS: list files by department
    @GetMapping("/department/{dept}")
    public ResponseEntity<List<CourseFile>> listByDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(fileService.getFilesByDepartment(dept));
    }

    // ALL USERS: download a file
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws MalformedURLException {
        Path filePath = fileService.downloadFile(id);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }

    // LECTURERS ONLY: delete an uploaded file
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id, Authentication auth) throws IOException {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only lecturers can delete files.");
        }

        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
