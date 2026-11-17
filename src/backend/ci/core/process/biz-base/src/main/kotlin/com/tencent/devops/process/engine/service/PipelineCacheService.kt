package com.tencent.devops.process.engine.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.pipeline.enums.ChannelCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 流水线基础信息缓存。渠道几乎不变，名称会改，因此分两个缓存、两套过期时间。
 */
@Service
class PipelineCacheService @Autowired constructor(
    private val pipelineInfoService: PipelineInfoService
) {

    private val channelCache: Cache<String, ChannelCode?> = Caffeine.newBuilder()
        .maximumSize(200000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build()

    private val nameCache: Cache<String, String?> = Caffeine.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build()

    /**
     * 根据 projectId、pipelineId 获取流水线渠道；未命中或未查到则返回 null
     */
    fun getChannelCode(projectId: String?, pipelineId: String?): ChannelCode? {
        if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank()) return null
        return channelCache.get(cacheKey(projectId, pipelineId)) {
            pipelineInfoService.getPipelineInfo(projectId, pipelineId)?.channelCode
        }
    }

    /**
     * 根据 projectId、pipelineId 获取流水线名称；未命中或未查到则返回 null
     */
    fun getPipelineName(projectId: String?, pipelineId: String?): String? {
        if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank()) return null
        return nameCache.get(cacheKey(projectId, pipelineId)) {
            pipelineInfoService.getPipelineInfo(projectId, pipelineId)?.pipelineName
        }
    }

    private fun cacheKey(projectId: String, pipelineId: String) = "$projectId:$pipelineId"
}
