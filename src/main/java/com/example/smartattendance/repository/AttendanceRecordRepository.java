package com.example.smartattendance.repository;

import com.example.smartattendance.model.AttendanceRecord;
import com.example.smartattendance.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySession_IdOrderByStudent_FullNameAsc(Long sessionId);

    List<AttendanceRecord> findByStudent_IdOrderBySession_SessionDateDesc(Long studentId);

    Optional<AttendanceRecord> findBySession_IdAndStudent_Id(Long sessionId, Long studentId);

    boolean existsByStudent_Id(Long studentId);

    boolean existsBySession_Subject_IdAndStudent_Id(Long subjectId, Long studentId);

    List<AttendanceRecord> findBySession_Subject_IdOrderBySession_SessionDateDesc(Long subjectId);

    Long countBySession_IdAndStatus(Long sessionId, AttendanceStatus status);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.status = :status")
    Long countByStatus(@Param("status") AttendanceStatus status);

    @Query("SELECT ar.student.id, COUNT(ar) FROM AttendanceRecord ar WHERE ar.status = :status GROUP BY ar.student.id")
    List<Object[]> countPresentByStudent(@Param("status") AttendanceStatus status);

    @Query("SELECT ar.student.id, COUNT(ar) FROM AttendanceRecord ar GROUP BY ar.student.id")
    List<Object[]> totalRecordsByStudent();

    @Query("SELECT ar.session.subject.id, ar.session.subject.subjectName, COUNT(ar) FROM AttendanceRecord ar WHERE ar.status = :status GROUP BY ar.session.subject.id, ar.session.subject.subjectName")
    List<Object[]> countPresentBySubject(@Param("status") AttendanceStatus status);
}
