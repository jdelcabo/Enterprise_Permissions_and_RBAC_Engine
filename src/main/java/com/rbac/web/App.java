package com.rbac.web;

import com.google.gson.Gson;
import com.rbac.model.Role;
import com.rbac.service.PermissionChecker;
import com.rbac.service.PermissionService;
import com.rbac.service.RoleHierarchy;
import spark.Request;
import spark.Response;
import spark.Service;
import java.util.Map;

/**
 * Wires together the web layer: defines HTTP routes and translates
 * HTTP requests into calls on the service layer.
 */
public class App {

    private final RoleHierarchy hierarchy;
    private final PermissionChecker permissionService;
    private final Gson gson = new Gson();
    private final Service http;

    public App(RoleHierarchy hierarchy, PermissionChecker permissionService, int port) {
        this.hierarchy = hierarchy;
        this.permissionService = permissionService;
        this.http = Service.ignite().port(port);
    }

    public void start() {
        http.get("/check-permission", this::handleCheckPermission);
        http.get("/roles/:name/permissions", this::handleListPermissions);

        // Catches ANY unhandled exception from ANY route, guaranteeing
        // every error response — expected or not — is consistent JSON.
        http.exception(Exception.class, (exception, req, res) -> {
            res.status(500);
            res.type("application/json");
            res.body(gson.toJson(Map.of(
                    "error", "An unexpected error occurred. Please try again later."
            )));
        });

        http.awaitInitialization();
    }

    public void stop() {
        http.stop();
        http.awaitStop();
    }

    private Object handleCheckPermission(Request req, Response res) {
        String roleName   = req.queryParams("role");
        String permission = req.queryParams("permission");

        // Validate input
        if (roleName == null || permission == null) {
            res.status(400);
            return gson.toJson(Map.of(
                    "error", "Both 'role' and 'permission' query parameters are required"
            ));
        }

        // Look up the role
        Role role = hierarchy.getRole(roleName);
        if (role == null) {
            res.status(404);
            return gson.toJson(Map.of("error", "Role not found: " + roleName));
        }

        // Delegate to the service layer — the actual business logic
        boolean hasPermission = permissionService.hasPermission(role, permission);

        res.status(200);
        res.type("application/json");
        return gson.toJson(Map.of(
                "role", roleName,
                "permission", permission,
                "hasPermission", hasPermission
        ));
    }

    private Object handleListPermissions(Request req, Response res) {
        String roleName = req.params(":name");

        Role role = hierarchy.getRole(roleName);
        if (role == null) {
            res.status(404);
            return gson.toJson(Map.of("error", "Role not found: " + roleName));
        }

        res.status(200);
        res.type("application/json");
        return gson.toJson(Map.of(
                "role", roleName,
                "permissions", permissionService.getAllPermissions(role)
        ));
    }
}