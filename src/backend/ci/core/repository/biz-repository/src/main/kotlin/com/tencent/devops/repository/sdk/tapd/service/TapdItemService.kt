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
import com.tencent.devops.repository.sdk.tapd.request.GetBugFieldRequest
import com.tencent.devops.repository.sdk.tapd.request.GetBugRequest
import com.tencent.devops.repository.sdk.tapd.request.GetStoryRequest
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdBugFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStory
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

    companion object {
        private val logger = LoggerFactory.getLogger(TapdItemService::class.java)
    }
}
