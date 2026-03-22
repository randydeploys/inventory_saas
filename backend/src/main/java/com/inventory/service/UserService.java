package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.CreateUserRequest;
import com.inventory.model.dto.UserResponse;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.Role;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper securityHelper;

    public UserService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            SecurityHelper securityHelper
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityHelper = securityHelper;
    }

    public List<UserResponse> getAll() {
        UUID tenantId = securityHelper.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        return userRepository.findByTenant(tenant).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        User user = userRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return toResponse(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        if (userRepository.existsByEmailAndTenant(request.email(), tenant)) {
            throw new ConflictException("Un utilisateur avec cet email existe déjà");
        }

        Role role;
        try {
            role = Role.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide : " + request.role());
        }

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(UUID id, String newRole) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        UUID currentUserId = securityHelper.getCurrentUserId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        User user = userRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (user.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas modifier votre propre rôle");
        }

        Role role;
        try {
            role = Role.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide : " + newRole);
        }

        user.setRole(role);
        user = userRepository.save(user);

        return toResponse(user);
    }

    @Transactional
    public void deactivate(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        UUID currentUserId = securityHelper.getCurrentUserId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        User user = userRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (user.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isActive(),
                user.getTenant().getId()
        );
    }
}