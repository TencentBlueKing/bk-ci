package com.tencent.devops.log.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.stream.annotation.StreamEventConsumer
import com.tencent.devops.common.stream.annotation.StreamEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.concurrent.TimeUnit

class IndexNameUtilsTest {

    @Test
    fun getIndexNameInCache() {
        val cacheSize: Long = 20
        val indexCache = Caffeine.newBuilder()
            .recordStats()
            .maximumSize(cacheSize)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build<String/*BuildId*/, String/*IndexName*/> { buildId ->
                "$buildId-abc"
            }
        val indexMap = mutableMapOf<String, String>()
        for (i in 1..cacheSize) {
            val buildId = "b-$i"
            val index = indexCache.get(buildId)
            indexMap[buildId] = index!!
        }
        println(indexCache.stats())

        for (i in 1..cacheSize) {
            val buildId = "b-$i"
            val index = indexCache.get(buildId)
            assertEquals(index, indexMap[buildId])
        }
        println(indexCache.stats())
    }

    @Test
    fun testReflections() {
        val definition = mutableListOf<String>()
        val eventClasses = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                .setExpandSuperTypes(true)
        ).getTypesAnnotatedWith(StreamEvent::class.java)
        eventClasses.forEach { clazz ->
            val streamEvent = clazz.getAnnotation(StreamEvent::class.java)
            println(
                "Found StreamEvent class: ${clazz.name}, " +
                    "with destination[${streamEvent.destination}]"
            )
            val bindingName = "${clazz.simpleName.decapitalize()}Out"
            definition.add(bindingName)
        }
        val consumerBeans = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                .setExpandSuperTypes(true)
                .setScanners(Scanners.MethodsAnnotated)
        ).getMethodsAnnotatedWith(StreamEventConsumer::class.java)
        consumerBeans.forEach { method ->
            val streamEventConsumer = method.getAnnotation(StreamEventConsumer::class.java)
            println(
                "Found StreamConsumer class: ${method.name}, " +
                    "with destination[${streamEventConsumer.destination}] group[${streamEventConsumer.group}]"
            )
            definition.add("${method.name}In")
            // 如果注解中指定了订阅组，则直接设置
            // 如果未指定则取当前服务名作为订阅组，保证所有分布式服务再同一个组内
        }
        println(definition)
    }
}
