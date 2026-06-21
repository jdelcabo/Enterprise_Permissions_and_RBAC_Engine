package com.rbac.repository

import com.rbac.model.Role
import org.jooq.DSLContext
import org.jooq.impl.DSL
import spock.lang.Specification
import spock.lang.Subject
import java.sql.DriverManager
import com.rbac.service.RoleHierarchy
import com.rbac.service.PermissionService

class RoleRepositorySpec extends Specification {

    @Subject
    RoleRepository roleRepository

    def setup() {
        def connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/rbac_db",
                "rbac_user",
                "rbac_password"
        )
        DSLContext dsl = DSL.using(connection)
        roleRepository = new RoleRepository(dsl)
    }

    def "it should load a role by name from the database"() {
        when:
        Role role = roleRepository.findByName("intern")

        then:
        role != null
        role.name == "intern"
    }

    def "it should load the direct permissions of a role"() {
        when:
        Role role = roleRepository.findByName("intern")

        then:
        role.hasPermission("read_wiki")
    }

    def "a role with no matching name returns null"() {
        expect:
        roleRepository.findByName("nonexistent_role") == null
    }

    def "it should load the full role hierarchy with inheritance relationships"() {
        when:
        RoleHierarchy hierarchy = roleRepository.loadFullHierarchy()

        Role intern   = hierarchy.getRole("intern")
        Role employee = hierarchy.getRole("employee")
        Role manager  = hierarchy.getRole("manager")
        Role ceo      = hierarchy.getRole("ceo")

        then: "all roles exist"
        intern != null
        employee != null
        manager != null
        ceo != null

        and: "inheritance relationships are wired correctly"
        employee.getInheritsFrom().contains(intern)
        manager.getInheritsFrom().contains(employee)
        ceo.getInheritsFrom().contains(manager)
    }

    def "permissions should be inherited correctly when loaded from the database"() {
        given:
        PermissionService service = new PermissionService()

        when:
        RoleHierarchy hierarchy = roleRepository.loadFullHierarchy()
        Role ceo = hierarchy.getRole("ceo")

        then: "ceo inherits everything down the chain"
        service.hasPermission(ceo, "fire_anybody")
        service.hasPermission(ceo, "approve_budget")
        service.hasPermission(ceo, "submit_expenses")
        service.hasPermission(ceo, "read_wiki")
    }
}