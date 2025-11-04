package com.tencent.devops.process.util

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * 插件被流水线使用情况的缓存工具类
 * 支持缓存分页数据、Excel导出数据，过期时间30分钟
 */
object PipelineAtomRelCacheUtil {

    private val commonCache = Caffeine.newBuilder()
        .maximumSize(8000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, String?>()

    fun getPipelineAtomRelVersions(
        atomCode: String,
        pipelineId: String
    ): String? {
        val cacheKey = buildCacheKey(
            atomCode = atomCode,
            pipelineId = pipelineId
        )
        return getFromCacheOrDb(cacheKey)
    }

    /**
     * 批量存储缓存：同一插件下，多流水线的版本号数据
     * @param atomCode 插件标识
     * @param pipelineVersionMap 流水线ID到版本号集合的映射
     */
    fun batchPutPipelineAtomRelVersions(
        atomCode: String,
        pipelineVersionMap: Map<String, MutableSet<String>>
    ) {
        val cacheMap = mutableMapOf<String, String?>()
        pipelineVersionMap.forEach { (pipelineId, versionSet) ->
            val cacheKey = buildCacheKey(atomCode, pipelineId)
            val versionStr = versionSet.joinToString(",")
            cacheMap[cacheKey] = versionStr
        }
        commonCache.putAll(cacheMap)
    }

    /**
     * 判断指定key是否存在于缓存中
     */
    fun containsKey(atomCode: String, pipelineId: String): Boolean {
        val cacheKey = buildCacheKey(atomCode, pipelineId)
        return commonCache.getIfPresent(cacheKey) != null
    }

    /**
     * @param cacheKey 唯一缓存键
     * @param dbFetcher 数据库查询逻辑（函数类型参数）
     * @return 缓存或数据库查询结果
     */
    private fun getFromCacheOrDb(cacheKey: String): String? {
        return commonCache.getIfPresent(cacheKey)
    }

    /**
     * @param dataType 数据类型（如page/excel，必传，避免不同类型数据冲突）
     * @param atomCode 插件标识
     * @param version 版本
     * @param page 页码
     * @param pageSize 每页条数
     */
    private fun buildCacheKey(
        atomCode: String,
        pipelineId: String
    ): String {
        val keyParts = mutableListOf(
            "pipelineId=$pipelineId",
            "atomCode=$atomCode",
        )
        return keyParts.joinToString(separator = ":")
    }
}