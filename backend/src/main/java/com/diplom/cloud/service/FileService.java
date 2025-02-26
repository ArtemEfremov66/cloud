package com.diplom.cloud.service;

import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.FileRepository;
import com.diplom.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileService {
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;
    private final String fileDirPath = "/app/uploads";

    public File getFile(String email, String filename) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        com.diplom.cloud.entity.File fileEntity = fileRepository.findByUserAndFilename(user, filename);
        if (fileEntity == null) {
            throw new RuntimeException("File not found");
        }
        return new File(fileDirPath + File.separator + filename);
    }

    @Transactional
    public void uploadFile(String email, String filename, MultipartFile file) throws IOException {
        System.out.println("Зашли в FileService uploadFile");
        System.out.println("Имя файла: " + filename);
        System.out.println("Размер файла: " + file.getSize());
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        try {
            // Указываем путь к папке uploads
            File uploadDir = new File(fileDirPath);
            // Создаем папку, если она не существует
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    throw new IOException("Не удалось создать папку: " + fileDirPath);
                }
            }
            // Формируем полный путь к файлу
            String filePath = fileDirPath + File.separator + filename;
            System.out.println("Путь к файлу: " + filePath);

            // Сохраняем файл
            File dest = new File(filePath);
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Не удалось сохранить файл: " + e.getMessage());
        }

        // Сохраняем информацию о файле в базе данных
        try {
            com.diplom.cloud.entity.File fileEntity = new com.diplom.cloud.entity.File();
            System.out.println("Сохраняем информацию в базу данных");
            fileEntity.setFilename(filename);
            fileEntity.setSize(file.getSize());
            fileEntity.setUser(user);
            System.out.println("Имя: " + fileEntity.getFilename() + " Размер: " + fileEntity.getSize());
            fileRepository.save(fileEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Не удалось сохранить файл в базе данных: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteFile(String email, String filename) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        try {
            // Удаляем файл с сервера
            File file = new File(fileDirPath + File.separator + filename);
            if (file.delete()) {
                // Удаляем информацию о файле из базы данных
                fileRepository.deleteByFilenameAndUser(filename, user);
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (Exception e) {
            System.out.println("Не удалось удалить");
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getFileList(String email, int limit) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Page<com.diplom.cloud.entity.File> filePage = fileRepository.findByUser(user, PageRequest.of(0, limit));
        System.out.println("Страничку с файлами создали: " + filePage.toString());
        return filePage.getContent().stream()
                .map(file -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("filename", file.getFilename());
                    fileInfo.put("size", file.getSize());
                    File serverFile = new File(fileDirPath + File.separator + file.getFilename());
                    if (serverFile.exists()) {
                        fileInfo.put("lastModified", serverFile.lastModified());
                    } else {
                        System.out.println("Файл для списка по дате изменения не найден");
                        fileInfo.put("lastModified", 0); // Если файл не найден, используем значение по умолчанию
                    }
                    System.out.println("Передаем список на front: " + fileInfo);
                    return fileInfo;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void renameFile(String email, String oldFilename, String newFilename) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Получаем файл из базы данных
        com.diplom.cloud.entity.File fileEntity = fileRepository.findByUserAndFilename(user, oldFilename);
        if (fileEntity == null) {
            throw new RuntimeException("File not found");
        }

        // Переименовываем файл на сервере
        String oldFilePath = fileDirPath + File.separator + oldFilename;
        String newFilePath = fileDirPath + File.separator + newFilename;

        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);

        if (!oldFile.exists()) {
            throw new RuntimeException("File not found on server");
        }
        if (newFile.exists()) {
            throw new RuntimeException("File with the new name already exists");
        }
        boolean renamed = oldFile.renameTo(newFile);
        if (!renamed) {
            throw new RuntimeException("Failed to rename file");
        }

        // Обновляем имя файла в базе данных
        fileEntity.setFilename(newFilename);
        fileRepository.save(fileEntity);
    }
}
