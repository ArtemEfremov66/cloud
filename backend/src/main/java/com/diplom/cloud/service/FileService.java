package com.diplom.cloud.service;

import com.diplom.cloud.Exceptions.FileDeleteException;
import com.diplom.cloud.Exceptions.FileNotFoundException;
import com.diplom.cloud.Exceptions.FileRenameException;
import com.diplom.cloud.Exceptions.FileUploadException;
import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.FileRepository;
import com.diplom.cloud.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger logger = LogManager.getLogger(FileService.class);


    //    private final String fileDirPath = "C:\\JavaPrograms\\Diplom\\cloud";
    private final String fileDirPath = "/app/uploads";

    public File getFile(User user, String filename) {
        com.diplom.cloud.entity.File fileEntity = fileRepository.findByUserAndFilename(user, filename);
        if (fileEntity == null) {
            throw new RuntimeException("File not found");
        }
        return new File(fileDirPath + File.separator + filename);
    }

    @Transactional
    public void uploadFile(User user, String filename, MultipartFile file) throws FileUploadException {
        try {
            // Указываем путь к папке uploads
            File uploadDir = new File(fileDirPath);
            // Создаем папку, если она не существует
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    throw new FileUploadException("Не удалось создать папку: " + fileDirPath);
                }
            }
            // Сохраняем информацию о файле в базе данных
            com.diplom.cloud.entity.File fileEntity = new com.diplom.cloud.entity.File();
            fileEntity.setFilename(filename);
            fileEntity.setSize(file.getSize());
            fileEntity.setUser(user);
            fileRepository.save(fileEntity);

            // Формируем полный путь к файлу
            String filePath = fileDirPath + File.separator + filename;

            // Сохраняем файл
            File dest = new File(filePath);
            file.transferTo(dest);
        } catch (IOException e) {
            throw new FileUploadException("Не удалось сохранить файл: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FileUploadException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteFile(User user, String filename) throws FileDeleteException {
        try {
            // Удаляем файл с сервера
            File file = new File(fileDirPath + File.separator + filename);
            if (!file.exists()) {
                throw new FileNotFoundException("Файл не найден: " + filename);
            }
            if (!file.delete()) {
                throw new FileDeleteException("Не удалось удалить файл: " + filename);
            }
            // Удаляем информацию о файле из базы
            fileRepository.deleteByFilenameAndUser(filename, user);
        } catch (FileNotFoundException e) {
            throw new FileDeleteException(e.getMessage());
        } catch (Exception e) {
            throw new FileDeleteException("Ошибка при удалении файла: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getFileList(User user, int limit) {
        Page<com.diplom.cloud.entity.File> filePage = fileRepository.findByUser(user, PageRequest.of(0, limit));
        return filePage.getContent().stream()
                .map(file -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("filename", file.getFilename());
                    fileInfo.put("size", file.getSize());
                    File serverFile = new File(fileDirPath + File.separator + file.getFilename());
                    if (serverFile.exists()) {
                        fileInfo.put("lastModified", serverFile.lastModified());
                    } else {
                        fileInfo.put("lastModified", 0); // Если файл не найден, используем значение по умолчанию
                    }
                    System.out.println("Передаем список на front: " + fileInfo);
                    return fileInfo;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void renameFile(User user, String oldFilename, String newFilename) throws FileRenameException {
        try {
            // Получаем файл из базы данных
            com.diplom.cloud.entity.File fileEntity = fileRepository.findByUserAndFilename(user, oldFilename);
            if (fileEntity == null) {
                throw new FileNotFoundException("Файл не найден: " + oldFilename);
            }
            // Проверяем, существует ли файл с новым именем
            if (fileRepository.findByUserAndFilename(user, newFilename) != null) {
                throw new FileRenameException("Файл с именем " + newFilename + " уже существует");
            }

            String oldFilePath = fileDirPath + File.separator + oldFilename;
            String newFilePath = fileDirPath + File.separator + newFilename;

            // Обновляем имя файла в базе данных
            fileEntity.setFilename(newFilename);
            fileRepository.save(fileEntity);

            // Переименовываем файл на сервере
            File oldFile = new File(oldFilePath);
            File newFile = new File(newFilePath);

            if (!oldFile.renameTo(newFile)) {
                throw new FileRenameException("Не удалось переименовать файл: " + oldFilename);
            }
        } catch (FileNotFoundException e) {
            throw new FileRenameException(e.getMessage());
        } catch (Exception e) {
            throw new FileRenameException("Ошибка при переименовании файла: " + e.getMessage());
        }
    }
}

