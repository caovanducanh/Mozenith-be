package com.example.demologin.aspect;

import com.example.demologin.cache.CacheDetector;
import com.example.demologin.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
@RequiredArgsConstructor
public class SmartCacheAspect {
    private static final Logger logger = LoggerFactory.getLogger(SmartCacheAspect.class);
    private static final ConcurrentMap<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, AtomicLong> DATA_VERSION = new ConcurrentHashMap<>();
    private static final AtomicLong GLOBAL_VERSION = new AtomicLong(0);

    private final CacheDetector detector;
    private final JwtUtil jwtUtil;

    @Around("@annotation(com.example.demologin.annotation.SmartCache)")
    public Object handleCache(ProceedingJoinPoint joinPoint) throws Throwable {
        // Generate base cache key including method parameters
        String baseCacheKey = generateCacheKeyWithArgs(joinPoint);
        String enhancedCacheKey = enhanceCacheKeyWithUserContext(baseCacheKey, joinPoint);

        boolean isWriteOperation = detector.isWriteOperation(joinPoint);

        // For read operations - check cache first
        if (!isWriteOperation) {
            Object cachedValue = CACHE.get(enhancedCacheKey);
            Long currentVersion = DATA_VERSION.get(enhancedCacheKey) != null ?
                    DATA_VERSION.get(enhancedCacheKey).get() : null;

            if (cachedValue != null && currentVersion != null) {
                logger.info("✅ [CACHE HIT] Key: {} | Version: {} | Cached Value Type: {}",
                        enhancedCacheKey,
                        currentVersion,
                        cachedValue.getClass().getSimpleName());
                return cachedValue;
            }
        }

        // Execute original method
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;

        // For write operations - update versions
        if (isWriteOperation) {
            handleWriteOperation(enhancedCacheKey, joinPoint, executionTime);
        } else {
            handleReadOperation(enhancedCacheKey, result, executionTime);
        }

        return result;
    }

    private String generateCacheKeyWithArgs(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String argsKey = "";
        if (args != null && args.length > 0) {
            argsKey = ":" + Arrays.stream(args)
                    .map(arg -> {
                        if (arg != null) {
                            // Handle arrays and collections for better cache key generation
                            if (arg.getClass().isArray()) {
                                return Arrays.toString((Object[]) arg);
                            } else if (arg instanceof java.util.Collection) {
                                return ((java.util.Collection<?>) arg).toString();
                            }
                            return arg.toString();
                        }
                        return "null";
                    })
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }
        return signature.getDeclaringType().getSimpleName() +
                ":" +
                signature.getMethod().getName() +
                argsKey;
    }

    private String enhanceCacheKeyWithUserContext(String baseCacheKey, ProceedingJoinPoint joinPoint) {
        // Only enhance cache key for permission-related methods
        if (baseCacheKey.contains("RolePermissionServiceImpl") &&
                baseCacheKey.contains("getPermissionsForRoles")) {
            try {
                HttpServletRequest request =
                        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                .getRequest();

                if (request != null) {
                    String token = request.getHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        Set<String> roles = jwtUtil.extractRoles(token.substring(7));
                        return baseCacheKey + ":roles=" + String.join(",", roles);
                    }
                }
            } catch (IllegalStateException e) {
                logger.warn("No request bound to current thread, using basic cache key");
            } catch (Exception e) {
                logger.warn("Could not extract user roles for cache key enhancement", e);
            }
        }
        return baseCacheKey;
    }

    private void handleWriteOperation(String cacheKey, ProceedingJoinPoint joinPoint, long executionTime) {
        long newVersion = GLOBAL_VERSION.incrementAndGet();
        Set<String> relatedCacheKeys = detector.getRelatedCacheKeys(joinPoint);

        // Invalidate all related caches
        for (String relatedKey : relatedCacheKeys) {
            DATA_VERSION.computeIfAbsent(relatedKey, k -> new AtomicLong(0))
                    .set(newVersion);
            CACHE.remove(relatedKey);
        }

        // Also invalidate the current operation's cache
        DATA_VERSION.computeIfAbsent(cacheKey, k -> new AtomicLong(0))
                .set(newVersion);
        CACHE.remove(cacheKey);

        // Special handling for permission updates - invalidate all permission caches
        if (cacheKey.contains("updatePermissions")) {
            CACHE.keySet().stream()
                    .filter(key -> key.contains("getPermissionsForRoles"))
                    .forEach(CACHE::remove);

            DATA_VERSION.keySet().stream()
                    .filter(key -> key.contains("getPermissionsForRoles"))
                    .forEach(key -> DATA_VERSION.computeIfAbsent(key, k -> new AtomicLong(0)).set(newVersion));
        }

        logger.info("🔄 [CACHE INVALIDATED] Key: {} and Related Keys: {} | New Version: {} | Execution Time: {}ms",
                cacheKey, relatedCacheKeys, newVersion, executionTime);
    }

    private void handleReadOperation(String cacheKey, Object result, long executionTime) {
        if (!CACHE.containsKey(cacheKey)) {
            DATA_VERSION.computeIfAbsent(cacheKey, k -> new AtomicLong(GLOBAL_VERSION.get()));
            CACHE.put(cacheKey, result);
            logger.info("📥 [CACHE STORED] Key: {} | Version: {} | Execution Time: {}ms | Value Type: {}",
                    cacheKey,
                    DATA_VERSION.get(cacheKey).get(),
                    executionTime,
                    result.getClass().getSimpleName());
        }
    }

    // Method to manually clear cache
    public static void clearCacheForKeys(String... keys) {
        for (String key : keys) {
            CACHE.remove(key);
            DATA_VERSION.remove(key);
        }
    }

    // Method to get cache statistics
    public static String getCacheStats() {
        return String.format("Cache size: %d, Data versions: %d, Global version: %d",
                CACHE.size(), DATA_VERSION.size(), GLOBAL_VERSION.get());
    }
}