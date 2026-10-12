package com.example.smartattendance.controller;

import com.example.smartattendance.model.AttendanceRecord;
import com.example.smartattendance.model.AttendanceStatus;
import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.SessionStatus;
import com.example.smartattendance.model.Student;
import com.example.smartattendance.model.Subject;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.ClassSessionRepository;
import com.example.smartattendance.repository.StudentRepository;
import com.example.smartattendance.repository.SubjectRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public DashboardController(StudentRepository studentRepository,
                              SubjectRepository subjectRepository,
                              ClassSessionRepository classSessionRepository,
                              AttendanceRecordRepository attendanceRecordRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.classSessionRepository = classSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        long totalStudents = studentRepository.count();
        long totalSubjects = subjectRepository.count();
        long totalFinalizedSessions = classSessionRepository.findByStatus(SessionStatus.FINALIZED).size();

        LocalDate today = LocalDate.now();
        List<ClassSession> todaySessions = classSessionRepository.findAll().stream()
                .filter(session -> session.getSessionDate().equals(today))
                .filter(session -> session.getStatus() == SessionStatus.FINALIZED)
                .toList();

        long presentToday = 0;
        long absentToday = 0;
        for (ClassSession session : todaySessions) {
            List<AttendanceRecord> attendance = attendanceRecordRepository.findBySession_IdOrderByStudent_FullNameAsc(session.getId());
            for (AttendanceRecord record : attendance) {
                if (record.getStatus() == AttendanceStatus.PRESENT) {
                    presentToday++;
                } else {
                    absentToday++;
                }
            }
        }

        List<AttendanceRecord> allRecords = attendanceRecordRepository.findAll();
        List<AttendanceRecord> conductedRecords = allRecords.stream()
                .filter(record -> record.getSession().getStatus() == SessionStatus.FINALIZED)
                .toList();
        long classesAttended = conductedRecords.stream()
                .filter(record -> record.getStatus() == AttendanceStatus.PRESENT)
                .count();
        long eligibleConductedClasses = conductedRecords.size();
        double overallAttendancePercentage = eligibleConductedClasses == 0 ? 0 : (classesAttended * 100.0) / eligibleConductedClasses;

        List<Map<String, Object>> lowAttendanceStudents = new ArrayList<>();
        for (Student student : studentRepository.findAllByOrderByFullNameAsc()) {
            List<AttendanceRecord> records = attendanceRecordRepository.findByStudent_IdOrderBySession_SessionDateDesc(student.getId()).stream()
                    .filter(record -> record.getSession().getStatus() == SessionStatus.FINALIZED)
                    .toList();
            long attended = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
            long total = records.size();
            double percentage = total == 0 ? 0 : (attended * 100.0) / total;
            if (total > 0 && percentage < 75.0) {
                Map<String, Object> item = new HashMap<>();
                item.put("studentId", student.getId());
                item.put("studentName", student.getFullName());
                item.put("rollNumber", student.getRollNumber());
                item.put("attendancePercentage", Math.round(percentage * 10.0) / 10.0);
                item.put("classesAttended", attended);
                item.put("classesMissed", total - attended);
                lowAttendanceStudents.add(item);
            }
        }

        List<Map<String, Object>> recentSessions = classSessionRepository.findAllByOrderBySessionDateDescCreatedAtDesc().stream()
                .limit(5)
                .map(session -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", session.getId());
                    item.put("subjectName", session.getSubject().getSubjectName());
                    item.put("sessionDate", session.getSessionDate().toString());
                    item.put("status", session.getStatus().name());
                    return item;
                })
                .toList();

        List<Map<String, Object>> subjectSummary = new ArrayList<>();
        for (Subject subject : subjectRepository.findAllByOrderBySubjectNameAsc()) {
            List<AttendanceRecord> subjectRecords = attendanceRecordRepository.findBySession_Subject_IdOrderBySession_SessionDateDesc(subject.getId()).stream()
                    .filter(record -> record.getSession().getStatus() == SessionStatus.FINALIZED)
                    .toList();
            long subjectTotal = subjectRecords.size();
            long subjectPresent = subjectRecords.stream()
                    .filter(record -> record.getStatus() == AttendanceStatus.PRESENT)
                    .count();
            double percentage = subjectTotal == 0 ? 0 : (subjectPresent * 100.0) / subjectTotal;
            Map<String, Object> item = new HashMap<>();
            item.put("subjectId", subject.getId());
            item.put("subjectName", subject.getSubjectName());
            item.put("subjectCode", subject.getSubjectCode());
            item.put("attendancePercentage", Math.round(percentage * 10.0) / 10.0);
            item.put("classesAttended", subjectPresent);
            item.put("totalClasses", subjectTotal);
            subjectSummary.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalStudents", totalStudents);
        response.put("totalSubjects", totalSubjects);
        response.put("totalFinalizedSessions", totalFinalizedSessions);
        response.put("todayPresent", presentToday);
        response.put("todayAbsent", absentToday);
        response.put("overallAttendancePercentage", Math.round(overallAttendancePercentage * 10.0) / 10.0);
        response.put("lowAttendanceStudents", lowAttendanceStudents);
        response.put("recentSessions", recentSessions);
        response.put("subjectSummary", subjectSummary);
        return response;
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        List<Student> students = studentRepository.findAllByOrderByFullNameAsc();
        List<Map<String, Object>> studentReports = new ArrayList<>();

        for (Student student : students) {
            List<AttendanceRecord> records = attendanceRecordRepository.findByStudent_IdOrderBySession_SessionDateDesc(student.getId()).stream()
                    .filter(record -> record.getSession().getStatus() == SessionStatus.FINALIZED)
                    .toList();
            long attended = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
            long total = records.size();
            double percentage = total == 0 ? 0 : (attended * 100.0) / total;
            Map<String, Object> report = new HashMap<>();
            report.put("studentId", student.getId());
            report.put("studentName", student.getFullName());
            report.put("rollNumber", student.getRollNumber());
            report.put("department", student.getDepartment());
            report.put("semester", student.getSemester());
            report.put("classesAttended", attended);
            report.put("totalClasses", total);
            report.put("attendancePercentage", Math.round(percentage * 10.0) / 10.0);
            studentReports.add(report);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("studentReports", studentReports);
        result.put("totalStudents", students.size());
        return result;
    }
}
