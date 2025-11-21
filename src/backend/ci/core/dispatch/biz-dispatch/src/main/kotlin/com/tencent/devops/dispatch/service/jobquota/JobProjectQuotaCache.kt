package com.tencent.devops.dispatch.service.jobquota

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.pojo.JobQuotaStatus
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 项目配额本地缓存
 * 使用Guava Cache实现，减少数据库查询压力
 */
object JobProjectQuotaCache {

    private val logger = LoggerFactory.getLogger(JobProjectQuotaCache::class.java)

    /**
     * 缓存配置：
     * - 最大缓存10000个项目配额
     * - 写入后5分钟过期
     * - 访问后5分钟过期
     */
    private val cache: Cache<String, JobQuotaStatus> = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build()

    /**
     * 生成缓存key
     */
    private fun buildCacheKey(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ): String {
        return "$projectId:${jobQuotaVmType.name}:$channelCode"
    }

    /**
     * 获取项目配额，如果缓存不存在则通过loader加载
     *
     * @param projectId 项目ID
     * @param jobQuotaVmType 构建机类型
     * @param channelCode 渠道代码
     * @param loader 加载函数，当缓存不存在时调用
     * @return 项目配额信息
     */
    fun get(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name,
        loader: () -> JobQuotaStatus
    ): JobQuotaStatus {
        val cacheKey = buildCacheKey(projectId, jobQuotaVmType, channelCode)

        return try {
            cache.get(cacheKey) {
                logger.info("Cache miss for key: $cacheKey, loading from database")
                loader()
            }
        } catch (e: Exception) {
            logger.error("Failed to load project quota from cache, key: $cacheKey", e)
            // 如果缓存加载失败，直接调用loader获取
            loader()
        }
    }

    /**
     * 手动放入缓存
     *
     * @param projectId 项目ID
     * @param jobQuotaVmType 构建机类型
     * @param channelCode 渠道代码
     * @param quota 项目配额信息
     */
    fun put(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name,
        quota: JobQuotaStatus
    ) {
        val cacheKey = buildCacheKey(projectId, jobQuotaVmType, channelCode)
        cache.put(cacheKey, quota)
        logger.debug("Put project quota into cache, key: $cacheKey")
    }

    /**
     * 使指定项目配额缓存失效
     *
     * @param projectId 项目ID
     * @param jobQuotaVmType 构建机类型
     * @param channelCode 渠道代码
     */
    fun invalidate(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ) {
        val cacheKey = buildCacheKey(projectId, jobQuotaVmType, channelCode)
        cache.invalidate(cacheKey)
        logger.debug("Invalidate project quota cache, key: $cacheKey")
    }

    /**
     * 使指定项目的所有配额缓存失效
     *
     * @param projectId 项目ID
     */
    fun invalidateProject(projectId: String) {
        val keysToInvalidate = cache.asMap().keys.filter { it.startsWith("$projectId:") }
        keysToInvalidate.forEach { cache.invalidate(it) }
        logger.debug("Invalidate all project quota cache for project: $projectId, count: ${keysToInvalidate.size}")
    }
}