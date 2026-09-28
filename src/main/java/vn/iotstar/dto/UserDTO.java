package vn.iotstar.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;

    // Không cho ký tự '@' để username không thể trùng với email của người khác (login bằng username hoặc email)
    @NotBlank(message = "Username không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$",
            message = "Username 3-50 ký tự, chỉ gồm chữ, số, dấu . _ -")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String images;
    private String roleName;
    private boolean enabled;

    /** Số sản phẩm của user (chỉ để hiển thị) */
    private long productCount;
}
