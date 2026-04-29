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

    private String fileName;
    private String filePath;
    private String department;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}