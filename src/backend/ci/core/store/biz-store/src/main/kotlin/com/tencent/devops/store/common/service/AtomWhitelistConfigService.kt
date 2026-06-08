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

package com.tencent.devops.store.common.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.store.common.dao.BusinessConfigDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 插件功能白名单配置服务
 * 基于 T_BUSINESS_CONFIG 存储，Caffeine 本地缓存
 *
 * 存储映射：
 *   BUSINESS = "STORE"
 *   FEATURE = "ATOM_WHITELIST"
 *   BUSINESS_VALUE = whitelistType (如 PYTHON_VENV)
 *   CONFIG_VALUE = ["atom1","atom2"] (JSON 数组)
 */
@Service
class AtomWhitelistConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val businessConfigDao: BusinessConfigDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AtomWhitelistConfigService::class.java)
        private const val BUSINESS = "STORE"
        private const val FEATURE = "ATOM_WHITELIST"
        private const val CACHE_EXPIRE_SECONDS = 60L
        private val objectMapper = ObjectMapper()
    }

    // 本地缓存：key = whitelistType, value = atomCode 集合
    private val cache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 判断插件是否在指定类型的白名单中
     */
    fun isAtomInWhitelist(atomCode: String, whitelistType: String): Boolean {
        val atomCodes = getAtomCodes(whitelistType)
        return atomCodes.contains(atomCode)
    }

    /**
     * 获取指定类型白名单中所有插件代码
     */
    fun getAtomCodes(whitelistType: String): Set<String> {
        return try {
            cache.get(whitelistType) { loadFromDb(whitelistType) } ?: emptySet()
        } catch (e: Exception) {
            logger.warn("getAtomCodes failed for whitelistType=$whitelistType", e)
            emptySet()
        }
    }

    /**
     * 主动失效缓存（配置变更后调用）
     */
    fun invalidateCache(whitelistType: String) {
        cache.invalidate(whitelistType)
        logger.info("invalidateCache: whitelistType=$whitelistType")
    }

    private fun loadFromDb(whitelistType: String): Set<String> {
        return try {
            val record = businessConfigDao.get(dslContext, BUSINESS, FEATURE, whitelistType)
            record?.let {
                objectMapper.readValue(it.configValue, object : TypeReference<Set<String>>() {})
            } ?: emptySet()
        } catch (e: Exception) {
            logger.warn("loadFromDb failed for whitelistType=$whitelistType", e)
            emptySet()
        }
    }
}
