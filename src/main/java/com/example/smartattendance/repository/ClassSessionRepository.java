package com.example.smartattendance.repository;

import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    boolean existsBySubject_Id(Long subjectId);

    List<ClassSession> findBySubject_IdOrderBySessionDateDesc(Long subjectId);

    List<ClassSession> findByStatus(SessionStatus status);

    List<ClassSession> findBySubject_IdAndSessionDate(Long subjectId, LocalDate date);

    List<ClassSession> findAllByOrderBySessionDateDescCreatedAtDesc();
}
