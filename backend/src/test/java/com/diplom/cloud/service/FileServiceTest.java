package com.diplom.cloud.service;

import com.diplom.cloud.Exceptions.FileUploadException;
import com.diplom.cloud.entity.File;
import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.FileRepository;
import com.diplom.cloud.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class FileServiceTest {
    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileService fileService;
    private User testUser;
    private File testFile;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setEmail("testUser");

        testFile = new File();
        testFile.setFilename("test.txt");
        testFile.setSize(1234L);
        testFile.setUser(testUser);
    }

    @Test
    public void testUploadFile_Success() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        when(fileRepository.save(any(File.class))).thenReturn(testFile);

        // Act
        fileService.uploadFile(testUser, "test.txt", mockFile);

        // Assert
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    public void testUploadFile_FileUploadException() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        doThrow(new RuntimeException("Failed to save file")).when(fileRepository).save(any(File.class));

        // Act & Assert
        assertThrows(FileUploadException.class, () -> fileService.uploadFile(testUser, "test.txt", mockFile));
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    public void testGetFile_Success() {
        // Arrange
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(testFile);

        // Act
        java.io.File result = fileService.getFile(testUser, "test.txt");

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("test.txt", result.getName());
        verify(fileRepository, times(1)).findByUserAndFilename(testUser, "test.txt");
    }

    @Test
    public void testGetFile_NotFound() {
        // Arrange
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> fileService.getFile(testUser, "test.txt"));
        verify(fileRepository, times(1)).findByUserAndFilename(testUser, "test.txt");
    }

    @Test
    public void testGetFileList_Success() {
        // Arrange
        when(fileRepository.findByUser(testUser, PageRequest.of(0, 10))).thenReturn(Page.empty());

        // Act
        List<Map<String, Object>> result = fileService.getFileList(testUser, 10);

        // Assert
        Assertions.assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileRepository, times(1)).findByUser(testUser, PageRequest.of(0, 10));
    }

    @Test
    public void testRenameFile_NotFound() {
        // Arrange
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> fileService.renameFile(testUser, "old.txt", "new.txt"));
        verify(fileRepository, times(1)).findByUserAndFilename(testUser, "old.txt");
        verify(fileRepository, times(0)).save(any(File.class));
    }
}
