package com.rbac;

import com.rbac.repository.RoleRepository;
import com.rbac.service.PermissionService;
import com.rbac.service.RoleHierarchy;
import com.rbac.web.App;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/rbac_db",
                "rbac_user",
                "rbac_password"
        );
        DSLContext dsl = DSL.using(connection);

        RoleRepository repository = new RoleRepository(dsl);
        RoleHierarchy hierarchy = repository.loadFullHierarchy();
        PermissionService permissionService = new PermissionService();

        App app = new App(hierarchy, permissionService, 4567);
        app.start();

        System.out.println("RBAC Engine running on http://localhost:4567");
    }
}