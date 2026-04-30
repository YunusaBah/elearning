package com.utg.elearning.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The original filename (e.g. "lecture1.pdf")
    @Column(name = "file_name")
    private String fileName;

    // Full path on disk
    @Column(name = "file_path")
    private String filePath;

    // Department this file belongs to
    private String department;

    // Username of the lecturer who uploaded it
    @Column(name = "uploaded_by")
    private String uploadedBy;

    // When it was uploaded
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}