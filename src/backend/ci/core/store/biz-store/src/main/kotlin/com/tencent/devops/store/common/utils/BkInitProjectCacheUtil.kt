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

package com.tencent.devops.store.common.utils

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.util.concurrent.TimeUnit

/**
 * 初始化项目缓存
 *
 * @since: 2023-08-12
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object BkInitProjectCacheUtil {

    private const val CACHE_MAX_SIZE = 5000L

    private val initProjectCache = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, String>()

    /**
     * 保存初始化项目缓存
     * @param key 缓存key值
     * @param value 缓存value值
     */
    fun put(key: String, value: String) {
        initProjectCache.put(key, value)
    }

    /**
     * 从缓存中获取初始化项目
     * @param key 缓存key值
     * @return 初始化项目
     */
    fun getIfPresent(key: String): String? {
        return initProjectCache.getIfPresent(key)
    }

    /**
     * 获取初始化项目在缓存中的key
     * @param storeCode 组件标识
     * @param storeType 组件类型
     * @return 初始化项目在缓存中的key
     */
    fun getInitProjectCacheKey(
        storeCode: String,
        storeType: StoreTypeEnum
    ): String {
        return "${getInitProjectCacheKeyPrefix(storeType)}:$storeCode"
    }

    /**
     * 获取初始化项目在缓存中的key前缀
     * @param storeType 组件类型
     * @return 初始化项目在缓存中的key前缀
     */
    fun getInitProjectCacheKeyPrefix(
        storeType: StoreTypeEnum
    ): String {
        return "STORE_INIT_PROJECT:${storeType.name}"
    }
}
