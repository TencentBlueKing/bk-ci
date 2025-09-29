/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.security.jwt

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * JWT密钥配置数据类
 *
 * @property kid 密钥ID，用于标识不同的密钥
 * @property privateKey 私钥
 * @property publicKey 公钥
 * @property isActive 是否为当前活跃密钥
 */
data class JwtKeyConfig(
    val kid: String,
    val privateKey: PrivateKey?,
    val publicKey: PublicKey,
    val isActive: Boolean = true
)


/**
 * JWT密钥配置管理器
 *
 * 支持多密钥管理和轮换功能
 */
class JwtKeyConfigManager {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtKeyConfigManager::class.java)
    }

    private val keyStore = ConcurrentHashMap<String, JwtKeyConfig>()
    private var activeKid: String = JwtSecurityConstants.getDefaultKeyId()

    /**
     * 获取当前活跃的密钥配置
     *
     * @return 当前活跃的密钥配置
     */
    fun getActiveKeyConfig(): JwtKeyConfig? {
        return getKeyConfigByKid(activeKid)
    }

    /**
     * 获取默认的密钥配置（kid=devops）
     *
     * @return 当前默认的密钥配置
     */
    fun getDefaultKeyConfig(): JwtKeyConfig? {
        return getKeyConfigByKid(JwtSecurityConstants.getDefaultKeyId())
    }

    /**
     * 根据密钥ID获取密钥配置
     *
     * @param kid 密钥ID
     * @return 对应的密钥配置，不存在时返回null
     */
    fun getKeyConfigByKid(kid: String): JwtKeyConfig? {
        return keyStore[kid]
    }

    /**
     * 添加新的密钥配置
     *
     * @param kid 新的密钥配置
     * @return 添加是否成功
     */
    fun addKeyConfig(kid: String, privateKey: String?, publicKey: String, active: Boolean): Boolean {
        return try {
            val keyFactory = KeyFactory.getInstance("RSA")
            keyStore[kid] = JwtKeyConfig(
                kid = kid,
                privateKey = privateKey?.let {
                    keyFactory.generatePrivate(
                        PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(privateKey)
                        )
                    )
                },
                publicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKey))),
                isActive = active
            )

            // 如果是活跃密钥，更新活跃密钥ID
            if (active) {
                activeKid = kid
            }

            logger.info("Added key config with kid: $kid")
            true
        } catch (e: Exception) {
            logger.error("Failed to add key config: $kid", e)
            false
        }
    }

}
