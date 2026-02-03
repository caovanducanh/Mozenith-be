package com.example.demologin.initializer;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.entity.Permission;
import com.example.demologin.entity.Role;
import com.example.demologin.initializer.components.PermissionRoleInitializer;
import com.example.demologin.repository.PermissionRepository;
import com.example.demologin.repository.RoleRepository;

@DataJpaTest
@Import(PermissionRoleInitializer.class)
public class PermissionRoleInitializerTest {

    @Autowired
    private PermissionRoleInitializer initializer;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @Transactional
    public void upsert_should_create_and_update_permissions_and_merge_roles() {
        // Arrange: pre-create a permission with outdated name and a member role without calendar perms
        permissionRepository.save(new Permission("CALENDAR_READ", "OLD NAME"));
        Role member = Role.builder().name("MEMBER").permissions(new HashSet<>()).build();
        roleRepository.save(member);

        // Act
        initializer.initializePermissionsAndRoles();

        // Assert: permission name updated
        Permission p = permissionRepository.findByCode("CALENDAR_READ").orElseThrow();
        assertEquals("Xem calendar / Lấy token calendar", p.getName());

        // Assert: member role has calendar permission merged
        Role memberReload = roleRepository.findByName("MEMBER").orElseThrow();
        assertTrue(memberReload.getPermissions().stream().anyMatch(pp -> pp.getCode().equals("CALENDAR_READ")));
    }
}
