package vn.iotstar.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 500, message = "Tên tối đa 500 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải >= 0")
    @Digits(integer = 16, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal price;

    private String imageUrl;

    private Long userId;
    private String username;
}
