package com.diplom.cloud.repository;

import com.diplom.cloud.entity.File;
import com.diplom.cloud.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
    Page<File> findByUser(User user, Pageable pageable);

    File findByUserAndFilename(User user, String filename);

    void deleteByFilenameAndUser(String filename, User user);
}
