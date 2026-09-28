package vn.iotstar.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.UserDTO;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.UserService;
import vn.iotstar.service.impl.UserServiceImpl;

/** Quản lý user - chỉ ROLE_ADMIN (khai báo trong SecurityConfig: /users/**). */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("users", userService.findAll(keyword, page, size));
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        return "users/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        UserDTO dto = new UserDTO();
        dto.setEnabled(true);
        dto.setRoleName("ROLE_USER");
        model.addAttribute("userDTO", dto);
        model.addAttribute("mode", "create");
        return "users/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute UserDTO dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        model.addAttribute("mode", "create");
        if (result.hasErrors()) {
            return "users/form";
        }
        try {
            userService.create(dto);
        } catch (IllegalArgumentException e) {
            result.reject("user.error", e.getMessage());
            return "users/form";
        }
        redirect.addFlashAttribute("success",
                "Tạo user thành công. Mật khẩu mặc định: " + UserServiceImpl.DEFAULT_PASSWORD);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("userDTO", userService.findById(id));
        model.addAttribute("mode", "edit");
        return "users/form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute UserDTO dto,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirect) {
        model.addAttribute("mode", "edit");
        dto.setId(id);
        if (result.hasErrors()) {
            return "users/form";
        }
        try {
            userService.update(id, dto);
        } catch (IllegalArgumentException e) {
            result.reject("user.error", e.getMessage());
            return "users/form";
        }
        redirect.addFlashAttribute("success", "Cập nhật user thành công.");
        return "redirect:/users";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails me,
                         RedirectAttributes redirect) {
        if (id.equals(me.getId())) {
            redirect.addFlashAttribute("error", "Không thể tự xóa tài khoản đang đăng nhập.");
            return "redirect:/users";
        }
        try {
            userService.delete(id);
            redirect.addFlashAttribute("success", "Xóa user thành công.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
}
