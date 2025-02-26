package com.diplom.cloud.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.FileRepository;
import com.diplom.cloud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Testcontainers
@SpringBootTest
public class FileServiceIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        fileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testUploadFile() throws IOException {
        // Arrange
        String email = "user@example.com";
        String filename = "test.txt";
        MultipartFile file = mock(MultipartFile.class);
        User user = new User();
        user.setEmail(email);
        userRepository.save(user);

        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn(filename);

        // Act
        fileService.uploadFile(email, filename, file);

        // Assert
        assertEquals(1, fileRepository.count());
    }
}