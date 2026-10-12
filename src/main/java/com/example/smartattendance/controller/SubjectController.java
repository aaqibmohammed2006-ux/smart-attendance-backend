package com.example.smartattendance.controller;

import com.example.smartattendance.dto.SubjectDto;
import com.example.smartattendance.model.Subject;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;

    public SubjectController(SubjectRepository subjectRepository, EnrollmentRepository enrollmentRepository,
                             ClassSessionRepository classSessionRepository) {
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.classSessionRepository = classSessionRepository;
    }

    @GetMapping
    public List<SubjectDto> getSubjects(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String query
    ) {
        return subjectRepository.search(department, semester, query).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectDto> getSubjectById(@PathVariable Long id) {
        Optional<Subject> subject = subjectRepository.findById(id);
        return subject.map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSubject(@Valid @RequestBody Subject subject) {
        if (subjectRepository.existsBySubjectCode(subject.getSubjectCode())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "A subject with this code already exists.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Subject saved = subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubject(@PathVariable Long id, @Valid @RequestBody Subject updatedSubject) {
        Optional<Subject> existing = subjectRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Subject current = existing.get();
        if (!current.getSubjectCode().equals(updatedSubject.getSubjectCode())
                && subjectRepository.existsBySubjectCode(updatedSubject.getSubjectCode())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Another subject already uses this code.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        current.setSubjectCode(updatedSubject.getSubjectCode());
        current.setSubjectName(updatedSubject.getSubjectName());
        current.setDepartment(updatedSubject.getDepartment());
        current.setSemester(updatedSubject.getSemester());

        Subject saved = subjectRepository.save(current);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable Long id) {
        if (!subjectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (enrollmentRepository.findBySubject_IdOrderByStudent_FullNameAsc(id).size() > 0) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Subject cannot be deleted because it has existing enrollments.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        if (classSessionRepository.existsBySubject_Id(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Subject cannot be deleted because class session history exists."));
        }
        subjectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private SubjectDto toDto(Subject subject) {
        SubjectDto dto = new SubjectDto();
        dto.setId(subject.getId());
        dto.setSubjectCode(subject.getSubjectCode());
        dto.setSubjectName(subject.getSubjectName());
        dto.setDepartment(subject.getDepartment());
        dto.setSemester(subject.getSemester());
        return dto;
    }
}
