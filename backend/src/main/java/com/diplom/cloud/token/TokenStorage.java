package com.diplom.cloud.token;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenStorage {

    private static final Map<String, String> tokenToUserMap = new HashMap<>();
    private static final Logger logger = LogManager.getLogger(TokenStorage.class);

    public static String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        tokenToUserMap.put("Bearer " + token, username);
        return token;
    }

    public static boolean isValidToken(String token) {
        logger.debug("Проверяем валидность, пришел токен: {}", token);
        logger.debug("В мапе найден пользователь по токену: {}", tokenToUserMap.get(token));
        return tokenToUserMap.containsKey(token);
    }

    public static String getUserByToken(String token) {
        return tokenToUserMap.get(token);
    }

    public static void removeToken(String token) {
        String username = tokenToUserMap.get(token);
        if (username != null) {
            tokenToUserMap.remove(token);
        }
    }
}