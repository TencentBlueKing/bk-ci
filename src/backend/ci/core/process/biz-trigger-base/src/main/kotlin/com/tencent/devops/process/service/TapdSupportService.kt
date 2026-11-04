/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.process.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.tapd.ServiceTapdResource
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdBugFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStory
import com.tencent.devops.scm.pojo.tapd.TapdWorkspace
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * TAPD 支持服务
 */
@Service
class TapdSupportService(private val client: Client) {

    /**
     * TAPD 项目信息本地缓存
     *
     * 项目名基本不会变化，缓存 1 小时可显著减少远端调用；
     * 查询失败或返回空时不缓存，避免瞬时故障被长期记忆。
     */
    private val workspaceCache: Cache<String, TapdWorkspace> = Caffeine.newBuilder()
        .maximumSize(WORKSPACE_CACHE_MAX_SIZE)
        .expireAfterWrite(WORKSPACE_CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .build()

    /**
     * 查询 TAPD 需求详情
     */
    fun getStoryInfo(workspaceId: String, storyId: String): TapdStory? {
        if (workspaceId.isBlank() || storyId.isBlank()) {
            logger.warn("invalid tapd story query|workspaceId=$workspaceId|storyId=$storyId")
            return null
        }
        return try {
            client.get(ServiceTapdResource::class).getStoryInfo(
                workspaceId = workspaceId,
                storyId = storyId
            ).data
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd story|workspaceId=$workspaceId|storyId=$storyId", ignored)
            null
        }
    }

    /**
     * 查询 TAPD 缺陷详情
     */
    fun getBugInfo(workspaceId: String, bugId: String): TapdBug? {
        if (workspaceId.isBlank() || bugId.isBlank()) {
            logger.warn("invalid tapd bug query|workspaceId=$workspaceId|bugId=$bugId")
            return null
        }
        return try {
            client.get(ServiceTapdResource::class).getBugInfo(
                workspaceId = workspaceId,
                bugId = bugId
            ).data
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd bug|workspaceId=$workspaceId|bugId=$bugId", ignored)
            null
        }
    }

    /**
     * 查询 TAPD 缺陷详情
     */
    fun getBugFieldsInfo(workspaceId: String): TapdBugFieldConfig? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd bug fields query|workspaceId=$workspaceId")
            return null
        }
        return try {
            client.get(ServiceTapdResource::class).getBugFieldsInfo(
                workspaceId = workspaceId
            ).data
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd bug|workspaceId=$workspaceId", ignored)
            null
        }
    }

    /**
     * 查询 TAPD 项目信息（带本地缓存）
     */
    fun getWorkspaceInfo(workspaceId: String): TapdWorkspace? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd workspace query|workspaceId=$workspaceId")
            return null
        }
        workspaceCache.getIfPresent(workspaceId)?.let { return it }
        return try {
            val info = client.get(ServiceTapdResource::class).getWorkspaceInfo(
                workspaceId = workspaceId
            ).data
            info?.also { workspaceCache.put(workspaceId, it) }
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd workspace|workspaceId=$workspaceId", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdSupportService::class.java)
        private const val WORKSPACE_CACHE_MAX_SIZE = 1000L
        private const val WORKSPACE_CACHE_EXPIRE_HOURS = 1L
    }
}
