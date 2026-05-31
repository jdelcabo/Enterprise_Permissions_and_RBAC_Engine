package com.rbac.service;

import com.rbac.model.Role;
import java.util.HashSet;
import java.util.Set;

public class PermissionService {

    /**
     * Checks whether a role has a given permission,
     * either directly or transitively through its inheritance chain.
     *
     * Uses Depth First Search (DFS) to traverse the role graph upward.
     *
     * @param role       the role to check
     * @param permission the permission string to look for
     * @return true if the role has the permission directly or via inheritance
     */
    public boolean hasPermission(Role role, String permission) {
        return dfs(role, permission, new HashSet<>());
    }

    private boolean dfs(Role current, String permission, Set<Role> visited) {
        // if we've already visited this role, skip it (avoid cycling)
        if (visited.contains(current)) {
            return false;
        }
        visited.add(current);

        // this role directly has the permission (base case)
        if (current.hasPermission(permission)) {
            return true;
        }

        // check all roles this role inherits from (recursive case)
        for (Role parent : current.getInheritsFrom()) {
            if (dfs(parent, permission, visited)) {
                return true;
            }
        }

        return false;
    }
}