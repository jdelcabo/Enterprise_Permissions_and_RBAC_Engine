package com.rbac.repository;

import com.rbac.model.Role;
import org.jooq.DSLContext;
import org.jooq.Record;

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
}