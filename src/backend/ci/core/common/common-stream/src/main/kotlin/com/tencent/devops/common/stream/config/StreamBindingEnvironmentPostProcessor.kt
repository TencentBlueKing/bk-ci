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

package com.tencent.devops.common.stream.config

import com.tencent.devops.common.stream.annotation.StreamConsumer
import com.tencent.devops.common.stream.annotation.StreamEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import java.util.Properties

// TODO #7443
class StreamBindingEnvironmentPostProcessor: EnvironmentPostProcessor, Ordered {

    @Value("\${spring.cloud.stream.default-binder:#{null}}")
    private val defaultBinder: String? = null

    @Value("\${spring.cloud.stream.service-binder:#{null}}")
    private val serviceBinder: String? = null

    @Value("\${spring.application.name:#{null}}")
    private val serviceName: String? = null

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        environment.propertySources.addLast(createPropertySource())
    }

    private fun createPropertySource(): PropertiesPropertySource {
        with(Properties()) {
            // 如果未配置服务使用的binder类型，则使用全局默认binder类型
            // 如果均未配置则不进行注解的反射解析
            val binder = serviceBinder ?: defaultBinder ?: return
            val ref = Reflections()
            val propertySources = applicationContext.environment.propertySources
            ref.getTypesAnnotatedWith(StreamEvent::class.java).forEach { clazz ->
                val streamEvent = clazz.getAnnotation(StreamEvent::class.java)
                logger.info("Found StreamEvent class: ${clazz.name}, " +
                                "with destination[${streamEvent.destination}]")
                propertySources.addLast(
                    MapPropertySource(streamEvent.destination, mapOf(
                        "destination" to streamEvent.destination,
                        "binder" to binder
                    ))
                )
            }
            ref.getTypesAnnotatedWith(StreamConsumer::class.java).forEach { clazz ->
                val streamConsumer = clazz.getAnnotation(StreamConsumer::class.java)
                println("Found StreamConsumer class: ${clazz.name}, " +
                            "with destination[${streamConsumer.destination}] group[${streamConsumer.group}]")
                // 如果注解中指定了订阅组，则直接设置
                // 如果未指定则取当前服务名作为订阅组，保证所有分布式服务再同一个组内
                val subscriptionGroup = streamConsumer.group.ifBlank {
                    serviceName?.let { "$it-service" } ?: "default"
                }
                propertySources.addLast(
                    MapPropertySource(clazz.simpleName, mapOf(
                        "destination" to streamConsumer.destination,
                        "group" to subscriptionGroup,
                        "binder" to binder
                    ))
                )
            }
            setProperty("spring.cloud.consul.config.name", "")
            setProperty("spring.cloud.consul.config.name", "")
            return PropertiesPropertySource(STREAM_SOURCE_NAME, this)
        }
    }

    override fun getOrder(): Int {
        return  ConfigDataEnvironmentPostProcessor.ORDER - 1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamBindingEnvironmentPostProcessor::class.java)
        private const val STREAM_SOURCE_NAME = "streamBindingProperties"
    }
}
