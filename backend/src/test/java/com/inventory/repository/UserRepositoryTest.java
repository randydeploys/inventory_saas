package com.inventory.repository;

import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserRepository userRepository;

    private Tenant tenant;
    private Tenant otherTenant;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder().name("Acme").slug("acme").build());
        otherTenant = tenantRepository.save(Tenant.builder().name("Beta").slug("beta").build());

        activeUser = userRepository.save(User.builder()
                .tenant(tenant)
                .email("admin@acme.com")
                .passwordHash("hashed")
                .firstName("Alice")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build());

        inactiveUser = userRepository.save(User.builder()
                .tenant(tenant)
                .email("old@acme.com")
                .passwordHash("hashed")
                .firstName("Bob")
                .lastName("Old")
                .role(Role.READER)
                .active(false)
                .build());
    }

    @Test
    void findByEmailAndTenant_returnsUser_whenExistsAndMatchesTenant() {
        Optional<User> result = userRepository.findByEmailAndTenant("admin@acme.com", tenant);
        assertThat(result).isPresent();
    }

    @Test
    void findByEmailAndTenant_returnsEmpty_whenWrongTenant() {
        Optional<User> result = userRepository.findByEmailAndTenant("admin@acme.com", otherTenant);
        assertThat(result).isEmpty();
    }

    @Test
    void findByTenantAndActiveTrue_excludesDeactivatedUser() {
        List<User> results = userRepository.findByTenantAndActiveTrue(tenant);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("admin@acme.com");
    }
}