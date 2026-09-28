package vn.iotstar.service.impl;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.RegisterDTO;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.AuthService;
import vn.iotstar.service.OtpService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_AVATAR = "/images/user.png";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        String email = normalize(dto.getEmail());

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không đúng");
        }

        User pending = userRepository.findByEmail(email).orElse(null);
        // Email đã kích hoạt => từ chối. Email đăng ký dở (chưa nhập OTP) => cho đăng ký lại, ghi đè bản ghi cũ.
        if (pending != null && pending.isEnabled()) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        boolean usernameTaken = userRepository.findByUsername(dto.getUsername())
                .filter(u -> pending == null || !u.getId().equals(pending.getId()))
                .isPresent();
        if (usernameTaken) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Chưa có ROLE_USER"));

        User user = pending != null
                ? pending
                : User.builder().role(role).images(DEFAULT_AVATAR).build();
        user.setUsername(dto.getUsername());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setEnabled(false);
        userRepository.save(user);

        otpService.sendRegisterOtp(email);
    }

    @Override
    @Transactional
    public void resendRegisterOtp(String email) {
        email = normalize(email);
        // Chỉ gửi lại cho tài khoản đang chờ kích hoạt (tránh bị lợi dụng để spam email người khác)
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isEnabled())
                .orElseThrow(() -> new IllegalArgumentException("Không có tài khoản nào đang chờ xác nhận với email này"));
        otpService.sendRegisterOtp(user.getEmail());
    }

    @Override
    @Transactional
    public boolean verifyRegister(String email, String otp) {
        email = normalize(email);
        if (!otpService.verifyRegisterOtp(email, otp)) {
            return false;
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        user.setEnabled(true);
        return true;
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        email = normalize(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Tài khoản chưa được xác nhận email");
        }
        otpService.sendResetPasswordOtp(email);
    }

    @Override
    public boolean verifyResetOtp(String email, String otp) {
        return otpService.verifyResetPasswordOtp(normalize(email), otp);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String password) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        user.setPassword(passwordEncoder.encode(password));
    }
}
