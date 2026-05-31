package com.rbac.service

import com.rbac.model.Role
import spock.lang.Specification
import spock.lang.Subject

class RoleHierarchySpec extends Specification {

    @Subject
    RoleHierarchy hierarchy

    def setup() {
        hierarchy = new RoleHierarchy()
    }

    def "a role can be added to the hierarchy"() {
        given:
        Role intern = new Role("intern")

        when:
        hierarchy.addRole(intern)

        then:
        hierarchy.getRole("intern") == intern
    }

    def "retrieving a role that does not exist returns null"() {
        expect:
        hierarchy.getRole("ghost") == null
    }

    def "two roles can be connected so one inherits from another"() {
        given:
        Role intern = new Role("intern")
        Role employee = new Role("employee")
        hierarchy.addRole(intern)
        hierarchy.addRole(employee)

        when:
        // employee inherits from intern
        hierarchy.connect(employee, intern)

        then:
        employee.getInheritsFrom().contains(intern)
    }

    def "connecting roles that were not added to the hierarchy throws an exception"() {
        given:
        Role ghost = new Role("ghost")
        Role intern = new Role("intern")

        when:
        hierarchy.connect(ghost, intern)

        then:
        thrown(IllegalArgumentException)
    }

    def "the full RBAC chain resolves permissions correctly end to end"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role employee = new Role("employee")
        employee.addPermission("submit_expenses")

        Role manager = new Role("manager")
        manager.addPermission("approve_budget")

        hierarchy.addRole(intern)
        hierarchy.addRole(employee)
        hierarchy.addRole(manager)

        // employee inherits from intern
        hierarchy.connect(employee, intern)
        // manager inherits from employee
        hierarchy.connect(manager, employee)

        PermissionService service = new PermissionService()

        expect:
        service.hasPermission(intern, "read_wiki")
        !service.hasPermission(intern, "submit_expenses")
        service.hasPermission(employee, "read_wiki")
        service.hasPermission(employee, "submit_expenses")
        !service.hasPermission(employee, "approve_budget")
        service.hasPermission(manager, "read_wiki")
        service.hasPermission(manager, "submit_expenses")
        service.hasPermission(manager, "approve_budget")
    }
}
