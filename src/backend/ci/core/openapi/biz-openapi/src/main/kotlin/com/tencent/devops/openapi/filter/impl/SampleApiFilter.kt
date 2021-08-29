package com.tencent.devops.openapi.filter.impl

import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.filter.ApiFilter
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import java.util.Date
import javax.crypto.SecretKey
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import kotlin.collections.HashMap

@Provider
@PreMatching
@RequestFilter
@Suppress("UNUSED")
class SampleApiFilter : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(SampleApiFilter::class.java)
        private const val jwtHeader = "X-Bkapi-JWT"
    }

    override fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        TODO("Not yet implemented")
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        TODO("Not yet implemented")
    }

    fun verifyUserTokenPermission(requestContext: ContainerRequestContext): Boolean {
        val bkApiJwt = requestContext.getHeaderString(jwtHeader)
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.request}")
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request bkapi jwt is empty.")
                    .build()
            )
            return false
        }
        return isValidToken(bkApiJwt)
    }

    fun generateUserToken(userDetails: String): String {
        return generateToken(userDetails)
    }

    /**
     * 用于生成 JWT 令牌的加密密钥
     */

    private fun generalKeyByDecoders(): SecretKey {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode("cuAihCz53DZRjZwbsGcZJ2Ai6At+T142uphtJMsk7iQ="));
    }

    /**
     * 为给定的 [UserDetails] 和有效期生成 JWT 令牌
     */
    fun generateToken(userDetails: String): String {
        val claims = HashMap<String, Any>()
        return createToken(claims, userDetails)
    }

    /**
     * 创建具有给定声明、主题和有效期的令牌
     */
    fun createToken(claims: Map<String, Any>, subject: String): String {
        //5 hours validity
        val expirationDate = Date(System.currentTimeMillis() + 1000 * 60 * 60 * 5)

        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(expirationDate)
            .signWith(generalKeyByDecoders(), SignatureAlgorithm.HS512).compact()
    }

    /**
     *返回令牌是否有效
     */
    fun isValidToken(token: String): Boolean {
        return !isTokenExpired(token)
    }

    /**
     * 提取给定 JWT 令牌的所有声明
     */
    fun extractAllClaims(token: String): Claims = Jwts.parser().setSigningKey(generalKeyByDecoders())
        .parseClaimsJws(token).body

    /**
     * 从给定的令牌和解析器函数中提取单个声明
     */
    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    /**
     * 返回给定的 JWT 令牌是否过期
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            extractExpiration(token).before(Date())
        } catch (e: ExpiredJwtException) {
            true
        }
    }

    /**
     * 提取给定 JWT 令牌的到期日期
     */
    fun extractExpiration(token: String): Date = extractClaim(token, Claims::getExpiration)

    /**
     * 提取给定 JWT 令牌的用户名
     */
    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    fun getTokenFromAuthHeader(authHeader: String): String? {
        return if (authHeader.startsWith("Bearer")) {
            authHeader.substring(7)
        } else {
            null
        }
    }
}