package com.rbac.web

import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import spock.lang.Shared
import spock.lang.Specification

import static spark.Spark.*

class AppSpec extends Specification {

    @Shared
    def httpClient = HttpClients.createDefault()

    // setupSpec runs ONCE before all tests in this file — not before each one
    def setupSpec() {
        port(4567)
        get("/ping", (req, res) -> "pong")
        awaitInitialization()   // blocks until Spark is actually ready to accept requests
    }

    // cleanupSpec runs ONCE after all tests finish
    def cleanupSpec() {
        stop()
        awaitStop()
    }

    def "GET /ping should return pong with status 200"() {
        given:
        def request = new HttpGet("http://localhost:4567/ping")

        when:
        def response = httpClient.execute(request)
        def body = response.getEntity().getContent().text

        then:
        response.code == 200
        body == "pong"
    }
}