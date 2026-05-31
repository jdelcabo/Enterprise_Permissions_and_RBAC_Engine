package com.rbac.service;

import com.rbac.model.Role;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the graph of roles and their inheritance relationships.
 * Responsible for storing roles and connecting them — nothing more.
 * Permission traversal is intentionally delegated to PermissionService.
 */
public class RoleHierarchy {

    // Role name → Role object. HashMap for O(1) lookup by name.
    private final Map<String, Role> roles = new HashMap<>();

    /**
     * Registers a role in the hierarchy.
     */
    public void addRole(Role role) {
        roles.put(role.getName(), role);
    }

    /**
     * Retrieves a role by name. Returns null if not found.
     */
    public Role getRole(String name) {
        return roles.get(name);
    }

    /**
     * Connects two roles so that 'child' inherits from 'parent'.
     * Both roles must already be registered in the hierarchy.
     *
     * @throws IllegalArgumentException if either role is not registered
     */
    public void connect(Role child, Role parent) {
        if (!roles.containsValue(child) || !roles.containsValue(parent)) {
            throw new IllegalArgumentException(
                    "Both roles must be added to the hierarchy before connecting them"
            );
        }
        child.addInheritance(parent);
    }
}