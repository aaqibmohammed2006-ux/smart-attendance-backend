package com.example.smartattendance.controller;

import com.example.smartattendance.dto.AttendanceSubmission;
import com.example.smartattendance.model.AttendanceRecord;
import com.example.smartattendance.model.AttendanceStatus;
import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.Enrollment;
import com.example.smartattendance.model.SessionStatus;
import com.example.smartattendance.model.Student;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.ClassSessionRepository;
import com.example.smartattendance.repository.EnrollmentRepository;
import com.example.smartattendance.repository.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRepository studentRepository;

    public AttendanceController(ClassSessionRepository classSessionRepository,
                               EnrollmentRepository enrollmentRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               StudentRepository studentRepository) {
        this.classSessionRepository = classSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/attendance/session/{sessionId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getAttendanceForSession(@PathVariable Long sessionId) {
        Optional<ClassSession> session = classSessionRepository.findById(sessionId);
        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<AttendanceRecord> records = attendanceRecordRepository.findBySession_IdOrderByStudent_FullNameAsc(sessionId);
        List<Map<String, Object>> result = records.stream().map(this::toSummary).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attendance/session/{sessionId}/eligible-students")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getEligibleStudents(@PathVariable Long sessionId) {
        Optional<ClassSession> session = classSessionRepository.findById(sessionId);
        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Enrollment> enrollments = enrollmentRepository.findBySubjectWithStudent(session.get().getSubject().getId());
        return ResponseEntity.ok(enrollments.stream().map(enrollment -> {
            Student student = enrollment.getStudent();
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", student.getId());
            item.put("fullName", student.getFullName());
            item.put("rollNumber", student.getRollNumber());
            item.put("department", student.getDepartment());
            item.put("semester", student.getSemester());
            return item;
        }).toList());
    }

    @Transactional
    @PostMapping("/attendance/session/{sessionId}")
    public ResponseEntity<?> saveAttendance(@PathVariable Long sessionId, @RequestBody List<AttendanceSubmission> submissions) {
        Optional<ClassSession> sessionOptional = classSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassSession session = sessionOptional.get();
        if (session.getStatus() == SessionStatus.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Attendance cannot be marked for a cancelled session."));
        }
        if (session.getStatus() == SessionStatus.FINALIZED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Attendance for a finalized session cannot be changed."));
        }

        List<Enrollment> subjectEnrollments = enrollmentRepository.findBySubjectWithStudent(session.getSubject().getId());
        Set<Long> eligibleStudentIds = new HashSet<>();
        for (Enrollment enrollment : subjectEnrollments) {
            eligibleStudentIds.add(enrollment.getStudent().getId());
        }

        Set<Long> seenStudentIds = new HashSet<>();
        for (AttendanceSubmission submission : submissions) {
            if (submission == null || submission.getStudentId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Every submission must include a studentId."));
            }
            Long studentId = submission.getStudentId();
            if (!eligibleStudentIds.contains(studentId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Student is not enrolled in this subject."));
            }
            if (!seenStudentIds.add(studentId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Duplicate attendance entries for the same student are not allowed."));
            }
            if (submission.getStatus() == null || (!submission.getStatus().equalsIgnoreCase("PRESENT") && !submission.getStatus().equalsIgnoreCase("ABSENT"))) {
                return ResponseEntity.badRequest().body(Map.of("message", "Attendance status must be PRESENT or ABSENT."));
            }
        }

        for (AttendanceSubmission submission : submissions) {
            Student student = studentRepository.findById(submission.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            AttendanceStatus status = AttendanceStatus.valueOf(submission.getStatus().trim().toUpperCase());
            Optional<AttendanceRecord> existing = attendanceRecordRepository.findBySession_IdAndStudent_Id(sessionId, student.getId());

            if (existing.isPresent()) {
                AttendanceRecord record = existing.get();
                record.setStatus(status);
                attendanceRecordRepository.save(record);
            } else {
                attendanceRecordRepository.save(new AttendanceRecord(session, student, status));
            }
        }

        return ResponseEntity.ok(Map.of("message", "Attendance saved successfully."));
    }

    private Map<String, Object> toSummary(AttendanceRecord record) {
        Student student = record.getStudent();
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("studentId", student.getId());
        map.put("fullName", student.getFullName());
        map.put("rollNumber", student.getRollNumber());
        map.put("status", record.getStatus().name());
        return map;
    }
}
