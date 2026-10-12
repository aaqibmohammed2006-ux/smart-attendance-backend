package com.example.smartattendance.repository;

import com.example.smartattendance.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_IdAndSubject_Id(Long studentId, Long subjectId);

    List<Enrollment> findBySubject_IdOrderByStudent_FullNameAsc(Long subjectId);

    List<Enrollment> findByStudent_IdOrderBySubject_SubjectNameAsc(Long studentId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s JOIN FETCH e.subject subj WHERE subj.id = :subjectId ORDER BY s.fullName ASC")
    List<Enrollment> findBySubjectWithStudent(@Param("subjectId") Long subjectId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s JOIN FETCH e.subject subj WHERE s.id = :studentId ORDER BY subj.subjectName ASC")
    List<Enrollment> findByStudentWithSubject(@Param("studentId") Long studentId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.subject.id = :subjectId")
    Optional<Enrollment> findByStudentAndSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    Long countBySubject_Id(Long subjectId);
}
