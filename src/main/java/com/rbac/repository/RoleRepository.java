package com.rbac.repository;

import com.rbac.model.Role;
import org.jooq.DSLContext;
import org.jooq.Record;
import com.rbac.service.RoleHierarchy;
import java.util.HashMap;
import java.util.Map;
import static com.rbac.generated.Tables.*;

/**
 * Loads Role data from PostgreSQL using jOOQ and converts it
 * into the Role objects the service layer already understands.
 */
public class RoleRepository {

    private final DSLContext dsl;

    public RoleRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds a role by name, including its directly assigned permissions.
     * Returns null if no role with that name exists.
     */
    public Role findByName(String name) {
        Record roleRecord = dsl.selectFrom(ROLES)
                .where(ROLES.NAME.eq(name))
                .fetchOne();

        if (roleRecord == null) {
            return null;
        }

        Role role = new Role(name);
        Integer roleId = roleRecord.get(ROLES.ID);

        dsl.select(PERMISSIONS.NAME)
                .from(PERMISSIONS)
                .join(ROLE_PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .where(ROLE_PERMISSIONS.ROLE_ID.eq(roleId))
                .fetch()
                .forEach(record -> role.addPermission(record.get(PERMISSIONS.NAME)));

        return role;
    }

    /**
     * Loads every role, its direct permissions, and all inheritance
     * relationships from the database, returning a fully wired RoleHierarchy.
     */
    public RoleHierarchy loadFullHierarchy() {
        RoleHierarchy hierarchy = new RoleHierarchy();
        Map<Integer, Role> rolesById = new HashMap<>();

        // Load every role and register it in the hierarchy
        dsl.selectFrom(ROLES).fetch().forEach(record -> {
            String name = record.get(ROLES.NAME);
            Integer id = record.get(ROLES.ID);

            Role role = new Role(name);
            rolesById.put(id, role);
            hierarchy.addRole(role);
        });

        // Load every direct permission assignment
        dsl.select(ROLE_PERMISSIONS.ROLE_ID, PERMISSIONS.NAME)
                .from(ROLE_PERMISSIONS)
                .join(PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .fetch()
                .forEach(record -> {
                    Integer roleId = record.get(ROLE_PERMISSIONS.ROLE_ID);
                    String permissionName = record.get(PERMISSIONS.NAME);
                    rolesById.get(roleId).addPermission(permissionName);
                });

        // Load every inheritance relationship and connect the roles
        dsl.selectFrom(ROLE_INHERITANCE).fetch().forEach(record -> {
            Integer childId = record.get(ROLE_INHERITANCE.CHILD_ROLE_ID);
            Integer parentId = record.get(ROLE_INHERITANCE.PARENT_ROLE_ID);

            Role child = rolesById.get(childId);
            Role parent = rolesById.get(parentId);

            hierarchy.connect(child, parent);
        });

        return hierarchy;
    }
}