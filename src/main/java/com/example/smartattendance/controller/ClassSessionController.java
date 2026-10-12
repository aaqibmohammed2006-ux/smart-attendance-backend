package com.example.smartattendance.controller;

import com.example.smartattendance.dto.ClassSessionDto;
import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.SessionStatus;
import com.example.smartattendance.model.Subject;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.ClassSessionRepository;
import com.example.smartattendance.repository.EnrollmentRepository;
import com.example.smartattendance.repository.SubjectRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@Transactional
@RequestMapping("/api")
public class ClassSessionController {

    private final ClassSessionRepository classSessionRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public ClassSessionController(ClassSessionRepository classSessionRepository,
                                SubjectRepository subjectRepository,
                                EnrollmentRepository enrollmentRepository,
                                AttendanceRecordRepository attendanceRecordRepository) {
        this.classSessionRepository = classSessionRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @GetMapping("/sessions")
    @Transactional(readOnly = true)
    public List<ClassSessionDto> getAllSessions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date
    ) {
        List<ClassSession> sessions = classSessionRepository.findAllByOrderBySessionDateDescCreatedAtDesc();
        return sessions.stream()
                .filter(session -> subjectId == null || session.getSubject().getId().equals(subjectId))
                .filter(session -> status == null || status.isBlank() || session.getStatus().name().equalsIgnoreCase(status))
                .filter(session -> date == null || date.isBlank() || session.getSessionDate().toString().equals(date))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/sessions/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ClassSessionDto> getSession(@PathVariable Long id) {
        return classSessionRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/subjects/{subjectId}/sessions")
    @Transactional(readOnly = true)
    public List<ClassSessionDto> getSessionsBySubject(@PathVariable Long subjectId) {
        return classSessionRepository.findBySubject_IdOrderBySessionDateDesc(subjectId).stream().map(this::toDto).toList();
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@Valid @RequestBody Map<String, Object> payload) {
        Long subjectId = asLong(payload.get("subjectId"));
        String dateText = asString(payload.get("sessionDate"));
        String startTime = asString(payload.get("startTime"));
        String statusText = asString(payload.get("status"));

        if (subjectId == null || dateText == null || dateText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "subjectId and sessionDate are required."));
        }

        Optional<Subject> subject = subjectRepository.findById(subjectId);
        if (subject.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Subject not found."));
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "sessionDate must be in YYYY-MM-DD format."));
        }

        SessionStatus status = parseStatus(statusText, null);
        if (statusText != null && status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "status must be DRAFT, FINALIZED, or CANCELLED."));
        }
        if (status != null && status != SessionStatus.DRAFT) {
            return ResponseEntity.badRequest().body(Map.of("message", "New sessions must start as DRAFT. Use the finalize endpoint after recording attendance."));
        }

        ClassSession session = new ClassSession();
        session.setSubject(subject.get());
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setStatus(SessionStatus.DRAFT);

        ClassSession saved = classSessionRepository.save(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(@PathVariable Long id, @Valid @RequestBody Map<String, Object> payload) {
        Optional<ClassSession> sessionOptional = classSessionRepository.findById(id);
        if (sessionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassSession session = sessionOptional.get();
        if (session.getStatus() == SessionStatus.FINALIZED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Finalized sessions cannot be edited. Reopen the session first."));
        }

        if (payload.containsKey("subjectId")) {
            Long subjectId = asLong(payload.get("subjectId"));
            if (subjectId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "subjectId must be a valid identifier."));
            }
            Optional<Subject> subject = subjectRepository.findById(subjectId);
            if (subject.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Subject not found."));
            }
            if (!session.getSubject().getId().equals(subjectId)
                    && !attendanceRecordRepository.findBySession_IdOrderByStudent_FullNameAsc(id).isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Session subject cannot be changed after attendance has been recorded."));
            }
            session.setSubject(subject.get());
        }

        String dateText = asString(payload.get("sessionDate"));
        if (dateText != null && !dateText.isBlank()) {
            try {
                session.setSessionDate(LocalDate.parse(dateText));
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "sessionDate must be in YYYY-MM-DD format."));
            }
        }

        if (payload.get("startTime") != null) {
            session.setStartTime(asString(payload.get("startTime")));
        }

        if (payload.get("status") != null) {
            String statusText = asString(payload.get("status"));
            SessionStatus status = parseStatus(statusText, null);
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "status must be DRAFT, FINALIZED, or CANCELLED."));
            }
            if (status == SessionStatus.FINALIZED) {
                return ResponseEntity.badRequest().body(Map.of("message", "Use the finalize endpoint after recording attendance."));
            }
            session.setStatus(status);
        }

        ClassSession saved = classSessionRepository.save(session);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/sessions/{id}/finalize")
    public ResponseEntity<?> finalizeSession(@PathVariable Long id) {
        Optional<ClassSession> optional = classSessionRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassSession session = optional.get();
        if (session.getStatus() == SessionStatus.CANCELLED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cancelled sessions cannot be finalized."));
        }

        Set<Long> enrolledStudentIds = new HashSet<>();
        enrollmentRepository.findBySubjectWithStudent(session.getSubject().getId())
                .forEach(enrollment -> enrolledStudentIds.add(enrollment.getStudent().getId()));
        Set<Long> recordedStudentIds = new HashSet<>();
        attendanceRecordRepository.findBySession_IdOrderByStudent_FullNameAsc(id)
                .forEach(record -> recordedStudentIds.add(record.getStudent().getId()));

        if (enrolledStudentIds.isEmpty() || !recordedStudentIds.equals(enrolledStudentIds)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Session must have an explicit attendance record for every enrolled student before finalizing."));
        }

        session.setStatus(SessionStatus.FINALIZED);
        classSessionRepository.save(session);
        return ResponseEntity.ok(Map.of("message", "Session finalized successfully."));
    }

    @PutMapping("/sessions/{id}/reopen")
    public ResponseEntity<?> reopenSession(@PathVariable Long id) {
        Optional<ClassSession> optional = classSessionRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassSession session = optional.get();
        if (session.getStatus() != SessionStatus.FINALIZED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Only finalized sessions can be reopened."));
        }

        session.setStatus(SessionStatus.DRAFT);
        classSessionRepository.save(session);
        return ResponseEntity.ok(Map.of("message", "Session reopened for attendance corrections."));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        Optional<ClassSession> session = classSessionRepository.findById(id);
        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (session.get().getStatus() == SessionStatus.FINALIZED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Reopen finalized sessions before deletion."));
        }

        attendanceRecordRepository.findBySession_IdOrderByStudent_FullNameAsc(id)
                .forEach(attendanceRecordRepository::delete);
        classSessionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ClassSessionDto toDto(ClassSession session) {
        ClassSessionDto dto = new ClassSessionDto();
        dto.setId(session.getId());
        dto.setSubjectId(session.getSubject().getId());
        dto.setSubjectName(session.getSubject().getSubjectName());
        dto.setSubjectCode(session.getSubject().getSubjectCode());
        dto.setSessionDate(session.getSessionDate() == null ? null : session.getSessionDate().toString());
        dto.setStartTime(session.getStartTime());
        dto.setStatus(session.getStatus().name());
        return dto;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private SessionStatus parseStatus(String input, SessionStatus fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        try {
            return SessionStatus.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
