package com.tencent.devops.common.sdk.github.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

object GithubJwtUtil {

    fun generatorJwt(appId: String, privateKey: String): String {
        val now = Instant.now()

        // Max token expiration is 10 minutes for GitHub
        // We use a smaller window since we likely will not need more than a few seconds
        val expiration: Instant = now.plus(Duration.ofMinutes(8))

        // Setting the issued at to a time in the past to allow for clock skew
        val issuedAt: Instant = getIssuedAt(now)

        // Let's set the JWT Claims
        val builder = Jwts.builder()
            .setIssuedAt(Date.from(issuedAt))
            .setExpiration(Date.from(expiration))
            .setIssuer(appId)
            .signWith(getPrivateKeyFromString(privateKey), SignatureAlgorithm.RS256)

        // Token will refresh 2 minutes before it expires
        val validUntil = expiration.minus(Duration.ofMinutes(2))

        // Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact()
    }

    private fun getPrivateKeyFromString(key: String): PrivateKey? {
        if (key.contains(" RSA ")) {
            throw InvalidKeySpecException(
                "Private key must be a PKCS#8 formatted string, to convert it from PKCS#1 use: " +
                    "openssl pkcs8 -topk8 -inform PEM -outform PEM -in current-key.pem -out new-key.pem -nocrypt"
            )
        }

        // Remove all comments and whitespace from PEM
        // such as "-----BEGIN PRIVATE KEY-----" and newlines
        val privateKeyContent = key.replace("(?m)^--.*".toRegex(), "")
            .replace("\\s".toRegex(), "")
        val kf = KeyFactory.getInstance("RSA")
        try {
            val decode: ByteArray = Base64.getDecoder().decode(privateKeyContent)
            val keySpecPKCS8 = PKCS8EncodedKeySpec(decode)
            return kf.generatePrivate(keySpecPKCS8)
        } catch (e: IllegalArgumentException) {
            throw InvalidKeySpecException("Failed to decode private key: " + e.message, e)
        }
    }

    private fun getIssuedAt(now: Instant): Instant {
        return now.minus(Duration.ofMinutes(2))
    }
}
