package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang nhỏ để kiểm tra rule hasRole("ADMIN") trong SecurityConfig. */
@Controller
public class AdminController {

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/index";
    }
}
