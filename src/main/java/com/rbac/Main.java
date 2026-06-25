package com.rbac;

import com.rbac.repository.RoleRepository;
import com.rbac.service.CachedPermissionService;
import com.rbac.service.PermissionChecker;
import com.rbac.service.PermissionService;
import com.rbac.service.RoleHierarchy;
import com.rbac.web.App;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import redis.clients.jedis.JedisPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Env vars with localhost fallbacks — works locally AND inside Docker later
        String dbHost    = System.getenv().getOrDefault("DB_HOST", "localhost");
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");

        Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://" + dbHost + ":5432/rbac_db",
                "rbac_user",
                "rbac_password"
        );
        DSLContext dsl = DSL.using(connection);

        RoleRepository repository = new RoleRepository(dsl);
        RoleHierarchy hierarchy = repository.loadFullHierarchy();

        JedisPool jedisPool = new JedisPool(redisHost, 6379);
        PermissionChecker permissionService =
                new CachedPermissionService(new PermissionService(), jedisPool);

        App app = new App(hierarchy, permissionService, 4567);
        app.start();

        System.out.println("RBAC Engine running on http://localhost:4567");
    }
}