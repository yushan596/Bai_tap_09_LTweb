package vn.iotstar.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "nvarchar(500)")
    private String name;

    @Column(columnDefinition = "nvarchar(2000)")
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    /** URL ảnh trên Cloudinary (secure_url) */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /** public_id của ảnh trên Cloudinary, dùng để xóa/thay ảnh */
    @Column(name = "image_public_id")
    private String imagePublicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
