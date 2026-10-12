package com.example.todo.dto;

public record AuthResponse(String token, String tokenType, String username, String email) {
    public static AuthResponse of(String token, String username, String email) {
        return new AuthResponse(token, "Bearer", username, email);
    }
}
