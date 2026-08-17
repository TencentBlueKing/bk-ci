package com.tencent.devops.remotedev.service.expert

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class ExpertSupportTokenGeneratorTest {

    private lateinit var generator: ExpertSupportTokenGenerator

    private val secret = "a-string-secret-at-least-256-bits-long"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    @BeforeEach
    fun setUp() {
        generator = ExpertSupportTokenGenerator()
        setField(generator, "appId", "anydev")
        setField(generator, "jwtSecret", secret)
    }

    @Test
    fun `oa user token should carry oa claims`() {
        val token = generator.generate(userId = "eastjiao", recordId = 100L)
        val claims = parse(token)

        assertEquals("anydev", claims["appId"])
        assertEquals("eastjiao", claims.subject)
        assertEquals("eastjiao", claims["user"])
        assertEquals("oa", claims["type"])
        assertEquals("100", claims["recordId"])
    }

    @Test
    fun `tai user token should carry tai claims`() {
        val token = generator.generate(userId = "anydev_odc@tai", recordId = 200L)
        val claims = parse(token)

        assertEquals("anydev_odc@tai", claims.subject)
        assertEquals("anydev_odc", claims["user"])
        assertEquals("tai", claims["type"])
        assertEquals("200", claims["recordId"])
    }

    @Test
    fun `token expiration should be three minutes after issued`() {
        val token = generator.generate(userId = "eastjiao", recordId = 1L)
        val claims = parse(token)

        val diffSeconds = (claims.expiration.time - claims.issuedAt.time) / 1000
        assertEquals(180L, diffSeconds)
    }

    @Test
    fun `token should be verifiable by the same secret`() {
        val token = generator.generate(userId = "eastjiao", recordId = 1L)
        assertTrue(token.split(".").size == 3)
        // parse 不抛异常即代表签名校验通过
        parse(token)
    }

    private fun parse(token: String) = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .payload

    private fun setField(target: Any, fieldName: String, value: Any) {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
