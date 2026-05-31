package com.rbac.model

import spock.lang.Specification
import spock.lang.Subject

class RoleSpec extends Specification {

    @Subject
    Role role

    def setup() {
        role = new Role("developer")
    }

    def "a role should know its own name"() {
        expect:
        role.name == "developer"
    }

    def "a role with no permissions should not grant any permission"() {
        expect:
        !role.hasPermission("delete_users")
    }

    def "a role should grant a permission explicitly assigned to it"() {
        given: "a permission is added to the role"
        role.addPermission("read_reports")

        expect: "the role confirms it has that permission"
        role.hasPermission("read_reports")
    }

    def "a role should NOT grant a permission that was never assigned"() {
        given:
        role.addPermission("read_reports")

        expect:
        !role.hasPermission("delete_users")
    }

    def "a role that has an inherited role should be able to return that role"() {
        given:
        Role newRole = new Role("employee")

        when:
        role.addInheritance(newRole)

        then:
        role.getInheritsFrom().contains(newRole)
    }

    def "adding the same inherited role twice should result in only one role stored"() {
        given:
        Role newRole = new Role("employee")

        when:
        role.addInheritance(newRole)
        role.addInheritance(newRole)

        then:
        role.getInheritsFrom().size() == 1
    }
}