package com.diplom.cloud.controller;

import com.diplom.cloud.Exceptions.FileUploadException;
import com.diplom.cloud.entity.User;
import com.diplom.cloud.service.FileService;
import com.diplom.cloud.service.UserService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private FileService fileService;
    @Mock
    private UserService userService;

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

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Мокируем поведение FileService (ничего не делаем, так как метод void)
            doNothing().when(fileService).uploadFile(user, "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile(user, "test.txt", file);
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
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);
            doThrow(new RuntimeException("User not found")).when(fileService).uploadFile(user, "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile(user, "test.txt", file);
        }
    }

    @Test
    public void testUploadFile_FileUploadException() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
        );

        // Мокируем поведение TokenStorage
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение FileService (внутренняя ошибка сервера)
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);
            doThrow(new FileUploadException("Failed to upload file")).when(fileService).uploadFile(user, "test.txt", file);

            // Act & Assert
            mockMvc.perform(multipart("/cloud/file")
                            .file(file)
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isInternalServerError());

            // Проверяем, что метод uploadFile был вызван
            verify(fileService, times(1)).uploadFile(user, "test.txt", file);
        }
    }

    @Test
    public void testDeleteFile() throws Exception {
        // Arrange
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Мокируем поведение FileService
            doNothing().when(fileService).deleteFile(user, "test.txt");

            // Act & Assert
            mockMvc.perform(delete("/cloud/file")
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token"))
                    .andExpect(status().isOk());

            // Проверяем, что метод deleteFile был вызван
            verify(fileService, times(1)).deleteFile(user, "test.txt");
        }
    }
    @Test
    public void testDownloadFile_Success() throws Exception {
        // Arrange
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Создаем временный файл для теста
            File tempFile = File.createTempFile("test", ".txt");
            tempFile.deleteOnExit(); // Удаляем файл после завершения теста
            when(fileService.getFile(user, "test.txt")).thenReturn(tempFile);

            // Act & Assert
            mockMvc.perform(get("/cloud/file")
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tempFile.getName() + "\""));

            // Проверяем, что метод getFile был вызван
            verify(fileService, times(1)).getFile(user, "test.txt");
        }
    }
    @Test
    public void testDownloadFile_NotFound() throws Exception {
        // Arrange
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Мокируем поведение FileService
            when(fileService.getFile(user, "test.txt")).thenThrow(new RuntimeException("File not found"));

            // Act & Assert
            mockMvc.perform(get("/cloud/file")
                            .param("filename", "test.txt")
                            .header("auth-token", "valid-token"))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test
    public void testGetFileList() throws Exception {
        // Arrange
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Мокируем поведение FileService
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("filename", "test.txt");
            fileInfo.put("size", 1234L);
            fileInfo.put("lastModified", 0L);
            when(fileService.getFileList(user, 10)).thenReturn(List.of(fileInfo));

            // Act & Assert
            mockMvc.perform(get("/cloud/list")
                            .param("limit", "10")
                            .header("auth-token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].filename").value("test.txt"))
                    .andExpect(jsonPath("$[0].size").value(1234));

            // Проверяем, что метод getFileList был вызван
            verify(fileService, times(1)).getFileList(user, 10);
        }
    }
    @Test
    public void testRenameFile() throws Exception {
        // Arrange
        try (MockedStatic<TokenStorage> mockedStatic = mockStatic(TokenStorage.class)) {
            mockedStatic.when(() -> TokenStorage.getUserByToken("valid-token")).thenReturn("testUser");

            // Мокируем поведение UserService
            User user = new User();
            user.setEmail("testUser");
            when(userService.findByLogin("testUser")).thenReturn(user);

            // Мокируем поведение FileService
            doNothing().when(fileService).renameFile(user, "old.txt", "new.txt");

            // Act & Assert
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("filename", "new.txt");

            mockMvc.perform(put("/cloud/file")
                            .param("filename", "old.txt")
                            .header("auth-token", "valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"filename\": \"new.txt\"}"))
                    .andExpect(status().isOk());

            // Проверяем, что метод renameFile был вызван
            verify(fileService, times(1)).renameFile(user, "old.txt", "new.txt");
        }
    }
}
