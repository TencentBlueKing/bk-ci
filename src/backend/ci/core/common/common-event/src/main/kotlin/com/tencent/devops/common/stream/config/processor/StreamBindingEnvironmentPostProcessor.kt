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

package com.tencent.devops.common.stream.config.processor

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.rabbit.RabbitQueueType
import com.tencent.devops.common.stream.utils.DefaultBindingUtils
import java.util.Properties
import java.util.function.Consumer
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import kotlin.reflect.jvm.kotlinFunction

@Suppress("LongMethod", "MagicNumber", "TooGenericExceptionThrown", "NestedBlockDepth")
class StreamBindingEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {
    private var groupName = "undefined"

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        environment.propertySources.forEach {
            val source = it.source
            if (source is Map<*, *>) {
                if (source.containsKey("spring.application.name")) {
                    groupName = source["spring.application.name"].toString()
                }
            }
            if (groupName.isBlank()) {
                println("WARNING: application name must be defined in resources/application.yml")
            }
        }

        println("StreamBindingEnvironmentPostProcessor start")
        environment.propertySources.addLast(createPropertySource())
        println("StreamBindingEnvironmentPostProcessor finish")
    }

    private fun createPropertySource(): PropertiesPropertySource {
        with(Properties()) {
            // 如果未配置服务使用的binder类型，则使用全局默认binder类型
            // 如果均未配置则不进行注解的反射解析
            val definition = mutableListOf<String>()

            // 反射扫描所有带有 Event 注解的事件类
            val eventClasses = Reflections(
                ConfigurationBuilder()
                    .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                    .setExpandSuperTypes(true)
            ).getTypesAnnotatedWith(Event::class.java)
            eventClasses.forEach { clazz ->
                val event = clazz.getAnnotation(Event::class.java)
                val bindingName = DefaultBindingUtils.getOutBindingName(clazz)
                logger.info(
                    "Found StreamEvent class: ${clazz.name}, bindingName[$bindingName], " +
                            "with destination[${event.destination}"
                )
                val rabbitPropPrefix = "spring.cloud.stream.rabbit.bindings.$bindingName"
                setProperty("$rabbitPropPrefix.producer.delayedExchange", "true")
                setProperty("$rabbitPropPrefix.producer.exchangeType", ExchangeTypes.TOPIC)
                val prefix = "spring.cloud.stream.bindings.$bindingName"
                setProperty("$prefix.destination", event.destination)
                setProperty("$prefix.binder", event.binder)
            }

            // 反射扫描所有带有 StreamConsumer 注解的bean方法
            val consumerMethodBeans = Reflections(
                ConfigurationBuilder()
                    .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                    .setExpandSuperTypes(true)
                    .setScanners(Scanners.MethodsAnnotated)
            ).getMethodsAnnotatedWith(EventConsumer::class.java)
            consumerMethodBeans.forEach { method ->
                if (method.returnType == Consumer::class.java) {
                    val consumerArgs = method.kotlinFunction?.returnType?.arguments?.get(0)?.type?.arguments
                    if (!consumerArgs.isNullOrEmpty()) {
                        val event = Class.forName(consumerArgs[0].toString()).getAnnotation(Event::class.java)
                        val consumer = method.getAnnotation(EventConsumer::class.java)
                        val bindingName = DefaultBindingUtils.getInBindingName(method)
                        println(
                            "Found StreamConsumer method: ${method.name}, bindingName[$bindingName], " +
                                    "with destination[${event.destination}], group[$groupName]"
                        )
                        definition.add(bindingName)
                        // 如果注解中指定了订阅组，则直接设置
                        // 如果未指定则取当前服务名作为订阅组，保证所有分布式服务再同一个组内
                        setBindings(bindingName, consumer, event)
                    }
                }
            }

            // 声明所有扫描结果的函数式声明
            setProperty("spring.cloud.stream.function.definition", definition.joinToString(";"))
            setProperty("spring.cloud.function.definition", definition.joinToString(";"))
            return PropertiesPropertySource(STREAM_SOURCE_NAME, this)
        }
    }

    private fun Properties.setBindings(
        bindingName: String,
        consumer: EventConsumer,
        event: Event
    ) {
        val concurrencyExpression = "\${bkScs.consumer.concurrency.$bindingName:${consumer.defaultConcurrency}}"
        val bindingPrefix = "spring.cloud.stream.bindings.$bindingName-in-0"
        val rabbitPropPrefix = "spring.cloud.stream.rabbit.bindings.$bindingName-in-0"
        val pulsarPropPrefix = "spring.cloud.stream.pulsar.bindings.$bindingName-in-0"
        setProperty("$bindingPrefix.destination", event.destination)
        setProperty("$bindingPrefix.binder", event.binder)
        setProperty("$bindingPrefix.consumer.concurrency", concurrencyExpression)
        if (consumer.anonymous) {
            // 如果队列匿名则在消费者销毁后删除该队列
            setProperty("$rabbitPropPrefix.consumer.anonymousGroupPrefix", groupName)
            setProperty("$rabbitPropPrefix.consumer.durableSubscription", "true")
            setProperty("$pulsarPropPrefix.consumer.subscriptionMode", "NonDurable")
        } else {
            setProperty("$bindingPrefix.group", consumer.groupName.ifBlank { "$groupName-$bindingName" })
        }

        // rabbitmq特殊配置
        setProperty("$rabbitPropPrefix.consumer.maxConcurrency", concurrencyExpression)
        setProperty("$rabbitPropPrefix.consumer.delayedExchange", "true")
        setProperty("$rabbitPropPrefix.consumer.exchangeType", ExchangeTypes.TOPIC)
        val quorumExpression = "\${bkci.rabbitQuorumEnable:false}"
        if (consumer.type == RabbitQueueType.QUORUM && !consumer.anonymous) {
            setProperty("$rabbitPropPrefix.consumer.quorum.enabled", quorumExpression)
        }
    }

    override fun getOrder(): Int {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamBindingEnvironmentPostProcessor::class.java)
        private const val STREAM_SOURCE_NAME = "streamBindingProperties"
    }
}
