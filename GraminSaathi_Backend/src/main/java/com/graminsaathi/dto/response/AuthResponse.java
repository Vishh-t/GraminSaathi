package com.graminsaathi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String fullName;
    private String email;

    public AuthResponse(String token, Long id, String fullName, String email) {
        this.token = token;
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }
}