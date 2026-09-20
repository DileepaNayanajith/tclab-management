package com.naturalfoliage.lab.config;

import com.naturalfoliage.lab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.List;
import java.util.ArrayList;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService users(UserRepository repository) {
        return username -> repository.findByUsername(username)
            .map(user -> {
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                user.getPermissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                return User.withUsername(user.getUsername()).password(user.getPassword())
                    .authorities(authorities).disabled(!user.isActive()).build();
            })
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/robots.txt", "/error").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) ->
                response.sendError(401, "Unauthorized")))
            .build();
    }

    @Bean CorsConfigurationSource cors(@Value("${app.cors.allowed-origin}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
