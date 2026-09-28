package vn.iotstar.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // @EntityGraph nạp luôn user (tránh N+1) mà vẫn phân trang/đếm đúng (khác với "join fetch")
    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from Product p
            where lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%'))
            """)
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    java.util.Optional<Product> findWithUserById(Long id);

    long countByUserId(Long userId);

    /** Đếm product cho nhiều user trong 1 câu query: [userId, count] */
    @Query("select p.user.id, count(p) from Product p where p.user.id in :ids group by p.user.id")
    List<Object[]> countByUserIds(@Param("ids") Collection<Long> ids);
}
