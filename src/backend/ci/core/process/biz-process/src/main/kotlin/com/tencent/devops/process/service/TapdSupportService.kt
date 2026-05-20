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

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.tapd.ServiceTapdResource
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdStory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TAPD 支持服务
 */
@Service
class TapdSupportService(private val client: Client) {

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

    companion object {
        private val logger = LoggerFactory.getLogger(TapdSupportService::class.java)
    }
}
