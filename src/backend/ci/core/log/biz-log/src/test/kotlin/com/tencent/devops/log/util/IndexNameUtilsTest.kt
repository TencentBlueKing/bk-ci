package com.tencent.devops.log.util

import com.github.benmanes.caffeine.cache.Caffeine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
}
