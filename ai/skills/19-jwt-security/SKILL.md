---
name: 19-jwt-security
description: JWT 安全认证指南，涵盖 JWT 生成验证、Token 刷新机制、权限校验、安全配置、OAuth2 集成。当用户实现 JWT 认证、配置安全过滤器、处理 Token 刷新或集成 OAuth2 时使用。
---

# JWT 安全认证

JWT 安全认证指南.

## 触发条件

当用户需要实现 JWT Token 生成、验证、密钥管理时，使用此 Skill。

## JwtManager

```kotlin
@Component
class JwtManager(
    private val jwtProperties: JwtProperties,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JwtManager::class.java)
    }
    
    // 生成 Token
    fun generateToken(userId: String, claims: Map<String, Any> = emptyMap()): String {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.expireSeconds * 1000)
        
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .addClaims(claims)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }
    
    // 验证 Token
    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            logger.warn("Token 已过期")
            null
        } catch (e: JwtException) {
            logger.warn("Token 无效: ${e.message}")
            null
        }
    }
    
    // 从 Token 获取用户ID
    fun getUserId(token: String): String? {
        return validateToken(token)?.subject
    }
}
```

## Token 缓存

```kotlin
// 缓存 Token 到 Redis
fun cacheToken(userId: String, token: String) {
    val key = "jwt:token:$userId"
    redisOperation.set(key, token, jwtProperties.expireSeconds)
}

// 验证 Token 是否在缓存中
fun isTokenValid(userId: String, token: String): Boolean {
    val key = "jwt:token:$userId"
    val cachedToken = redisOperation.get(key)
    return cachedToken == token
}

// 使 Token 失效
fun invalidateToken(userId: String) {
    val key = "jwt:token:$userId"
    redisOperation.delete(key)
}
```

## 配置

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expireSeconds: 86400  # 24 小时
  refreshExpireSeconds: 604800  # 7 天
```

## 密钥轮换

```kotlin
fun rotateKey() {
    val newSecret = generateSecureSecret()
    // 1. 保存新密钥
    saveNewSecret(newSecret)
    // 2. 设置过渡期，同时支持新旧密钥
    // 3. 过渡期后删除旧密钥
}
```

## 最佳实践

1. **密钥安全**：使用环境变量存储密钥
2. **合理过期**：设置适当的过期时间
3. **Token 刷新**：实现无感刷新机制
4. **黑名单**：支持主动使 Token 失效

## 相关文件

- `common-security/src/main/kotlin/com/tencent/devops/common/security/jwt/`
