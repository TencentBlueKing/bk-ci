package com.tencent.devops.log.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.stream.annotation.StreamConsumer
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
        val re = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                .setExpandSuperTypes(true)
        ).getTypesAnnotatedWith(StreamEvent::class.java)
        println(re)
        re.forEach { clazz ->
            val streamEvent = clazz.getAnnotation(StreamEvent::class.java)
            println(
                "Found StreamEvent class: ${clazz.canonicalName}, " +
                    "with destination[${streamEvent.destination}]"
            )
        }
        val re1 = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
                .setExpandSuperTypes(true)
                .setScanners(Scanners.MethodsAnnotated)
        ).getMethodsAnnotatedWith(StreamConsumer::class.java)
        println(re1)
        re1.forEach { method ->
            val streamEvent = method.getAnnotation(StreamConsumer::class.java)
            println(
                "Found StreamEvent class: ${method.name}, " +
                    "with destination[${streamEvent.streamEvent}]"
            )
        }
    }
}
