package vn.iotstar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // POST /login do Spring Security xử lý
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
