package com.graminsaathi.service;

import com.graminsaathi.dto.request.LoginRequest;
import com.graminsaathi.dto.request.SignupRequest;
import com.graminsaathi.dto.response.AuthResponse;
import com.graminsaathi.model.User;
import com.graminsaathi.repository.UserRepository;
import com.graminsaathi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getEmail());

        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = jwtUtil.generateToken(userDetails.getUsername());
        User user = (User) userDetails;

        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail());
    }
}