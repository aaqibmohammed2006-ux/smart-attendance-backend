package com.example.smartattendance.controller;

import com.example.smartattendance.dto.EnrollmentDto;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.model.Enrollment;
import com.example.smartattendance.model.Student;
import com.example.smartattendance.model.Subject;
import com.example.smartattendance.repository.EnrollmentRepository;
import com.example.smartattendance.repository.StudentRepository;
import com.example.smartattendance.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public EnrollmentController(EnrollmentRepository enrollmentRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               StudentRepository studentRepository,
                               SubjectRepository subjectRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @GetMapping("/enrollments")
    @Transactional(readOnly = true)
    public List<EnrollmentDto> getAllEnrollments() {
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        List<EnrollmentDto> dtoList = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            dtoList.add(toDto(enrollment));
        }
        return dtoList;
    }

    @GetMapping("/subjects/{subjectId}/enrollments")
    @Transactional(readOnly = true)
    public List<EnrollmentDto> getSubjectEnrollments(@PathVariable Long subjectId) {
        return enrollmentRepository.findBySubjectWithStudent(subjectId).stream().map(this::toDto).toList();
    }

    @GetMapping("/students/{studentId}/enrollments")
    @Transactional(readOnly = true)
    public List<EnrollmentDto> getStudentEnrollments(@PathVariable Long studentId) {
        return enrollmentRepository.findByStudentWithSubject(studentId).stream().map(this::toDto).toList();
    }

    @PostMapping("/enrollments")
    public ResponseEntity<?> enrollStudent(@RequestBody Map<String, Object> request) {
        Long studentId = asLong(request.get("studentId"));
        Long subjectId = asLong(request.get("subjectId"));

        if (studentId == null || subjectId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId and subjectId are required."));
        }

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Subject> subjectOpt = subjectRepository.findById(subjectId);

        if (studentOpt.isEmpty() || subjectOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Student or subject not found."));
        }

        if (enrollmentRepository.existsByStudent_IdAndSubject_Id(studentId, subjectId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This student is already enrolled in the selected subject."));
        }

        Enrollment saved = enrollmentRepository.save(new Enrollment(studentOpt.get(), subjectOpt.get()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<?> removeEnrollment(@PathVariable Long id) {
        Optional<Enrollment> enrollment = enrollmentRepository.findById(id);
        if (enrollment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (attendanceRecordRepository.existsBySession_Subject_IdAndStudent_Id(
                enrollment.get().getSubject().getId(), enrollment.get().getStudent().getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Enrollment cannot be removed because attendance history exists."));
        }
        enrollmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        Subject subject = enrollment.getSubject();
        EnrollmentDto dto = new EnrollmentDto();
        dto.setId(enrollment.getId());
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFullName());
        dto.setRollNumber(student.getRollNumber());
        dto.setSubjectId(subject.getId());
        dto.setSubjectName(subject.getSubjectName());
        dto.setSubjectCode(subject.getSubjectCode());
        return dto;
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
}
