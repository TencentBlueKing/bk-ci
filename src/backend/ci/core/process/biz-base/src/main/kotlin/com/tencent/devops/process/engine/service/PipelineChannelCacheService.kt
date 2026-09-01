/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.process.engine.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.pipeline.enums.ChannelCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 流水线渠道缓存
 * 流水线渠道几乎不变，故使用 Caffeine 缓存以减轻 DB 压力。
 */
@Service
class PipelineChannelCacheService @Autowired constructor(
    private val pipelineInfoService: PipelineInfoService
) {

    private val cache: Cache<String, ChannelCode?> = Caffeine.newBuilder()
        .maximumSize(200000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build()

    /**
     * 根据 projectId、pipelineId 获取流水线渠道；未命中或未查到则返回 null
     */
    fun getChannelCode(projectId: String?, pipelineId: String?): ChannelCode? {
        if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank()) return null
        return cache.get(cacheKey(projectId, pipelineId)) {
            pipelineInfoService.getPipelineInfo(projectId, pipelineId)?.channelCode
        }
    }

    private fun cacheKey(projectId: String, pipelineId: String) = "$projectId:$pipelineId"
}
