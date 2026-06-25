package com.rbac.service

import com.rbac.model.Role
import redis.clients.jedis.JedisPool
import spock.lang.Specification
import spock.lang.Subject

class CachedPermissionServiceSpec extends Specification {

    JedisPool jedisPool = new JedisPool("localhost", 6379)

    // Mock() creates a fake stand-in for an interface. We control exactly
    // what it returns, and we can verify HOW MANY TIMES it was called.
    PermissionChecker delegate = Mock(PermissionChecker)

    @Subject
    CachedPermissionService cachedService = new CachedPermissionService(delegate, jedisPool)

    def cleanup() {
        // Wipe Redis between tests so they don't interfere with each other
        jedisPool.resource.withCloseable { it.flushAll() }
    }

    def "should only call the real service once for repeated identical checks"() {
        given:
        Role manager = new Role("manager")
        delegate.hasPermission(manager, "approve_budget") >> true   // stub the response

        when:
        def first  = cachedService.hasPermission(manager, "approve_budget")
        def second = cachedService.hasPermission(manager, "approve_budget")

        then:
        first
        second
        1 * delegate.hasPermission(manager, "approve_budget")   // ← verifies it ran exactly ONCE
    }

    def "should fall back to the real service when nothing is cached yet"() {
        given:
        Role intern = new Role("intern")
        delegate.hasPermission(intern, "read_wiki") >> false

        expect:
        !cachedService.hasPermission(intern, "read_wiki")
    }
}