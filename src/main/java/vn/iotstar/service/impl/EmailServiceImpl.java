package vn.iotstar.service.impl;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtp(String email, String otp, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText("""
                Xin chào,

                Mã OTP của bạn là: %s

                OTP có hiệu lực trong 5 phút và chỉ sử dụng một lần.
                Không chia sẻ mã này cho người khác.
                """.formatted(otp));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Gửi email OTP thất bại tới {}", email, e);
            throw new IllegalStateException("Không gửi được email. Kiểm tra cấu hình SMTP (MAIL_USERNAME/MAIL_PASSWORD).", e);
        }
    }
}
