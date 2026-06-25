package com.rbac.service;

import com.rbac.model.Role;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

/**
 * Decorates a PermissionChecker with a Redis-backed cache.
 * Identical questions within the TTL window are answered from Redis
 * instead of re-running the real traversal logic.
 */
public class CachedPermissionService implements PermissionChecker {

    private static final int TTL_SECONDS = 60;

    private final PermissionChecker delegate;
    private final JedisPool jedisPool;

    public CachedPermissionService(PermissionChecker delegate, JedisPool jedisPool) {
        this.delegate = delegate;
        this.jedisPool = jedisPool;
    }

    @Override
    public boolean hasPermission(Role role, String permission) {
        String key = "perm:" + role.getName() + ":" + permission;

        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(key);
            if (cached != null) {
                return Boolean.parseBoolean(cached);
            }

            boolean result = delegate.hasPermission(role, permission);
            jedis.setex(key, TTL_SECONDS, Boolean.toString(result));
            return result;
        }
    }

    @Override
    public Set<String> getAllPermissions(Role role) {
        String key = "permissions:" + role.getName();

        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.exists(key)) {
                return jedis.smembers(key);
            }

            Set<String> result = delegate.getAllPermissions(role);
            if (!result.isEmpty()) {
                jedis.sadd(key, result.toArray(new String[0]));
                jedis.expire(key, TTL_SECONDS);
            }
            return result;
        }
    }
}