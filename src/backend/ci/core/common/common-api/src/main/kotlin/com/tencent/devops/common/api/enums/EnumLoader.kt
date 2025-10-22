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

package com.tencent.devops.common.api.enums

import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 仅在初始化时调用一次，不可重复使用
 */
object EnumLoader {

    private var modify = AtomicBoolean(false)

    private val logger = LoggerFactory.getLogger(EnumLoader::class.java)

    fun enumModified() {
        // 同一JVM中防止多次重复加载，造成Enum实例不一致
        if (!modify.compareAndSet(false, true)) {
            return
        }
        val clazz = EnumModifier::class.java
        var fetcheries = ServiceLoader.load(clazz)
        if (!fetcheries.iterator().hasNext()) {
            fetcheries = ServiceLoader.load(clazz, ServiceLoader::class.java.classLoader)
        }
        fetcheries.forEach { modifier ->
            logger.info("[ENUM MODIFIER]: $modifier")
            try {
                modifier.modified()
            } catch (e: Exception) {
                logger.error("[ENUM MODIFIER]| load fail| ${e.message}", e)
            }
        }
    }
}
