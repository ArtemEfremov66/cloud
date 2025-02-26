package com.diplom.cloud.service;

import com.diplom.cloud.entity.File;
import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.FileRepository;
import com.diplom.cloud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class FileServiceTest {
    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUploadFile() throws IOException {
        // Arrange
        String email = "test@mail.ru";
        String filename = "test.txt";
        MultipartFile file = mock(MultipartFile.class);
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(user);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn(filename);

        // Act
        fileService.uploadFile(email, filename, file);

        // Assert
        verify(fileRepository, times(1)).save(any(File.class));
    }
}
