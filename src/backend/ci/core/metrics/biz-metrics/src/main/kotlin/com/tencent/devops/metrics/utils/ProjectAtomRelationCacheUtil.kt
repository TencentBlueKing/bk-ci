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

package com.tencent.devops.metrics.utils

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * 项目-插件关联关系本地缓存。
 *
 * 用于 metrics 上报路径上避免每次构建都对 T_PROJECT_ATOM 做"先查再写"：
 *   - 命中缓存即认为该 (projectId, atomCode, atomName) 已写入过，无需再走 DB；
 *   - 大幅减少对 T_PROJECT_ATOM 的写入次数，缓解 ON DUPLICATE KEY UPDATE 在
 *     唯一索引 UNI_TPA_PROJECT_CODE 上产生的 next-key / gap lock 等待。
 */
object ProjectAtomRelationCacheUtil {

    private const val MAX_CACHE_SIZE = 50_000L
    private const val EXPIRE_DAYS = 1L

    private val cache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .expireAfterWrite(EXPIRE_DAYS, TimeUnit.DAYS)
        .build<String, Boolean>()

    fun getIfPresent(key: String): Boolean? = cache.getIfPresent(key)

    fun put(key: String, value: Boolean = true) {
        cache.put(key, value)
    }

    fun buildKey(projectId: String, atomCode: String, atomName: String): String =
        "$projectId:$atomCode:$atomName"
}
