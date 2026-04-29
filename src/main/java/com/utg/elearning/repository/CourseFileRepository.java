// CourseFileRepository.java
package com.utg.elearning.repository;

import com.utg.elearning.model.CourseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {
    List<CourseFile> findByDepartment(String department);
}