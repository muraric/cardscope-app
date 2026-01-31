package com.shomuran.cardscope.controller;

import com.shomuran.cardscope.dto.UserProfileDto;
import com.shomuran.cardscope.model.PasswordResetToken;
import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.PasswordResetTokenRepository;
import com.shomuran.cardscope.repository.UserProfileRepository;
import com.shomuran.cardscope.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private UserService userService;
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
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        System.out.println("Reset link sent");
        userService.sendResetLink(email); // Your email logic
        return ResponseEntity.ok(Map.of("message", "Reset link sent"));
    }

    @GetMapping("/reset/{token}")
    public ResponseEntity<?> verifyToken(@PathVariable String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        return ResponseEntity.ok("Token valid");
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<?> confirmReset(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        userService.confirmResetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @PostMapping("/apple")
    public ResponseEntity<?> appleLogin(@RequestBody Map<String, Object> payload) {
        try {
            String identityToken = (String) payload.get("identityToken");
            String appleUserId = (String) payload.get("user");
            String email = (String) payload.get("email");
            String givenName = (String) payload.get("givenName");
            String familyName = (String) payload.get("familyName");

            // Decode the JWT to extract email if not provided
            if (email == null || email.isEmpty()) {
                email = extractEmailFromJwt(identityToken);
            }

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Could not extract email from Apple token"));
            }

            // Build user name
            String name = buildName(givenName, familyName, email);

            // Find or create user
            final String finalEmail = email;
            final String finalName = name;
            UserProfile user = userProfileRepository.findByEmail(email)
                    .orElseGet(() -> {
                        UserProfile newUser = new UserProfile();
                        newUser.setEmail(finalEmail);
                        newUser.setName(finalName);
                        newUser.setProvider("apple");
                        newUser.setProviderId(appleUserId);
                        newUser.setUserCards(new ArrayList<>());
                        return userProfileRepository.save(newUser);
                    });

            // Update provider info if user exists but was registered differently
            if (user.getProvider() == null || !user.getProvider().equals("apple")) {
                user.setProvider("apple");
                user.setProviderId(appleUserId);
                userProfileRepository.save(user);
            }

            String token = UUID.randomUUID().toString();

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName() != null ? user.getName() : ""
                    )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Apple authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, Object> payload) {
        try {
            String idToken = (String) payload.get("idToken");
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) payload.get("user");

            String email = userInfo != null ? (String) userInfo.get("email") : null;
            String googleId = userInfo != null ? (String) userInfo.get("id") : null;
            String givenName = userInfo != null ? (String) userInfo.get("givenName") : null;
            String familyName = userInfo != null ? (String) userInfo.get("familyName") : null;

            // If email not in user object, try to extract from token
            if (email == null || email.isEmpty()) {
                email = extractEmailFromJwt(idToken);
            }

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Could not extract email from Google token"));
            }

            String name = buildName(givenName, familyName, email);

            final String finalEmail = email;
            final String finalName = name;
            final String finalGoogleId = googleId;

            UserProfile user = userProfileRepository.findByEmail(email)
                    .orElseGet(() -> {
                        UserProfile newUser = new UserProfile();
                        newUser.setEmail(finalEmail);
                        newUser.setName(finalName);
                        newUser.setProvider("google");
                        newUser.setProviderId(finalGoogleId);
                        newUser.setUserCards(new ArrayList<>());
                        return userProfileRepository.save(newUser);
                    });

            // Update provider info if needed
            if (user.getProvider() == null || !user.getProvider().equals("google")) {
                user.setProvider("google");
                user.setProviderId(googleId);
                userProfileRepository.save(user);
            }

            String token = UUID.randomUUID().toString();

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getName() != null ? user.getName() : ""
                    )
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }

    private String extractEmailFromJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            return (String) claims.get("email");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildName(String givenName, String familyName, String email) {
        StringBuilder nameBuilder = new StringBuilder();
        if (givenName != null && !givenName.isEmpty()) {
            nameBuilder.append(givenName);
        }
        if (familyName != null && !familyName.isEmpty()) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(familyName);
        }
        if (nameBuilder.length() == 0 && email != null) {
            // Use part before @ as name
            nameBuilder.append(email.split("@")[0]);
        }
        return nameBuilder.toString();
    }
}
