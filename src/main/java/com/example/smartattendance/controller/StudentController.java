package com.example.smartattendance.controller;

import com.example.smartattendance.dto.StudentDto;
import com.example.smartattendance.model.Student;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.EnrollmentRepository;
import com.example.smartattendance.repository.StudentRepository;
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
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public StudentController(StudentRepository studentRepository, EnrollmentRepository enrollmentRepository,
                             AttendanceRecordRepository attendanceRecordRepository) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @GetMapping
    public List<StudentDto> getStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String query
    ) {
        return studentRepository.search(department, semester, query).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> getStudentById(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStudent(@Valid @RequestBody Student student) {
        if (studentRepository.existsByRollNumber(student.getRollNumber())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "A student with this roll number already exists.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Student saved = studentRepository.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @Valid @RequestBody Student updatedStudent) {
        Optional<Student> existing = studentRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Student current = existing.get();
        if (!current.getRollNumber().equals(updatedStudent.getRollNumber())
                && studentRepository.existsByRollNumber(updatedStudent.getRollNumber())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Another student already has this roll number.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        current.setRollNumber(updatedStudent.getRollNumber());
        current.setFullName(updatedStudent.getFullName());
        current.setEmail(updatedStudent.getEmail());
        current.setDepartment(updatedStudent.getDepartment());
        current.setSemester(updatedStudent.getSemester());

        Student saved = studentRepository.save(current);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (enrollmentRepository.findByStudent_IdOrderBySubject_SubjectNameAsc(id).size() > 0) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Student cannot be deleted while still enrolled in subjects.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        if (attendanceRecordRepository.existsByStudent_Id(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Student cannot be deleted because attendance history exists."));
        }
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private StudentDto toDto(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setRollNumber(student.getRollNumber());
        dto.setFullName(student.getFullName());
        dto.setEmail(student.getEmail());
        dto.setDepartment(student.getDepartment());
        dto.setSemester(student.getSemester());
        return dto;
    }
}
