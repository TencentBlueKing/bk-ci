/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.custom

import com.tencent.devops.common.stream.pulsar.extend.ErrorAcknowledgeHandler
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.util.StringUtils
import java.util.concurrent.ConcurrentHashMap

object PulsarBeanContainerCache {

    private val CLASSES = arrayOf<Class<*>>(
        CompositeMessageConverter::class.java,
        ErrorAcknowledgeHandler::class.java
    )

    private val BEANS_CACHE: MutableMap<String, Any> = ConcurrentHashMap()

    fun putBean(beanName: String, beanObj: Any) {
        BEANS_CACHE[beanName] = beanObj
    }

    fun getClassAry(): Array<Class<*>> {
        return CLASSES
    }

    fun <T> getBean(beanName: String, clazz: Class<T?>): T? {
        return getBean(beanName, clazz, null)
    }

    fun <T> getBean(beanName: String, clazz: Class<T>, defaultObj: T): T {
        if (!StringUtils.hasLength(beanName)) {
            return defaultObj
        }
        val obj = BEANS_CACHE[beanName] ?: return defaultObj
        return if (clazz.isAssignableFrom(obj.javaClass)) {
            obj as T
        } else defaultObj
    }
}
