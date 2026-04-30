package com.utg.elearning.controller;

import com.utg.elearning.model.Role;
import com.utg.elearning.model.User;
import com.utg.elearning.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private UserRepository userRepository;

    // In-memory list of submissions (replace with DB entity if you want persistence)
    private final List<Map<String, Object>> submissions = new CopyOnWriteArrayList<>();
    private long idCounter = 1;

    /**
     * POST /api/submissions/upload
     * Students upload their completed assignment files.
     * Params: file (MultipartFile), studentName (String), assignmentTitle (String)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentName") String studentName,
            @RequestParam("assignmentTitle") String assignmentTitle,
            Authentication auth) throws IOException {

        // Any authenticated user can submit (students primarily)
        Path submissionsDir = Paths.get(uploadDir, "submissions");
        if (!Files.exists(submissionsDir)) {
            Files.createDirectories(submissionsDir);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = submissionsDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> submission = new LinkedHashMap<>();
        submission.put("id", idCounter++);
        submission.put("studentName", studentName);
        submission.put("assignmentTitle", assignmentTitle);
        submission.put("fileName", file.getOriginalFilename());
        submission.put("filePath", filePath.toString());
        submission.put("submittedBy", auth.getName());
        submission.put("submittedAt", LocalDateTime.now().toString());

        submissions.add(submission);

        return ResponseEntity.ok(Map.of(
                "message", "Assignment submitted successfully",
                "fileName", file.getOriginalFilename()
        ));
    }

    /**
     * GET /api/submissions/list
     * Lecturers see all student submissions.
     */
    @GetMapping("/list")
    public ResponseEntity<?> listSubmissions(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only lecturers can view all submissions.");
        }

        return ResponseEntity.ok(submissions);
    }

    /**
     * GET /api/submissions/download/{id}
     * Download a specific submission file.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadSubmission(
            @PathVariable Long id,
            Authentication auth) throws MalformedURLException {

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only lecturers can download student submissions
        if (user.getRole() != Role.LECTURER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> submission = submissions.stream()
                .filter(s -> s.get("id").equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Path filePath = Paths.get(submission.get("filePath").toString());
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }
}