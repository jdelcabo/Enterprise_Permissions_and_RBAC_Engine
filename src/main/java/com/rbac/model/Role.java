package com.rbac.model;

import java.util.HashSet;
import java.util.Set;

public class Role {
    // Attributes
    private final String name;
    private final Set<String> permissions;
    private final Set<Role> inheritsFrom;

    // Constructor
    public Role(String name) {
        this.name = name;
        this.permissions = new HashSet<>();
        this.inheritsFrom = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public void addInheritance(Role role) {
        inheritsFrom.add(role);
    }

    public Set<Role> getInheritsFrom() {
        return inheritsFrom;
    }
}