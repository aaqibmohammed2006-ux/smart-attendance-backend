package com.example.smartattendance.config;

import com.example.smartattendance.model.AttendanceRecord;
import com.example.smartattendance.model.AttendanceStatus;
import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.Enrollment;
import com.example.smartattendance.model.SessionStatus;
import com.example.smartattendance.model.Student;
import com.example.smartattendance.model.Subject;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.ClassSessionRepository;
import com.example.smartattendance.repository.EnrollmentRepository;
import com.example.smartattendance.repository.StudentRepository;
import com.example.smartattendance.repository.SubjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@Configuration
public class DatabaseInitializer {

    @Bean
    @Profile({"default", "dev"})
    CommandLineRunner seedDatabase(
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            EnrollmentRepository enrollmentRepository,
            ClassSessionRepository classSessionRepository,
            AttendanceRecordRepository attendanceRecordRepository
    ) {
        return args -> {
            if (studentRepository.count() > 0 || subjectRepository.count() > 0) {
                return;
            }

            Student s1 = studentRepository.save(new Student("CS-101", "Aisha Rahman", "aisha@example.com", "CSE", "Semester 5"));
            Student s2 = studentRepository.save(new Student("CS-102", "Rohan Patel", "rohan@example.com", "CSE", "Semester 5"));
            Student s3 = studentRepository.save(new Student("CS-103", "Meera Nair", "meera@example.com", "EEE", "Semester 4"));
            Student s4 = studentRepository.save(new Student("CS-104", "Kabir Singh", "kabir@example.com", "IT", "Semester 6"));
            Student s5 = studentRepository.save(new Student("CS-105", "Nisha Verma", "nisha@example.com", "CSE", "Semester 3"));
            Student s6 = studentRepository.save(new Student("CS-106", "Arjun Das", "arjun@example.com", "ECE", "Semester 5"));

            Subject sub1 = subjectRepository.save(new Subject("CSE-201", "Data Structures", "CSE", "Semester 5"));
            Subject sub2 = subjectRepository.save(new Subject("CSE-205", "Database Systems", "CSE", "Semester 5"));
            Subject sub3 = subjectRepository.save(new Subject("EEE-301", "Signals and Systems", "EEE", "Semester 4"));

            enrollmentRepository.save(new Enrollment(s1, sub1));
            enrollmentRepository.save(new Enrollment(s2, sub1));
            enrollmentRepository.save(new Enrollment(s3, sub1));
            enrollmentRepository.save(new Enrollment(s4, sub1));
            enrollmentRepository.save(new Enrollment(s1, sub2));
            enrollmentRepository.save(new Enrollment(s2, sub2));
            enrollmentRepository.save(new Enrollment(s5, sub2));
            enrollmentRepository.save(new Enrollment(s3, sub3));
            enrollmentRepository.save(new Enrollment(s6, sub3));

            ClassSession session1 = classSessionRepository.save(new ClassSession(sub1, LocalDate.now().minusDays(4), "09:00", SessionStatus.FINALIZED));
            ClassSession session2 = classSessionRepository.save(new ClassSession(sub1, LocalDate.now().minusDays(2), "09:00", SessionStatus.FINALIZED));
            ClassSession session3 = classSessionRepository.save(new ClassSession(sub2, LocalDate.now().minusDays(3), "11:00", SessionStatus.DRAFT));
            ClassSession session4 = classSessionRepository.save(new ClassSession(sub3, LocalDate.now().minusDays(1), "10:30", SessionStatus.FINALIZED));

            attendanceRecordRepository.save(new AttendanceRecord(session1, s1, AttendanceStatus.PRESENT));
            attendanceRecordRepository.save(new AttendanceRecord(session1, s2, AttendanceStatus.ABSENT));
            attendanceRecordRepository.save(new AttendanceRecord(session1, s3, AttendanceStatus.PRESENT));
            attendanceRecordRepository.save(new AttendanceRecord(session1, s4, AttendanceStatus.PRESENT));

            attendanceRecordRepository.save(new AttendanceRecord(session2, s1, AttendanceStatus.PRESENT));
            attendanceRecordRepository.save(new AttendanceRecord(session2, s2, AttendanceStatus.PRESENT));
            attendanceRecordRepository.save(new AttendanceRecord(session2, s3, AttendanceStatus.ABSENT));
            attendanceRecordRepository.save(new AttendanceRecord(session2, s4, AttendanceStatus.ABSENT));

            attendanceRecordRepository.save(new AttendanceRecord(session4, s3, AttendanceStatus.PRESENT));
            attendanceRecordRepository.save(new AttendanceRecord(session4, s6, AttendanceStatus.ABSENT));
        };
    }
}
