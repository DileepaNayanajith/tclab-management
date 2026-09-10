package com.naturalfoliage.lab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String username;
    @JsonIgnore @Column(nullable = false) private String password;
    @Column(nullable = false) private String fullName;
    @Column(nullable = false, unique = true) private String employeeId;
    private String labSection;
    @Column(nullable = false) private boolean active = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", nullable = false)
    private Set<String> permissions = new LinkedHashSet<>();
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getLabSection() { return labSection; }
    public void setLabSection(String labSection) { this.labSection = labSection; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissions);
    }
}
