package com.rbac.repository

import com.rbac.model.Role
import org.jooq.DSLContext
import org.jooq.impl.DSL
import spock.lang.Specification
import spock.lang.Subject
import java.sql.DriverManager

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
}