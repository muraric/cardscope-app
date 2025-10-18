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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
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
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset/" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom("noreply@shomuran.com", "CardScope Support");
            helper.setSubject("Password Reset Request – CardScope");

            // --- HTML template ---
            String html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8" />
          <title>Password Reset</title>
        </head>
        <body style="font-family: Arial, sans-serif; background-color: #f5f6fa; margin:0; padding:0;">
          <table align="center" width="100%%" style="max-width:600px; background:white; border-radius:8px; padding:30px;">
            <tr>
              <td style="text-align:center; padding-bottom:20px;">
                <img src="https://cardscope-web.vercel.app/logo.png" alt="CardScope" height="48" style="margin-bottom:10px;" />
                <h2 style="color:#1e1e2f; margin:0;">Password Reset Request</h2>
              </td>
            </tr>
            <tr>
              <td style="color:#333; font-size:15px; line-height:1.6;">
                <p>Hello %s,</p>
                <p>We received a password reset request for your CardScope account.</p>
                <p>If it was you, click the button below to set a new password:</p>
                <p style="text-align:center; margin:30px 0;">
                  <a href="%s"
                     style="background-color:#2563EB; color:white; padding:12px 24px; text-decoration:none; border-radius:6px; font-weight:bold; display:inline-block;">
                    Reset Password
                  </a>
                </p>
                <p>This link will expire in <b>30 minutes</b>.</p>
                <p>If you didn’t request this, you can safely ignore this email.</p>
                <p>Thanks,<br/>The CardScope Team</p>
              </td>
            </tr>
          </table>
          <p style="text-align:center; font-size:12px; color:#777; margin-top:16px;">
            CardScope by Shomuran Services• Weston, FL 33327
          </p>
        </body>
        </html>
        """.formatted(user.getName(), resetLink);

            helper.setText(html, true); // true => HTML content
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send reset email", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
