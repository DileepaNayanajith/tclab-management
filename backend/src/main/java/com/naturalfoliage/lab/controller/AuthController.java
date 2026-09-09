package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api")
public class AuthController {
    private final UserRepository users;
    public AuthController(UserRepository users) { this.users = users; }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status", "UP"); }
    @GetMapping("/me") public Map<String,Object> me(Authentication auth) {
        var user = users.findByUsername(auth.getName()).orElseThrow();
        return Map.of("username", user.getUsername(), "fullName", user.getFullName(),
            "employeeId", user.getEmployeeId(), "labSection", user.getLabSection(), "role", user.getRole());
    }
}
