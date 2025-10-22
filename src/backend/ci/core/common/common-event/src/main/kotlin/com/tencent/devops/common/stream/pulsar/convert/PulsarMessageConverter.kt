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

package com.tencent.devops.common.stream.pulsar.convert

import org.springframework.messaging.converter.ByteArrayMessageConverter
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.util.ClassUtils

/**
 * The default message converter of Pulsar,its bean name is {@link #DEFAULT_NAME} .
 */
class PulsarMessageConverter {
    private var messageConverter: CompositeMessageConverter? = null
    private val jacksonPresent: Boolean
    private val fastjsonPresent: Boolean
    init {
        val classLoader = PulsarMessageConverter::class.java.classLoader
        jacksonPresent = ClassUtils
            .isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
            ClassUtils.isPresent(
                "com.fasterxml.jackson.core.JsonGenerator",
                classLoader
            )
        fastjsonPresent = ClassUtils.isPresent("com.alibaba.fastjson.JSON", classLoader) &&
            ClassUtils.isPresent(
                "com.alibaba.fastjson.support.config.FastJsonConfig",
                classLoader
            )
    }

    init {
        val messageConverters: MutableList<MessageConverter> = ArrayList()
        val byteArrayMessageConverter = ByteArrayMessageConverter()
        byteArrayMessageConverter.contentTypeResolver = null
        messageConverters.add(byteArrayMessageConverter)
        messageConverters.add(StringMessageConverter())
        if (jacksonPresent) {
            messageConverters.add(MappingJackson2MessageConverter())
        }
        if (fastjsonPresent) {
            try {
                messageConverters.add(
                    ClassUtils.forName(
                        "com.alibaba.fastjson.support.spring.messaging.MappingFastJsonMessageConverter",
                        ClassUtils.getDefaultClassLoader()
                    ).newInstance() as MessageConverter
                )
            } catch (ignored: ClassNotFoundException) {
                // ignore this exception
            } catch (ignored: IllegalAccessException) {
            } catch (ignored: InstantiationException) {
            }
        }
        messageConverter = CompositeMessageConverter(messageConverters)
    }

    fun getMessageConverter(): CompositeMessageConverter {
        return messageConverter!!
    }

    companion object {
        /**
         * if you want to customize a bean, please use the BeanName.
         */
        const val DEFAULT_NAME = "com.tencent.devops.common.stream.pulsar.convert.PulsarMessageConverter"
    }
}
