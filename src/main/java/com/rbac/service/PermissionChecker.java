package com.rbac.service;

import com.rbac.model.Role;
import java.util.Set;

public interface PermissionChecker {
    boolean hasPermission(Role role, String permission);
    Set<String> getAllPermissions(Role role);
}