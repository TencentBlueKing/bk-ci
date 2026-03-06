package com.tencent.devops.common.security.jwt

/**
 * JWT安全相关常量定义
 *
 * 用于定义JWT安全增强功能中使用的各种常量，包括算法、Header字段、Claims字段和错误码等
 */
object JwtSecurityConstants {

    /**
     * JWT算法常量
     */
    object JwtAlgorithms {
        /** RSA SHA-512算法 */
        const val RS512: String = "RS512"

        /** RSA SHA-256算法 */
        const val RS256: String = "RS256"

        /** 支持的算法列表 */
        val SUPPORTED_ALGORITHMS: Set<String> = setOf(RS512, RS256)
    }

    /**
     * JWT Header常量
     */
    object JwtHeaders {
        /** 算法字段名 */
        const val ALGORITHM: String = "alg"

        /** 类型字段名 */
        const val TYPE: String = "typ"

        /** 密钥ID字段名 */
        const val KEY_ID: String = "kid"

        /** 默认类型值 */
        const val DEFAULT_TYPE: String = "JWT"
    }

    /**
     * JWT Claims常量
     */
    object JwtClaims {
        /** 签发者字段名 */
        const val ISSUER: String = "iss"

        /** 主题字段名 */
        const val SUBJECT: String = "sub"

        /** 受众字段名 */
        const val AUDIENCE: String = "aud"

        /** 过期时间字段名 */
        const val EXPIRATION: String = "exp"

        /** 生效时间字段名 */
        const val NOT_BEFORE: String = "nbf"

        /** 签发时间字段名 */
        const val ISSUED_AT: String = "iat"
    }

    /**
     * JWT错误码常量
     */
    object JwtErrorCodes {
        /** 无效算法错误码 */
        const val INVALID_ALGORITHM: String = "JWT_INVALID_ALGORITHM"

        /** 无效密钥ID错误码 */
        const val INVALID_KID: String = "JWT_INVALID_KID"

        /** Token过期错误码 */
        const val EXPIRED_TOKEN: String = "JWT_EXPIRED_TOKEN"

        /** 无效签名错误码 */
        const val INVALID_SIGNATURE: String = "JWT_INVALID_SIGNATURE"

        /** 无效签发者错误码 */
        const val INVALID_ISSUER: String = "JWT_INVALID_ISSUER"

        /** 无效受众错误码 */
        const val INVALID_AUDIENCE: String = "JWT_INVALID_AUDIENCE"

        /** Token尚未生效错误码 */
        const val TOKEN_NOT_YET_VALID: String = "JWT_TOKEN_NOT_YET_VALID"
    }

    /**
     * 获取默认的密钥ID
     *
     * @return 默认密钥ID（devops）
     */
    fun getDefaultKeyId(): String = "devops"

    /**
     * 获取默认的签发者
     *
     * @return 默认签发者（DEVOPS）
     */
    fun getDefaultIssuer(): String = "DEVOPS"

    /**
     * 检查算法是否被支持
     *
     * @param algorithm 算法名称
     * @return 是否支持该算法
     */
    fun isAlgorithmSupported(algorithm: String): Boolean {
        return JwtAlgorithms.SUPPORTED_ALGORITHMS.contains(algorithm)
    }
}
