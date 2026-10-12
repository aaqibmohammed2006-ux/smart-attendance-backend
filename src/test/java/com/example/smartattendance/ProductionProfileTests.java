package com.example.smartattendance;

import com.example.smartattendance.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production-profile;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.cors.allowed-origins=https://frontend.example"
})
@ActiveProfiles("postgres")
class ProductionProfileTests {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void productionProfileDoesNotSeedDevelopmentRecords() {
        assertEquals(0, studentRepository.count());
    }
}
