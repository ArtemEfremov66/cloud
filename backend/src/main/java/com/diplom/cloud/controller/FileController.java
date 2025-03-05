package com.diplom.cloud.controller;

import com.diplom.cloud.Exceptions.*;
import com.diplom.cloud.entity.User;
import com.diplom.cloud.service.FileService;
import com.diplom.cloud.service.UserService;
import com.diplom.cloud.token.TokenStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cloud")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam("filename") String filename,
                                        @RequestParam("file") MultipartFile file) {
        try {
            String username = TokenStorage.getUserByToken(authToken);
            if (username == null) {
                throw new InvalidTokenException("Invalid token");
            }
            User user = userService.findByLogin(username);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            fileService.uploadFile(user, filename, file);
            return ResponseEntity.ok().build();
        } catch (InvalidTokenException | UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (FileUploadException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam("filename") String filename) {
        try {
            String username = TokenStorage.getUserByToken(authToken);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            User user = userService.findByLogin(username);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            fileService.deleteFile(user, filename);
            return ResponseEntity.ok().build();
        } catch (FileDeleteException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(@RequestHeader("auth-token") String authToken,
                                          @RequestParam("filename") String filename) {
        try {
            String username = TokenStorage.getUserByToken(authToken);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            User user = userService.findByLogin(username);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            File file = fileService.getFile(user, filename);
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(new FileSystemResource(file));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFileList(@RequestHeader("auth-token") String authToken,
                                         @RequestParam("limit") int limit) {
        try {
            String username = TokenStorage.getUserByToken(authToken);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            User user = userService.findByLogin(username);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            List<Map<String, Object>> fileList = fileService.getFileList(user, limit);
            return ResponseEntity.ok(fileList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(@RequestHeader("auth-token") String authToken,
                                        @RequestParam("filename") String filename,
                                        @RequestBody Map<String, String> request) {
        try {
            // Получаем новое имя файла из тела запроса
            String newFilename = request.get("filename");
            if (newFilename == null || newFilename.isEmpty()) {
                return ResponseEntity.badRequest().body("New filename is required");
            }

            // Получаем пользователя по токену
            String username = TokenStorage.getUserByToken(authToken);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            User user = userService.findByLogin(username);
            if (user == null) {
                throw new UserNotFoundException("User not found");
            }

            // Вызываем сервис для переименования файла
            fileService.renameFile(user, filename, newFilename);
            return ResponseEntity.ok().build();
        } catch (FileRenameException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

