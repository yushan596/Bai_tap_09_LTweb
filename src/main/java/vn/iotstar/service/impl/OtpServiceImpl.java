package vn.iotstar.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.entity.OtpToken;
import vn.iotstar.repository.OtpTokenRepository;
import vn.iotstar.service.EmailService;
import vn.iotstar.service.OtpService;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_MINUTES = 5;
    /** Chống spam email: tối thiểu 60 giây giữa 2 lần gửi OTP cùng loại */
    private static final int RESEND_SECONDS = 60;

    private final OtpTokenRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    private String generateOtp() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }

    private void send(String email, String type, String subject) {
        repository.findTopByEmailAndTypeOrderByCreatedAtDesc(email, type).ifPresent(last -> {
            if (last.getCreatedAt().plusSeconds(RESEND_SECONDS).isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException(
                        "Vui lòng đợi %d giây rồi hãy gửi lại OTP.".formatted(RESEND_SECONDS));
            }
        });

        repository.deleteByEmailAndType(email, type);

        String otp = generateOtp();
        repository.save(OtpToken.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_MINUTES))
                .attempts(0)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build());

        emailService.sendOtp(email, otp, subject);
    }

    private boolean verify(String email, String otp, String type) {
        OtpToken token = repository
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElse(null);

        if (token == null
                || token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }

        token.setAttempts(token.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            repository.save(token);
            return false;
        }
        token.setUsed(true);
        repository.save(token);
        return true;
    }

    @Override
    @Transactional
    public void sendRegisterOtp(String email) {
        send(email, OtpToken.REGISTER, "Shop - Xác nhận đăng ký tài khoản");
    }

    @Override
    @Transactional
    public boolean verifyRegisterOtp(String email, String otp) {
        return verify(email, otp, OtpToken.REGISTER);
    }

    @Override
    @Transactional
    public void sendResetPasswordOtp(String email) {
        send(email, OtpToken.RESET_PASSWORD, "Shop - OTP đặt lại mật khẩu");
    }

    @Override
    @Transactional
    public boolean verifyResetPasswordOtp(String email, String otp) {
        return verify(email, otp, OtpToken.RESET_PASSWORD);
    }
}
