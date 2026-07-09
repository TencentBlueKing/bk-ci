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
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreWhitelistConfigService @Autowired constructor(
    private val businessConfigService: BusinessConfigService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StoreWhitelistConfigService::class.java)
    }

    private val whitelistCache: Cache<String, List<String>> = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build()

    fun isInWhitelist(storeType: StoreTypeEnum, whitelistType: String, code: String): Boolean {
        return try {
            val codes = whitelistCache.get("${storeType.name}_$whitelistType") { key ->
                val configValue = businessConfigService.getConfigValue(
                    business = storeType.name,
                    feature = "${storeType.name}_WHITELIST",
                    businessValue = whitelistType
                )
                if (configValue != null) {
                    JsonUtil.to(configValue, object : TypeReference<List<String>>() {})
                } else {
                    emptyList()
                }
            }
            codes.contains(code)
        } catch (ignored: Exception) {
            logger.warn(
                "isInWhitelist failed|storeType=$storeType|whitelistType=$whitelistType|code=$code",
                ignored
            )
            false
        }
    }
}
