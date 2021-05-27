package com.tencent.devops.log.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class IndexNameUtilsTest {

    @Test
    fun getIndexName() {
        val cacheSize: Long = 19
        val indexCache = CacheBuilder.newBuilder()
            .recordStats()
            .maximumSize(cacheSize)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build<String/*BuildId*/, String/*IndexName*/>(
                object : CacheLoader<String, String>() {
                    override fun load(buildId: String): String {
                        return "$buildId-abc"
                    }
                }
            )
        val indexMap = mutableMapOf<String, String>()
        for (i in 1..cacheSize) {
            val buildId = "b-$i"
            val index = indexCache.get(buildId)
            indexMap[buildId] = index
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
    fun caffeine() {
        val cacheSize: Long = 20
        val indexCache = Caffeine.newBuilder()
            .recordStats()
            .maximumSize(cacheSize)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build<String/*BuildId*/, String/*IndexName*/> { buildId ->
//                println(buildId)
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
}
