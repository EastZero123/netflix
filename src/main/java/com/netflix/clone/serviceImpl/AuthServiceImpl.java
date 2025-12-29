package com.netflix.clone.serviceImpl;

import com.netflix.clone.dao.UserRepository;
import com.netflix.clone.dto.request.UserRequest;
import com.netflix.clone.dto.response.EmailValidationResponse;
import com.netflix.clone.dto.response.LoginResponse;
import com.netflix.clone.dto.response.MessageResponse;
import com.netflix.clone.entity.User;
import com.netflix.clone.enums.Role;
import com.netflix.clone.exception.*;
import com.netflix.clone.security.JwtUtil;
import com.netflix.clone.service.AuthService;
import com.netflix.clone.service.EmailService;
import com.netflix.clone.util.ServiceUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private EmailService emailService;

    private JwtUtil jwtUtil;

    private ServiceUtils serviceUtils;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtUtil jwtUtil, ServiceUtils serviceUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.serviceUtils = serviceUtils;
    }

    @Override
    public MessageResponse signup(UserRequest userRequest) {
        if(userRepository.existsByEmail(userRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setFullName(userRequest.getFullName());
        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(86400));
        userRepository.save(user);
        emailService.sendVerificationEmail(userRequest.getEmail(), verificationToken);

        return new MessageResponse("User registered successfully!");
    }

    @Override
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials!"));

        if(!user.isActive()) {
            throw new AccountDeactivatedException("Account deactivated!");
        }

        if(!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified!");
        }

        final String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @Override
    public EmailValidationResponse validateEmail(String email) {
        boolean exists = userRepository.existsByEmail(email);
        return new EmailValidationResponse(exists, !exists);
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token!"));

        if(user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token expired!");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Email verified successfully!");
    }

    @Override
    public MessageResponse resendVerification(String email) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(86400));
        userRepository.save(user);
        emailService.sendVerificationEmail(email, verificationToken);
        return new MessageResponse("Verification email resent successfully!");
    }

    @Override
    public MessageResponse forgotPassword(String email) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(Instant.now().plusSeconds(86400));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(email, resetToken);

        return new MessageResponse("Password reset email sent successfully!");
    }

    @Override
    public MessageResponse resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token!"));

        if(user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new InvalidTokenException("Token expired!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return new MessageResponse("Password reset successfully!");
    }

    @Override
    public MessageResponse changePassword(String email, String currentPassword, String newPassword) {
        User user = serviceUtils.getUserByEmailOrThrow(email);

        if(!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid current password!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return new MessageResponse("Password changed successfully!");
    }

    @Override
    public LoginResponse currentUser(String email) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        return new LoginResponse(null, user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
