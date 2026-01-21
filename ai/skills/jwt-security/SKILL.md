---
name: jwt-security
description: JWT 安全认证指南，涵盖 JWT 生成验证、Token 刷新机制、权限校验、安全配置、OAuth2 集成。当用户实现 JWT 认证、配置安全过滤器、处理 Token 刷新或集成 OAuth2 时使用。
core_files:
  - "src/backend/ci/core/common/common-security/src/main/kotlin/com/tencent/devops/common/security/jwt/"
related_skills:
  - auth-module-architecture
token_estimate: 1500
---

# JWT 安全认证

## Quick Reference

```
生成：Jwts.builder().setSubject(userId).signWith(key).compact()
验证：Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
缓存：Redis 存储，支持主动失效
```

### 最简示例

```kotlin
@Component
class JwtManager(private val jwtProperties: JwtProperties, private val redis: RedisOperation) {
    
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
    
    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).body
        } catch (e: ExpiredJwtException) {
            null
        } catch (e: JwtException) {
            null
        }
    }
    
    fun getUserId(token: String) = validateToken(token)?.subject
}
```

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expireSeconds: 86400  # 24 小时
```

## When to Use

- 用户认证
- API 鉴权
- Token 管理

---

## Token 缓存

```kotlin
// 缓存到 Redis
fun cacheToken(userId: String, token: String) {
    redis.set("jwt:token:$userId", token, jwtProperties.expireSeconds)
}

// 主动失效
fun invalidateToken(userId: String) {
    redis.delete("jwt:token:$userId")
}
```

---

## Checklist

- [ ] 使用环境变量存储密钥
- [ ] 设置适当的过期时间
- [ ] 实现无感刷新机制
- [ ] 支持主动使 Token 失效
