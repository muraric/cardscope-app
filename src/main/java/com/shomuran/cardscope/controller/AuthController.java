package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    @Autowired
    private UserProfileRepository userProfileRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String name = payload.get("name");

        if (userProfileRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
        }

        UserProfile  user = new UserProfile();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserCards(new ArrayList<>());

        userProfileRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        return userProfileRepository.findByEmail(email)
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPasswordHash())) {
                        // For now return a dummy token; later replace with JWT
                        String token = UUID.randomUUID().toString();
                        return ResponseEntity.ok(Map.of(
                                "token", token,
                                "email", user.getEmail(),
                                "name", user.getName()
                        ));
                    } else {
                        return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
    }
}
