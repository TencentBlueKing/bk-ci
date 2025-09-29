package com.tencent.devops.common.security.jwt

import com.tencent.devops.common.api.util.JsonUtil
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import java.util.Base64
import org.slf4j.LoggerFactory

/**
 * Claims验证规则数据类
 *
 * @property claimName Claims名称
 * @property required 是否必需
 * @property expectedValue 期望值（可选）
 * @property validator 自定义验证函数
 */
data class ClaimsValidationRule(
    val claimName: String,
    val required: Boolean = false,
    val expectedValue: Any? = null,
    val validator: (Any?) -> Boolean = { true }
)

/**
 * Claims验证上下文数据类
 *
 * @property claims JWT Claims内容
 * @property validationRules 验证规则列表
 * @property currentTime 当前时间戳
 */
data class ClaimsValidationContext(
    val claims: Map<String, Any>,
    val validationRules: List<ClaimsValidationRule>,
    val currentTime: Long = System.currentTimeMillis()
)

/**
 * JWT验证结果数据类
 *
 * @property isValid 验证是否通过
 * @property errorCode 错误码（可选）
 * @property errorMessage 错误信息（可选）
 * @property claims JWT Claims内容
 * @property kid 密钥ID（可选）
 */
data class JwtValidationResult(
    val isValid: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val claims: Map<String, Any> = emptyMap(),
    val kid: String? = null
)

/**
 * JWT Token分割结果数据类
 *
 * @property header Header部分（Base64编码）
 * @property payload Payload部分（Base64编码）
 * @property signature Signature部分（Base64编码）
 */
data class JwtTokenParts(
    val header: String,
    val payload: String,
    val signature: String
)

/**
 * 解析后的JWT Header数据类
 *
 * @property algorithm 签名算法
 * @property type Token类型
 * @property keyId 密钥ID（可选）
 * @property rawHeader 原始Header字符串
 */
data class ParsedJwtHeader(
    val algorithm: String,
    val type: String,
    val keyId: String? = null,
    val rawHeader: String
)

/**
 * JWT Claims验证器
 *
 * 支持各种Claims的验证逻辑，包括过期时间、签发者、受众、生效时间等
 */
class JwtClaimsValidator(
    private val keyConfigManager: JwtKeyConfigManager
) {

    private val logger = LoggerFactory.getLogger(JwtClaimsValidator::class.java)

    /**
     * 执行完整的Claims验证
     *
     * @param context 验证上下文，包含Claims和验证规则
     * @return 完整的验证结果
     */
    private fun validateAllClaims(context: ClaimsValidationContext): JwtValidationResult {
        val claims = context.claims

        // 根据验证规则进行其他验证
        for (rule in context.validationRules) {
            val claimValue = claims[rule.claimName]

            // 检查必需字段
            if (rule.required && claimValue == null) {
                return JwtValidationResult(
                    isValid = false,
                    errorCode = "JWT_MISSING_REQUIRED_CLAIM",
                    errorMessage = "Missing required claim: ${rule.claimName}",
                    claims = claims
                )
            }

            // 检查期望值
            if (rule.expectedValue != null && claimValue != rule.expectedValue) {
                val errorCode = when (rule.claimName) {
                    JwtSecurityConstants.JwtClaims.ISSUER -> JwtSecurityConstants.JwtErrorCodes.INVALID_ISSUER
                    JwtSecurityConstants.JwtClaims.AUDIENCE -> JwtSecurityConstants.JwtErrorCodes.INVALID_AUDIENCE
                    else -> "JWT_CLAIM_VALIDATION_FAILED"
                }
                return JwtValidationResult(
                    isValid = false,
                    errorCode = errorCode,
                    errorMessage = "Claim validation failed for ${rule.claimName}. Expected: ${rule.expectedValue}, Actual: $claimValue",
                    claims = claims
                )
            }

            // 执行自定义验证
            if (claimValue != null && !rule.validator(claimValue)) {
                return JwtValidationResult(
                    isValid = false,
                    errorCode = "JWT_CUSTOM_VALIDATION_FAILED",
                    errorMessage = "Custom validation failed for claim: ${rule.claimName}",
                    claims = claims
                )
            }
        }

        return JwtValidationResult(
            isValid = true,
            claims = claims
        )
    }

    /**
     * 将JWT Token分割为三个部分
     *
     * @param token 完整的JWT Token
     * @return 分割后的Token各部分
     */
    private fun splitJwtToken(token: String): JwtTokenParts {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token format. Expected 3 parts, got ${parts.size}")
        }

        return JwtTokenParts(
            header = parts[0],
            payload = parts[1],
            signature = parts[2]
        )
    }

    /**
     * 解码JWT Header的Base64编码
     *
     * @param encodedHeader Base64编码的Header
     * @return 解码后的Header JSON字符串
     */
    private fun decodeHeader(encodedHeader: String): String {
        return try {
            String(Base64.getUrlDecoder().decode(encodedHeader), Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Failed to decode JWT header: $encodedHeader", e)
            throw IllegalArgumentException("Invalid Base64 encoded JWT header", e)
        }
    }

    /**
     * 解析Header JSON字符串为结构化对象
     *
     * @param headerJson Header JSON字符串
     * @return 解析后的Header对象
     */
    private fun parseHeaderJson(headerJson: String): ParsedJwtHeader {
        return try {
            val headerMap = JsonUtil.toMap(headerJson)
            ParsedJwtHeader(
                algorithm = headerMap[JwtSecurityConstants.JwtHeaders.ALGORITHM]?.toString()
                    ?: throw IllegalArgumentException("Missing algorithm in JWT header"),
                type = headerMap[JwtSecurityConstants.JwtHeaders.TYPE]?.toString()
                    ?: JwtSecurityConstants.JwtHeaders.DEFAULT_TYPE,
                keyId = headerMap[JwtSecurityConstants.JwtHeaders.KEY_ID]?.toString(),
                rawHeader = headerJson
            )
        } catch (e: Exception) {
            logger.error("Failed to parse JWT header JSON: $headerJson", e)
            throw IllegalArgumentException("Invalid JWT header JSON format", e)
        }
    }

    /**
     * 验证Header结构是否完整
     *
     * @param header 解析后的Header对象
     * @return Header结构验证结果
     */
    private fun validateHeaderStructure(header: ParsedJwtHeader): Boolean {
        // 检查必需字段
        if (header.algorithm.isBlank()) {
            logger.warn("JWT header missing algorithm field")
            return false
        }

        // 验证算法是否支持
        if (!JwtSecurityConstants.isAlgorithmSupported(header.algorithm)) {
            logger.warn("Unsupported JWT algorithm: ${header.algorithm}")
            return false
        }
        return true
    }

    /**
     * 解析JWT Header（完整流程）
     *
     * @param token JWT Token字符串
     * @return 解析后的Header对象
     */
    private fun parseJwtHeader(token: String): ParsedJwtHeader {
        val parts = splitJwtToken(token)
        val headerJson = decodeHeader(parts.header)
        val parsedHeader = parseHeaderJson(headerJson)

        if (!validateHeaderStructure(parsedHeader)) {
            throw IllegalArgumentException("JWT header validation failed")
        }

        return parsedHeader
    }

    /**
     * 验证JWT Token的完整性和有效性
     *
     * @param token JWT Token字符串
     * @param validationRules 额外的验证规则
     * @return JWT验证结果
     */
    private fun verifyJwtToken(
        token: String,
        validationRules: List<ClaimsValidationRule> = emptyList()
    ): JwtValidationResult {
        try {
            // 解析并验证Header
            val parsedHeader = parseJwtHeader(token)
            // 获取密钥配置
            val keyConfig = if (parsedHeader.keyId != null) {
                keyConfigManager.getKeyConfigByKid(parsedHeader.keyId)
            } else {
                keyConfigManager.getDefaultKeyConfig() ?: keyConfigManager.getActiveKeyConfig()
            }

            if (keyConfig == null) {
                return JwtValidationResult(
                    isValid = false,
                    errorCode = JwtSecurityConstants.JwtErrorCodes.INVALID_KID,
                    errorMessage = "Key config not found for kid: ${parsedHeader.keyId}"
                )
            }

            // 验证签名并解析Claims
            val claims = Jwts.parser()
                .verifyWith(keyConfig.publicKey)
                .build()
                .parseSignedClaims(token)
                .payload

            // 转换Claims为Map
            val claimsMap = claims.map { it.key to it.value }.toMap()

            // 验证Claims
            val validationContext = ClaimsValidationContext(
                claims = claimsMap,
                validationRules = validationRules,
                currentTime = System.currentTimeMillis()
            )

            val claimsResult = validateAllClaims(validationContext)
            if (!claimsResult.isValid) {
                return claimsResult.copy(kid = parsedHeader.keyId)
            }

            return JwtValidationResult(
                isValid = true,
                claims = claimsMap,
                kid = parsedHeader.keyId
            )

        } catch (e: ExpiredJwtException) {
            logger.warn("JWT token expired", e)
            return JwtValidationResult(
                isValid = false,
                errorCode = JwtSecurityConstants.JwtErrorCodes.EXPIRED_TOKEN,
                errorMessage = "JWT token has expired"
            )
        } catch (e: Exception) {
            logger.warn("JWT verification failed", e)
            return JwtValidationResult(
                isValid = false,
                errorCode = JwtSecurityConstants.JwtErrorCodes.INVALID_SIGNATURE,
                errorMessage = "JWT verification failed: ${e.message}"
            )
        }
    }

    /**
     * 验证JWT Token（使用密钥配置）
     *
     * @param token JWT Token字符串
     * @param validationRules 额外的验证规则
     * @return JWT验证结果
     */
    fun verifyJwtTokenWithKeyConfig(
        token: String,
        validationRules: List<ClaimsValidationRule> = emptyList()
    ): JwtValidationResult {
        return try {
            verifyJwtToken(token, validationRules)
        } catch (e: Exception) {
            logger.error("Failed to create public key from config", e)
            JwtValidationResult(
                isValid = false,
                errorCode = "JWT_KEY_CONFIG_ERROR",
                errorMessage = "Failed to process key configuration: ${e.message}"
            )
        }
    }
}
