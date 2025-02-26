package com.diplom.cloud.service;

import com.diplom.cloud.entity.User;
import com.diplom.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User findByLogin(String login) {
        return userRepository.findByEmail(login);
    }
}
