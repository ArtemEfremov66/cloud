package com.diplom.cloud.controller;

import com.diplom.cloud.entity.User;
import com.diplom.cloud.login.LoginRequest;
import com.diplom.cloud.repository.UserRepository;
import com.diplom.cloud.service.UserService;
import com.diplom.cloud.token.TokenStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cloud")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    private static final Logger logger = LogManager.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Аутентификация пользователя {} ", loginRequest.getLogin());
            //Добавляем пользователя для аутентификации--
//            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//            String hashedPassword = passwordEncoder.encode("password");
//            User user = new User();
//            user.setEmail("artem@mail.ru");
//            user.setPassword(hashedPassword);
//            userRepository.save(user);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getLogin(), loginRequest.getPassword()));
            logger.info("Аутентификация пройдена");

            // Генерируем токен
            String token = TokenStorage.generateToken(loginRequest.getLogin());
            String email = loginRequest.getLogin();
            logger.debug("Токен получен: {}", token);

            // Успешный ответ
            Map<String, String> response = new HashMap<>();
            response.put("email", email);
            response.put("auth-token", token);

            // Возвращаем токен в ответе
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) { // Обрабатываем неверные учетные данные
            // Создаем ответ с ошибкой в нужном формате
            Map<String, String[]> errors = new HashMap<>();
            errors.put("email", new String[]{"Неверный email или пароль"});
            errors.put("password", new String[]{"Неверный email или пароль"});

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        TokenStorage.removeToken(token);
        return ResponseEntity.ok().build();
    }
}