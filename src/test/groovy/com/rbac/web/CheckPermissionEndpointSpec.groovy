package com.rbac.web

import com.rbac.model.Role
import com.rbac.service.PermissionService
import com.rbac.service.RoleHierarchy
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import spock.lang.Shared
import spock.lang.Specification

class CheckPermissionEndpointSpec extends Specification {

    @Shared App app
    @Shared def httpClient = HttpClients.createDefault()
    static final int PORT = 4568

    def setupSpec() {
        RoleHierarchy hierarchy = new RoleHierarchy()

        Role intern = new Role("intern")
        intern.addPermission("read_wiki")

        Role employee = new Role("employee")
        employee.addPermission("submit_expenses")

        Role manager = new Role("manager")
        manager.addPermission("approve_budget")

        hierarchy.addRole(intern)
        hierarchy.addRole(employee)
        hierarchy.addRole(manager)
        hierarchy.connect(employee, intern)
        hierarchy.connect(manager, employee)

        app = new App(hierarchy, new PermissionService(), PORT)
        app.start()
    }

    def cleanupSpec() {
        app.stop()
    }

    def "a manager checking an inherited permission should return true"() {
        given:
        def request = new HttpGet("http://localhost:${PORT}/check-permission?role=manager&permission=read_wiki")

        when:
        def response = httpClient.execute(request)
        def body = response.getEntity().getContent().text

        then:
        response.code == 200
        body.contains('"hasPermission":true')
    }

    def "an intern checking a permission above their rank should return false"() {
        given:
        def request = new HttpGet("http://localhost:${PORT}/check-permission?role=intern&permission=approve_budget")

        when:
        def response = httpClient.execute(request)
        def body = response.getEntity().getContent().text

        then:
        response.code == 200
        body.contains('"hasPermission":false')
    }

    def "a missing query parameter should return 400"() {
        given:
        def request = new HttpGet("http://localhost:${PORT}/check-permission?role=manager")

        when:
        def response = httpClient.execute(request)

        then:
        response.code == 400
    }

    def "an unknown role should return 404"() {
        given:
        def request = new HttpGet("http://localhost:${PORT}/check-permission?role=ghost&permission=read_wiki")

        when:
        def response = httpClient.execute(request)

        then:
        response.code == 404
    }

    def "listing permissions for manager should include inherited ones"() {
        given:
        def request = new HttpGet("http://localhost:${PORT}/roles/manager/permissions")

        when:
        def response = httpClient.execute(request)
        def body = response.getEntity().getContent().text

        then:
        response.code == 200
        body.contains("approve_budget")
        body.contains("submit_expenses")
        body.contains("read_wiki")
    }
}