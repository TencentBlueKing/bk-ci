/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.project.util

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * 国际化信息缓存
 *
 * @since: 2023-02-03
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object BkI18nMessageCacheUtil {

    private val i18nMessageCache = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build<String, String>()

    /**
     * 保存国际化信息缓存
     * @param key 缓存key值
     * @param value 缓存value值
     */
    fun put(key: String, value: String) {
        i18nMessageCache.put(key, value)
    }

    /**
     * 从缓存中获取国际化信息
     * @param key 缓存key值
     * @return 国际化信息
     */
    fun getIfPresent(key: String): String? {
        return i18nMessageCache.getIfPresent(key)
    }

    /**
     * 获取国际化信息缓在缓存中的key
     * @param moduleCode 模块标识
     * @param language 语言信息
     * @param key 国际化变量名
     * @return 国际化信息缓在缓存中的key
     */
    fun getI18nMessageCacheKey(
        moduleCode: String,
        language: String,
        key: String
    ): String {
        return "BK_I18N_MESSAGE:$moduleCode:$language:$key"
    }
}
