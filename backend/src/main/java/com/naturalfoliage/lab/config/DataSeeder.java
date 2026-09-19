package com.naturalfoliage.lab.config;

import com.naturalfoliage.lab.model.Role;
import com.naturalfoliage.lab.model.User;
import com.naturalfoliage.lab.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataSeeder {
    @Bean CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
        @Value("${ADMIN_PASSWORD:ChangeMe123!}") String adminPassword) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                var admin = new User();
                admin.setUsername("admin"); admin.setFullName("System Administrator");
                admin.setEmployeeId("ADMIN-001"); admin.setLabSection("Administration");
                admin.setPassword(encoder.encode(adminPassword)); admin.setRole(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}
