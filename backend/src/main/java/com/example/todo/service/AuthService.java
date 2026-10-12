package com.example.todo.service;

import com.example.todo.dto.AuthResponse;
import com.example.todo.dto.LoginRequest;
import com.example.todo.dto.RegisterRequest;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import com.example.todo.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        return AuthResponse.of(jwtUtil.generateToken(user.getUsername()), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User logged in: {}", user.getUsername());
        return AuthResponse.of(jwtUtil.generateToken(user.getUsername()), user.getUsername(), user.getEmail());
    }
}
