package com.rbac.service

import com.rbac.model.Role
import spock.lang.Specification
import spock.lang.Subject

class PermissionServiceSpec extends Specification {

    @Subject
    PermissionService permissionService

    def setup() {
        permissionService = new PermissionService()
    }

    // Direct permission checks

    def "a role with a direct permission should be granted that permission"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        expect:
        permissionService.hasPermission(intern, "read_wiki")
    }

    def "a role without a permission should be denied"() {
        given:
        Role intern = new Role("intern")

        expect:
        !permissionService.hasPermission(intern, "read_wiki")
    }

    // Inherited permission checks

    def "a role should inherit permissions from a direct parent"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role employee = new Role("employee")
        employee.addPermission("submit_expenses")
        // employee inherits from intern
        employee.addInheritance(intern)

        expect:
        permissionService.hasPermission(employee, "read_wiki")  // inherited
    }

    def "a role should inherit permissions transitively through a chain"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role employee = new Role("employee")
        employee.addInheritance(intern)

        Role manager = new Role("manager")
        manager.addPermission("approve_budget")
        manager.addInheritance(employee)

        expect:
        permissionService.hasPermission(manager, "read_wiki")
    }

    def "a role should NOT inherit permissions from a role it has no path to"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role manager = new Role("manager")
        manager.addPermission("approve_budget")

        expect:
        !permissionService.hasPermission(intern, "approve_budget")
    }

    def "a role should inherit permissions from multiple parents"() {
        given:
        Role frontend = new Role("frontend")
        frontend.addPermission("edit_ui")

        Role backend = new Role("backend")
        backend.addPermission("edit_api")

        Role fullstack = new Role("fullstack")
        fullstack.addInheritance(frontend)
        fullstack.addInheritance(backend)

        expect:
        // from frontend
        permissionService.hasPermission(fullstack, "edit_ui")
        // from backend
        permissionService.hasPermission(fullstack, "edit_api")
    }

    def "getAllPermissions should return every permission reachable through inheritance"() {
        given:
        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role employee = new Role("employee")
        employee.addPermission("submit_expenses")
        employee.addInheritance(intern)

        Role manager = new Role("manager")
        manager.addPermission("approve_budget")
        manager.addInheritance(employee)

        when:
        Set<String> permissions = permissionService.getAllPermissions(manager)

        then:
        permissions == ["approve_budget", "submit_expenses", "read_wiki"] as Set
    }
}