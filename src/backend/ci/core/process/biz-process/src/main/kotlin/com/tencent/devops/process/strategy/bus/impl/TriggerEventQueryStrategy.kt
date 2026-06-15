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

package com.tencent.devops.process.strategy.bus.impl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_HISTORY
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Field
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * 触发事件查询策略
 * TRIGGER_EVENT -> TRIGGER_EVENT_TYPE
 */
@Component
class TriggerEventQueryStrategy @Autowired constructor(
    private val client: Client
) : AbstractHistoryConditionQueryStrategy() {

    /**
     * 缓存 value 到 displayName 的映射
     * 缓存时间：30分钟
     * 最大缓存数量：3000
     */
    private val displayNameCache: Cache<String, String> = Caffeine.newBuilder()
        .maximumSize(3000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build()

    override fun getField(): Field<String?> {
        return T_PIPELINE_BUILD_HISTORY.TRIGGER_EVENT_TYPE
    }

    /**
     * 将原始值转换为IdValue
     * 首先尝试从接口获取组件名称，如果获取不到则使用StartType.toReadableString做国际化
     * 使用 Caffeine 缓存优化性能
     */
    override fun convertToIdValue(userId: String, value: String): IdValue {
        // 先从缓存中获取
        val cachedDisplayName = displayNameCache.getIfPresent(value)
        if (cachedDisplayName != null) {
            return IdValue(value, cachedDisplayName)
        }
        // 缓存未命中，计算 displayName
        val displayName = computeDisplayName(value, userId)
        // 将结果放入缓存
        displayNameCache.put(value, displayName)
        return IdValue(value, displayName)
    }

    /**
     * 计算显示名称
     * 首先尝试从接口获取组件名称，如果获取不到则使用StartType.toReadableString做国际化
     */
    private fun computeDisplayName(value: String, userId: String): String {
        // 首先尝试从接口获取组件名称
        val componentName = try {
            val result: Result<StoreBaseInfo?> = client.get(ServiceStoreComponentResource::class)
                .getComponentBaseInfo(
                    userId = userId,
                    storeType = StoreTypeEnum.TRIGGER_EVENT.name,
                    storeCode = value
                )
            val resultData = result.data
            if (result.isOk() && resultData != null) {
                resultData.storeName
            } else {
                logger.warn("Failed to get component($value) name, result:${result.message}")
                null
            }
        } catch (ignored: Throwable) {
            logger.warn("Failed to get component($value) name", ignored)
            null
        }
        // 如果接口获取不到值，使用StartType.toReadableString做国际化
        return componentName ?: StartType.toReadableString(
            type = value,
            channelCode = ChannelCode.getRequestChannelCode(),
            language = I18nUtil.getLanguage(userId)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerEventQueryStrategy::class.java)
    }
}
