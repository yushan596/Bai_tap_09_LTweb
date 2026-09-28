package vn.iotstar.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.ProductDTO;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ProductService;

/**
 * Mọi user đã đăng nhập xem được danh sách sản phẩm.
 * Chỉ chủ sở hữu (hoặc ADMIN) mới được sửa / xóa sản phẩm.
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("products", productService.findAll(keyword, page, size));
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        return "products/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("mode", "create");
        return "products/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute ProductDTO dto,
                         BindingResult result,
                         @RequestParam(required = false) MultipartFile image,
                         @AuthenticationPrincipal CustomUserDetails me,
                         Model model,
                         RedirectAttributes redirect) {
        model.addAttribute("mode", "create");
        if (result.hasErrors()) {
            return "products/form";
        }
        dto.setUserId(me.getId()); // luôn lấy từ session, không tin dữ liệu từ form
        try {
            productService.create(dto, image);
        } catch (IllegalArgumentException e) {
            result.reject("product.error", e.getMessage());
            return "products/form";
        }
        redirect.addFlashAttribute("success", "Tạo sản phẩm thành công.");
        return "redirect:/products";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails me,
                       Model model) {
        ProductDTO dto = productService.findById(id);
        checkOwner(dto, me);
        model.addAttribute("productDTO", dto);
        model.addAttribute("mode", "edit");
        return "products/form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute ProductDTO dto,
                       BindingResult result,
                       @RequestParam(required = false) MultipartFile image,
                       @AuthenticationPrincipal CustomUserDetails me,
                       Model model,
                       RedirectAttributes redirect) {
        ProductDTO current = productService.findById(id);
        checkOwner(current, me);

        model.addAttribute("mode", "edit");
        dto.setId(id);
        dto.setImageUrl(current.getImageUrl()); // để form hiển thị lại ảnh hiện tại khi có lỗi
        if (result.hasErrors()) {
            return "products/form";
        }
        try {
            productService.update(id, dto, image);
        } catch (IllegalArgumentException e) {
            result.reject("product.error", e.getMessage());
            return "products/form";
        }
        redirect.addFlashAttribute("success", "Cập nhật sản phẩm thành công.");
        return "redirect:/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails me,
                         RedirectAttributes redirect) {
        checkOwner(productService.findById(id), me);
        productService.delete(id);
        redirect.addFlashAttribute("success", "Xóa sản phẩm thành công.");
        return "redirect:/products";
    }

    private void checkOwner(ProductDTO product, CustomUserDetails me) {
        if (!me.isAdmin() && !me.getId().equals(product.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền với sản phẩm này");
        }
    }
}
