package com.naturalfoliage.lab.controller;

import com.naturalfoliage.lab.model.Role;
import com.naturalfoliage.lab.model.User;
import com.naturalfoliage.lab.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public record UserRequest(@NotBlank String employeeId, @NotBlank String fullName,
        @NotBlank String username, @Size(min = 8) String password,
        Role role, String labSection, boolean active) {}

    public record UserResponse(Long id, String employeeId, String fullName, String username,
        Role role, String labSection, boolean active) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmployeeId(), user.getFullName(),
                user.getUsername(), user.getRole(), user.getLabSection(), user.isActive());
        }
    }

    public record StatusRequest(boolean active) {}

    @GetMapping
    public List<UserResponse> all() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest input) {
        if (users.findByUsername(input.username()).isPresent()) {
            throw new IllegalStateException("Username already exists");
        }
        var user = new User();
        apply(user, input, true);
        return UserResponse.from(users.save(user));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest input) {
        var user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        apply(user, input, false);
        return UserResponse.from(users.save(user));
    }

    @PatchMapping("/{id}/status")
    public UserResponse status(@PathVariable Long id, @RequestBody StatusRequest input) {
        var user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if ("admin".equals(user.getUsername()) && !input.active()) {
            throw new IllegalStateException("The main admin account cannot be deactivated");
        }
        user.setActive(input.active());
        return UserResponse.from(users.save(user));
    }

    private void apply(User user, UserRequest input, boolean passwordRequired) {
        if (passwordRequired && (input.password() == null || input.password().isBlank())) {
            throw new IllegalArgumentException("Password is required");
        }
        user.setEmployeeId(input.employeeId().trim());
        user.setFullName(input.fullName().trim());
        user.setUsername(input.username().trim());
        user.setRole(input.role() == null ? Role.TECHNICIAN : input.role());
        user.setLabSection(input.labSection());
        user.setActive(input.active());
        if (input.password() != null && !input.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(input.password()));
        }
    }
}
