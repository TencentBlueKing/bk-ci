package com.tencent.devops.process.util

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.process.pojo.PipelineAtomRel

/**
 * 插件被流水线使用情况的缓存工具类
 * 支持缓存分页数据、Excel导出数据，过期时间30分钟
 */
object PipelineAtomRelCacheUtil {

    private val commonCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, Any?>()

    /**
     * 获取分页数据
     */
    fun getPipelineAtomRelPageList(
        atomCode: String,
        version: String?,
        page: Int,
        pageSize: Int,
        dbFetcher: () -> Page<PipelineAtomRel>?
    ): Page<PipelineAtomRel>? {
        val cacheKey = buildCacheKey(
            dataType = "page",
            atomCode = atomCode,
            version = version,
            page = page,
            pageSize = pageSize
        )
        return getFromCacheOrDb(cacheKey, dbFetcher)
    }

    /**
     * 获取Excel导出数据
     */
    fun getPipelineAtomRelExcelList(
        atomCode: String,
        version: String?,
        dbFetcher: () -> List<Array<String?>>?
    ): List<Array<String?>>? {
        val cacheKey = buildCacheKey(
            dataType = "excel",
            atomCode = atomCode,
            version = version
        )
        return getFromCacheOrDb(cacheKey, dbFetcher)
    }

    /**
     * @param cacheKey 唯一缓存键
     * @param dbFetcher 数据库查询逻辑（函数类型参数）
     * @return 缓存或数据库查询结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> getFromCacheOrDb(cacheKey: String, dbFetcher: () -> T?): T? {
        val cachedData = commonCache.getIfPresent(cacheKey)
        if (cachedData != null) {
            return cachedData as T
        }
        val dbData = dbFetcher()
        if (dbData != null) {
            commonCache.put(cacheKey, dbData)
        }
        return dbData
    }

    /**
     * @param dataType 数据类型（如page/excel，必传，避免不同类型数据冲突）
     * @param atomCode 插件标识
     * @param version 版本
     * @param page 页码
     * @param pageSize 每页条数
     */
    private fun buildCacheKey(
        dataType: String,
        atomCode: String,
        version: String?,
        page: Int? = null,
        pageSize: Int? = null
    ): String {
        val keyParts = mutableListOf(
            "dataType=$dataType",
            "atomCode=$atomCode",
            "version=${version ?: "null"}"
        )
        if (page != null && pageSize != null) {
            keyParts.add("page=$page")
            keyParts.add("pageSize=$pageSize")
        }
        return keyParts.joinToString(separator = ":")
    }
}