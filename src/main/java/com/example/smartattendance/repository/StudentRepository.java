package com.example.smartattendance.repository;

import com.example.smartattendance.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRollNumber(String rollNumber);

    @Query("SELECT s FROM Student s WHERE " +
            "(:department IS NULL OR :department = '' OR LOWER(s.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
            "(:semester IS NULL OR :semester = '' OR LOWER(s.semester) LIKE LOWER(CONCAT('%', :semester, '%'))) AND " +
            "(:term IS NULL OR :term = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :term, '%'))) " +
            "ORDER BY s.fullName ASC")
    List<Student> search(@Param("department") String department,
                         @Param("semester") String semester,
                         @Param("term") String term);

    List<Student> findAllByOrderByFullNameAsc();
}
