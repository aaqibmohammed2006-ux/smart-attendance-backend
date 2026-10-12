package com.example.smartattendance;

import com.example.smartattendance.model.AttendanceRecord;
import com.example.smartattendance.model.AttendanceStatus;
import com.example.smartattendance.model.ClassSession;
import com.example.smartattendance.model.SessionStatus;
import com.example.smartattendance.repository.AttendanceRecordRepository;
import com.example.smartattendance.repository.ClassSessionRepository;
import com.example.smartattendance.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SmartAttendanceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void developmentProfileSeedsDemoDataAndServesDashboard() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(studentRepository.count() > 0);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents", greaterThan(0)));
    }

    @Test
    void healthChecksDatabaseAndCorsAllowsOnlyConfiguredOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/health")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void frontendReadRoutesReturnTheirExpectedCollections() throws Exception {
        mockMvc.perform(get("/api/students")).andExpect(status().isOk()).andExpect(jsonPath("$[0].rollNumber").exists());
        mockMvc.perform(get("/api/subjects")).andExpect(status().isOk()).andExpect(jsonPath("$[0].subjectCode").exists());
        mockMvc.perform(get("/api/enrollments")).andExpect(status().isOk()).andExpect(jsonPath("$[0].studentId").exists());
        mockMvc.perform(get("/api/sessions")).andExpect(status().isOk()).andExpect(jsonPath("$[0].subjectId").exists());
        mockMvc.perform(get("/api/reports")).andExpect(status().isOk()).andExpect(jsonPath("$.studentReports").isArray());
        mockMvc.perform(get("/api/attendance/session/1")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/attendance/session/1/eligible-students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").exists());
    }

    @Test
    void finalizedAttendanceRequiresReopeningBeforeCorrection() throws Exception {
        ClassSession finalizedSession = classSessionRepository.findByStatus(SessionStatus.FINALIZED).get(0);
        List<AttendanceRecord> records = attendanceRecordRepository
                .findBySession_IdOrderByStudent_FullNameAsc(finalizedSession.getId());
        AttendanceRecord record = records.get(0);
        AttendanceStatus correctedStatus = record.getStatus() == AttendanceStatus.PRESENT
                ? AttendanceStatus.ABSENT
                : AttendanceStatus.PRESENT;
        String payload = "[{\"studentId\":" + record.getStudent().getId()
                + ",\"status\":\"" + correctedStatus.name() + "\"}]";

        mockMvc.perform(post("/api/attendance/session/{id}", finalizedSession.getId())
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/sessions/{id}/reopen", finalizedSession.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/attendance/session/{id}", finalizedSession.getId())
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertEquals(correctedStatus, attendanceRecordRepository
                .findBySession_IdAndStudent_Id(finalizedSession.getId(), record.getStudent().getId())
                .orElseThrow()
                .getStatus());
    }
}
