package com.diplom.cloud.login;

public class LoginResponse {
    private String email;
    private String authToken;


    public LoginResponse(String email, String authToken) {
        this.authToken = authToken;
        this.email = email;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
