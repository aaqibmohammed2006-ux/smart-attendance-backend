package com.example.smartattendance.repository;

import com.example.smartattendance.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubjectCode(String subjectCode);

    @Query("SELECT s FROM Subject s WHERE " +
            "(:department IS NULL OR :department = '' OR LOWER(s.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
            "(:semester IS NULL OR :semester = '' OR LOWER(s.semester) LIKE LOWER(CONCAT('%', :semester, '%'))) AND " +
            "(:term IS NULL OR :term = '' OR LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(s.subjectCode) LIKE LOWER(CONCAT('%', :term, '%'))) " +
            "ORDER BY s.subjectName ASC")
    List<Subject> search(@Param("department") String department,
                         @Param("semester") String semester,
                         @Param("term") String term);

    List<Subject> findAllByOrderBySubjectNameAsc();
}
