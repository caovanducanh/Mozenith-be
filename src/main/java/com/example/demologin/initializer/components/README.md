# 🧩 Initialization Components

This folder contains modular initialization components that are orchestrated by the main `MainDataInitializer`.

## 📁 Components

### 🔑 PermissionRoleInitializer
- **Purpose**: Creates system permissions and roles
- **Dependencies**: None (runs first)
- **Output**: 28 permissions, 2 roles (ADMIN, MEMBER)
 - **Behavior**: Safe to re-run — permissions are upserted (created or updated) and role permission assignments are merged (new permissions added to roles without removing existing ones)

### 👥 DefaultUserInitializer  
- **Purpose**: Creates default system users
- **Dependencies**: PermissionRoleInitializer (needs roles)
- **Output**: admin and member users with assigned roles

## 🔄 Execution Order

1. **PermissionRoleInitializer** - Creates permissions and roles
2. **DefaultUserInitializer** - Creates users with roles

## ➕ Adding New Components

1. Create your initializer in this folder
2. Add it to `MainDataInitializer` 
3. Ensure proper dependency order

Example:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class YourFeatureInitializer {
    
    @Transactional
    public void initializeYourFeature() {
        // Your initialization logic
    }
}
```

## 📋 Guidelines

- ✅ Use `@Component` for Spring dependency injection
- ✅ Use `@Transactional` for database operations
- ✅ Make operations idempotent (check if data exists)
- ✅ Provide detailed logging
- ✅ Handle dependencies properly
