package com.shomuran.cardscope.service;

import com.shomuran.cardscope.model.PasswordResetToken;
import com.shomuran.cardscope.model.UserProfile;
import com.shomuran.cardscope.repository.PasswordResetTokenRepository;
import com.shomuran.cardscope.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendResetLink(String email) {
        // 1️⃣ Check if user exists
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // 2️⃣ Generate token
        String token = UUID.randomUUID().toString();

        // 3️⃣ Save token in DB
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(resetToken);

        // 4️⃣ Create reset link
        String resetLink = frontendUrl + "/reset/" + token;

        // 5️⃣ Send email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setFrom("noreply@cardscope.app");
        mailMessage.setSubject("Password Reset Request - CardScope");
        mailMessage.setText("""
                Hello %s,
                
                We received a password reset request for your CardScope account.
                Click the link below to reset your password:
                
                %s
                
                This link will expire in 30 minutes.
                
                If you didn’t request this, you can safely ignore this email.
                
                — CardScope Support
                """.formatted(user.getName(), resetLink));

        mailSender.send(mailMessage);
    }

    public void confirmResetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        UserProfile user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userProfileRepository.save(user);

        tokenRepository.delete(resetToken);
    }


}
