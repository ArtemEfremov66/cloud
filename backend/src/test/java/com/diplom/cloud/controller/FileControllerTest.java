package com.diplom.cloud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diplom.cloud.service.FileService;
import com.diplom.cloud.token.TokenStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @Mock
    private TokenStorage tokenStorage;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    public void setUp() {
        // Инициализация MockMvc с контроллером
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }

    @Test
    public void testUploadFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
        );

        // Мокируем поведение TokenStorage
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение FileService (ничего не делаем, так как метод void)
            doNothing().when(fileService).uploadFile("testUser", "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile("testUser", "test.txt", file);
        }
    }

    @Test
    public void testUploadFile_InvalidToken() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
        );

        // Мокируем поведение TokenStorage (токен невалиден)
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn(null);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "invalid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnauthorized());

            // Проверяем, что метод uploadFile не был вызван
            verify(fileService, never()).uploadFile(any(), any(), any());
        }
    }

    @Test
    public void testUploadFile_UserNotFound() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
        );

        // Мокируем поведение TokenStorage
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение FileService (пользователь не найден)
            doThrow(new RuntimeException("User not found")).when(fileService).uploadFile("testUser", "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile("testUser", "test.txt", file);
        }
    }

    @Test
    public void testUploadFile_InternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
        );

        // Мокируем поведение TokenStorage
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение FileService (внутренняя ошибка сервера)
            doThrow(new IOException("Failed to upload file")).when(fileService).uploadFile("testUser", "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isInternalServerError());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile("testUser", "test.txt", file);
        }
    }
}
