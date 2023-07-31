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

package com.tencent.devops.common.web.utils

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * 国际化语言信息缓存
 *
 * @since: 2023-06-13
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object BkI18nLanguageCacheUtil {

    private val i18nLanguageCache = Caffeine.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, String>()

    /**
     * 保存国际化语言信息缓存
     * @param userId 用户Id
     * @param language 国际化语言信息
     */
    fun put(userId: String, language: String) {
        i18nLanguageCache.put(userId, language)
    }

    /**
     * 从缓存中获取国际化语言信息
     * @param userId 用户Id
     * @return 国际化语言信息
     */
    fun getIfPresent(userId: String): String? {
        return i18nLanguageCache.getIfPresent(userId)
    }
}
