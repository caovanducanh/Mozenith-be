package com.example.demologin.initializer.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.entity.Permission;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.PermissionRepository;
import com.example.demologin.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Permission and Role Initializer
 *
 * Responsible for creating all system permissions and roles.
 * This must run before user initialization since users depend on roles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionRoleInitializer {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    // ===================== PERMISSION CODES =====================
    private static final String USER_MANAGE = "USER_MANAGE";  // mới thêm
    private static final String USER_TOKEN_MANAGEMENT = "USER_TOKEN_MANAGEMENT";
    private static final String TOKEN_INVALIDATE_OWN = "TOKEN_INVALIDATE_OWN";
    private static final String TOKEN_INVALIDATE_USER = "TOKEN_INVALIDATE_USER";
    private static final String TOKEN_VIEW_OWN = "TOKEN_VIEW_OWN";
    private static final String TOKEN_VIEW_USER = "TOKEN_VIEW_USER";

    private static final String ROLE_VIEW = "ROLE_VIEW";
    private static final String ROLE_CREATE = "ROLE_CREATE";
    private static final String ROLE_UPDATE = "ROLE_UPDATE";
    private static final String ROLE_DELETE = "ROLE_DELETE";
    private static final String ROLE_UPDATE_PERMISSIONS = "ROLE_UPDATE_PERMISSIONS";

    private static final String PERMISSION_VIEW = "PERMISSION_VIEW";
    private static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";

    private static final String LOG_VIEW_ACTIVITY = "LOG_VIEW_ACTIVITY";
    private static final String ADMIN_ACTIVITY_LOG_EXPORT = "ADMIN_ACTIVITY_LOG_EXPORT";
    private static final String LOG_DELETE = "LOG_DELETE";

    private static final String ADMIN_TRANSACTION_VIEW = "ADMIN_TRANSACTION_VIEW";

    private static final String USER_VIEW_OWN_LOGIN_HISTORY = "USER_VIEW_OWN_LOGIN_HISTORY";
    
        // Calendar permissions
        private static final String CALENDAR_READ = "CALENDAR_READ";
        private static final String CALENDAR_CREATE = "CALENDAR_CREATE";
        private static final String CALENDAR_UPDATE = "CALENDAR_UPDATE";
        private static final String CALENDAR_DELETE = "CALENDAR_DELETE";

    @Transactional
    public void initializePermissionsAndRoles() {
        log.debug("🔑 Initializing system permissions and roles...");

        // Always try to create/update permissions and roles so the initializer can be
        // re-run safely when we add new permissions in code. We will upsert (create if
        // missing, update description if changed) permissions and then ensure roles
        // exist and have the expected permission assignments (merge, do not remove).
        createOrUpdatePermissions();
        createOrUpdateRoles();

        log.debug("✅ Successfully initialized {} permissions and {} roles",
            permissionRepository.count(), roleRepository.count());
    }

    private void createOrUpdatePermissions() {
        log.debug("📋 Upserting system permissions...");

        List<Permission> desired = Arrays.asList(
                new Permission(USER_MANAGE, "Quản lý user (Admin)"),
                new Permission(USER_TOKEN_MANAGEMENT, "Quản lý token của user"),
                new Permission(TOKEN_INVALIDATE_OWN, "Hủy token của bản thân"),
                new Permission(TOKEN_INVALIDATE_USER, "Hủy token của user cụ thể"),
                new Permission(TOKEN_VIEW_OWN, "Xem token version của bản thân"),
                new Permission(TOKEN_VIEW_USER, "Xem token version của user cụ thể"),
                new Permission(ROLE_VIEW, "Xem vai trò"),
                new Permission(ROLE_CREATE, "Tạo vai trò"),
                new Permission(ROLE_UPDATE, "Cập nhật vai trò"),
                new Permission(ROLE_DELETE, "Xóa vai trò"),
                new Permission(ROLE_UPDATE_PERMISSIONS, "Gán quyền cho vai trò"),
                new Permission(PERMISSION_VIEW, "Xem quyền"),
                new Permission(PERMISSION_UPDATE, "Cập nhật quyền"),
                new Permission(LOG_VIEW_ACTIVITY, "Xem user activity logs"),
                new Permission(ADMIN_ACTIVITY_LOG_EXPORT, "Export user activity logs"),
                new Permission(LOG_DELETE, "Xóa user activity logs"),
                new Permission(ADMIN_TRANSACTION_VIEW, "Xem lịch sử giao dịch"),
                new Permission(USER_VIEW_OWN_LOGIN_HISTORY, "Xem lịch sử đăng nhập của bản thân"),
                new Permission(CALENDAR_READ, "Xem calendar / Lấy token calendar"),
                new Permission(CALENDAR_CREATE, "Tạo sự kiện calendar"),
                new Permission(CALENDAR_UPDATE, "Cập nhật sự kiện calendar"),
                new Permission(CALENDAR_DELETE, "Xóa sự kiện calendar")
        );

        for (Permission p : desired) {
            permissionRepository.findByCode(p.getCode()).ifPresentOrElse(existing -> {
                if (!existing.getName().equals(p.getName())) {
                    existing.setName(p.getName());
                    permissionRepository.save(existing);
                }
            }, () -> {
                permissionRepository.save(p);
            });
        }
    }

        private void createOrUpdateRoles() {
        // quiet operation: no per-role logs to keep startup output clean

        Map<String, Permission> permMap = permissionRepository.findAll()
            .stream()
            .collect(Collectors.toMap(Permission::getCode, p -> p));

        // Admin: full permissions
        Role admin = roleRepository.findByName("ADMIN").orElseGet(() -> Role.builder().name("ADMIN").build());
        Set<Permission> adminPerms = new HashSet<>(permMap.values());
        admin.setPermissions(adminPerms);
        roleRepository.save(admin);

        // Member: desired codes (merge into existing role permissions)
        Set<String> memberCodes = Set.of(
            USER_TOKEN_MANAGEMENT,
            TOKEN_INVALIDATE_OWN,
            TOKEN_VIEW_OWN,
            USER_VIEW_OWN_LOGIN_HISTORY,
            CALENDAR_READ,
            CALENDAR_CREATE,
            CALENDAR_UPDATE,
            CALENDAR_DELETE
        );

        Role member = roleRepository.findByName("MEMBER").orElseGet(() -> Role.builder().name("MEMBER").build());
        Set<Permission> desiredMemberPerms = memberCodes.stream()
            .map(permMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<Permission> existing = member.getPermissions() == null ? new HashSet<>() : new HashSet<>(member.getPermissions());
        existing.addAll(desiredMemberPerms);
        member.setPermissions(existing);
        roleRepository.save(member);

        // roles upserted
        }
}
