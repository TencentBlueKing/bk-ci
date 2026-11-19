/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.sdk.tapd.service

import com.tencent.devops.repository.sdk.tapd.AutoRetryTapdClient
import com.tencent.devops.repository.sdk.tapd.request.AppInstalledWorkspacesRequest
import com.tencent.devops.repository.sdk.tapd.request.GetBugFieldRequest
import com.tencent.devops.repository.sdk.tapd.request.GetBugRequest
import com.tencent.devops.repository.sdk.tapd.request.GetStoryFieldRequest
import com.tencent.devops.repository.sdk.tapd.request.GetStoryRequest
import com.tencent.devops.repository.sdk.tapd.request.GetWorkspaceInfoRequest
import com.tencent.devops.repository.sdk.tapd.request.GrantedWorkspacesRequest
import com.tencent.devops.repository.sdk.tapd.request.UserParticipantProjectsRequest
import com.tencent.devops.scm.pojo.tapd.ParticipantWorkspaceResponse
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdBugFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStory
import com.tencent.devops.scm.pojo.tapd.TapdStoryFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdWorkspace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * TAPD 业务对象查询服务实现
 */
@Service
class TapdItemService @Autowired constructor(
    private val autoRetryTapdClient: AutoRetryTapdClient
) : ITapdItemService {

    override fun getStoryInfo(workspaceId: String, storyId: String): TapdStory? {
        if (workspaceId.isBlank() || storyId.isBlank()) {
            logger.warn("invalid tapd story query|workspaceId=$workspaceId|storyId=$storyId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetStoryRequest(workspaceId = workspaceId, id = storyId)
            )
            result.data?.firstOrNull()?.story
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd story|workspaceId=$workspaceId|storyId=$storyId", ignored)
            null
        }
    }

    override fun getBugInfo(workspaceId: String, bugId: String): TapdBug? {
        if (workspaceId.isBlank() || bugId.isBlank()) {
            logger.warn("invalid tapd bug query|workspaceId=$workspaceId|bugId=$bugId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetBugRequest(workspaceId = workspaceId, id = bugId)
            )
            result.data?.firstOrNull()?.bug
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd bug|workspaceId=$workspaceId|bugId=$bugId", ignored)
            null
        }
    }

    override fun getBugFieldsInfo(workspaceId: String): TapdBugFieldConfig? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd bug field query|workspaceId=$workspaceId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetBugFieldRequest(workspaceId = workspaceId)
            )
            result.data
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd bug field|workspaceId=$workspaceId", ignored)
            null
        }
    }

    override fun getWorkspaceInfo(workspaceId: String): TapdWorkspace? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd workspace query|workspaceId=$workspaceId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetWorkspaceInfoRequest(workspaceId = workspaceId)
            )
            result.data?.workspace
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd workspace|workspaceId=$workspaceId", ignored)
            null
        }
    }

    override fun listParticipantWorkspaces(nick: String, status: String?): List<ParticipantWorkspaceResponse> {
        if (nick.isBlank()) {
            logger.warn("invalid tapd participant workspaces query")
            return emptyList()
        }
        return try {
            val result = autoRetryTapdClient.execute(
                UserParticipantProjectsRequest(nick = nick, status = status)
            )
            if (result.status != TAPD_SUCCESS_STATUS) {
                logger.warn(
                    "tapd participant workspaces unexpected status|status=${result.status}|info=${result.info}"
                )
                emptyList()
            } else {
                result.data.orEmpty()
            }
        } catch (ignored: Exception) {
            logger.warn("fail to list tapd participant workspaces", ignored)
            emptyList()
        }
    }

    override fun listGrantedWorkspaceIds(): Set<String>? {
        return try {
            val ids = mutableSetOf<String>()
            var page = 1
            while (page <= MAX_PAGE) {
                val result = autoRetryTapdClient.execute(
                    GrantedWorkspacesRequest(limit = PAGE_SIZE, page = page)
                )
                if (result.status != TAPD_SUCCESS_STATUS) {
                    logger.warn(
                        "tapd granted workspaces unexpected status|status=${result.status}|info=${result.info}"
                    )
                    return null
                }
                val data = result.data ?: return null
                val list = data.list ?: return null
                list.forEach { item ->
                    item.app?.workspaceId?.takeIf { it.isNotBlank() }?.let(ids::add)
                }
                val count = data.pager?.count ?: list.size
                if (list.isEmpty() || page * PAGE_SIZE >= count) break
                page++
            }
            ids
        } catch (ignored: Exception) {
            logger.warn("fail to list tapd granted workspaces", ignored)
            null
        }
    }

    override fun listInstalledWorkspaceIds(): Set<String>? {
        return try {
            val ids = mutableSetOf<String>()
            var page = 1
            while (page <= MAX_PAGE) {
                val result = autoRetryTapdClient.execute(
                    AppInstalledWorkspacesRequest(page = page, perPage = PAGE_SIZE)
                )
                if (result.status != TAPD_SUCCESS_STATUS) {
                    logger.warn(
                        "tapd installed workspaces unexpected status|status=${result.status}|info=${result.info}"
                    )
                    return null
                }
                val data = result.data ?: return null
                val list = data.workspaces.orEmpty()
                list.forEach { item ->
                    item.workspaceId?.takeIf { it.isNotBlank() }?.let(ids::add)
                }
                val total = maxOf(data.total, list.size)
                if (list.isEmpty() || page * PAGE_SIZE >= total) break
                page++
            }
            ids
        } catch (ignored: Exception) {
            logger.warn("fail to list tapd installed workspaces", ignored)
            null
        }
    }

    override fun getStoryFieldConfig(workspaceId: String): TapdStoryFieldConfig? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd story field query|workspaceId=$workspaceId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetStoryFieldRequest(workspaceId = workspaceId)
            )
            if (result.status != TAPD_SUCCESS_STATUS) {
                logger.warn(
                    "tapd story fields unexpected status|status=${result.status}|info=${result.info}"
                )
                null
            } else {
                result.data
            }
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd story fields|workspaceId=$workspaceId", ignored)
            null
        }
    }

    override fun getBugFieldConfig(workspaceId: String, allOptions: Boolean): TapdBugFieldConfig? {
        if (workspaceId.isBlank()) {
            logger.warn("invalid tapd bug field config query|workspaceId=$workspaceId")
            return null
        }
        return try {
            val result = autoRetryTapdClient.execute(
                GetBugFieldRequest(
                    workspaceId = workspaceId,
                    allOptions = if (allOptions) 1 else null
                )
            )
            if (result.status != TAPD_SUCCESS_STATUS) {
                logger.warn(
                    "tapd bug fields unexpected status|status=${result.status}|info=${result.info}"
                )
                null
            } else {
                result.data
            }
        } catch (ignored: Exception) {
            logger.warn("fail to query tapd bug field config|workspaceId=$workspaceId", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdItemService::class.java)
        private const val TAPD_SUCCESS_STATUS = 1
        private const val PAGE_SIZE = 200
        private const val MAX_PAGE = 10
    }
}
