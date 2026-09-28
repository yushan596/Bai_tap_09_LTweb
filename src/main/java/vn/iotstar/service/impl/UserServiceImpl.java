package vn.iotstar.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.UserDTO;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.mapper.UserMapper;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** Mật khẩu mặc định cho user do Admin tạo (hiển thị trên form để admin biết) */
    public static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_AVATAR = "/images/user.png";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));

        Page<User> users = userRepository.search(keyword == null ? "" : keyword.trim(), pageable);
        Map<Long, Long> counts = countProductsByUser(users.getContent().stream().map(User::getId).toList());

        return users.map(u -> {
            UserDTO dto = userMapper.toDTO(u);
            dto.setProductCount(counts.getOrDefault(u.getId(), 0L));
            return dto;
        });
    }

    private Map<Long, Long> countProductsByUser(Collection<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        List<Object[]> rows = productRepository.countByUserIds(ids);
        for (Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + id));
        UserDTO dto = userMapper.toDTO(user);
        dto.setProductCount(productRepository.countByUserId(id));
        return dto;
    }

    @Override
    @Transactional
    public UserDTO create(UserDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = userMapper.toEntity(dto);
        user.setEmail(email);
        user.setRole(findRole(dto.getRoleName()));
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setEnabled(dto.isEnabled());
        if (user.getImages() == null || user.getImages().isBlank()) {
            user.setImages(DEFAULT_AVATAR);
        }
        return userMapper.toDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO update(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + id));

        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        user.setUsername(dto.getUsername());
        user.setEmail(email);
        user.setFullName(dto.getFullName());
        user.setEnabled(dto.isEnabled());
        if (dto.getRoleName() != null && !dto.getRoleName().isBlank()) {
            user.setRole(findRole(dto.getRoleName()));
        }

        UserDTO result = userMapper.toDTO(userRepository.save(user));
        result.setProductCount(productRepository.countByUserId(id));
        return result;
    }

    private Role findRole(String roleName) {
        String name = roleName == null || roleName.isBlank() ? "ROLE_USER" : roleName;
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + name));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + id));
        long products = productRepository.countByUserId(id);
        if (products > 0) {
            throw new IllegalArgumentException(
                    "User '%s' còn %d sản phẩm. Hãy xóa các sản phẩm trước.".formatted(user.getUsername(), products));
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts(Long userId) {
        return productRepository.countByUserId(userId);
    }
}
