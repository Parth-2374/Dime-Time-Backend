package com.dimetime.dto;

public class LoginResponse {
    private String message;
    private String username;
    private String role;
    private String fullName;
    private String companyName;
    private String token;
    private Long id;

    public LoginResponse() {
    }

    public LoginResponse(String message, String username, String role, String fullName, String companyName) {
        this.message = message;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.companyName = companyName;
    }

    public LoginResponse(String message, String username, String role, String fullName, String companyName, String token) {
        this.message = message;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.companyName = companyName;
        this.token = token;
    }

    public LoginResponse(String message, String username, String role, String fullName, String companyName, String token, Long id) {
        this.message = message;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.companyName = companyName;
        this.token = token;
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
