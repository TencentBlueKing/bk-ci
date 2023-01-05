/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.stream.v1.components

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.stream.v1.client.V1ScmClient
import com.tencent.devops.stream.v1.dao.V1GitCISettingDao
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventDao
import com.tencent.devops.stream.v1.pojo.enums.V1GitCICommitCheckState
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class V1SendCommitCheck @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmClient: V1ScmClient,
    private val gitRequestEventDao: V1GitRequestEventDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val gitCISettingDao: V1GitCISettingDao,
    private val gitPipelineResourceDao: V1GitPipelineResourceDao
) {
    fun sendCommitCheckV1(
        buildId: String,
        userId: String,
        streamBuildId: Long,
        requestEventId: Long,
        pipelineId: String,
        buildStatus: String
    ) {
        val requestEvent = gitRequestEventDao.getWithEvent(dslContext, requestEventId) ?: return

        // 当人工触发时不推送CommitCheck消息
        if (requestEvent.objectKind == StreamGitObjectKind.MANUAL.value) {
            return
        }

        // 更新流水线执行状态
        gitRequestEventBuildDao.updateBuildStatusById(
            dslContext = dslContext,
            id = streamBuildId,
            buildStatus = BuildStatus.valueOf(buildStatus)
        )

        val pipeline = gitPipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = listOf(pipelineId)
        ).getOrNull(0) ?: throw OperationException("git ci pipeline not exist")

        val gitProjectConf = gitCISettingDao.getSetting(dslContext, pipeline.gitProjectId)
            ?: throw OperationException("git ci all projectCode not exist")

        scmClient.pushCommitCheck(
            commitId = requestEvent.commitId,
            description = requestEvent.commitMsg ?: "",
            mergeRequestId = requestEvent.mergeRequestId,
            pipelineId = pipeline.pipelineId,
            buildId = buildId,
            userId = userId,
            status = buildStatus.getGitCommitCheckState(),
            context = "${pipeline.displayName}(${pipeline.filePath})",
            gitProjectConf = gitProjectConf
        )
    }
}

// 获取commit checkState
private fun String.getGitCommitCheckState(): V1GitCICommitCheckState {
    val status = try {
        BuildStatus.valueOf(this)
    } catch (e: Exception) {
        BuildStatus.UNKNOWN
    }
    // stage审核的状态专门判断为成功
    return when (status) {
        BuildStatus.REVIEWING -> {
            V1GitCICommitCheckState.PENDING
        }
        //  审核成功的阶段性状态
        BuildStatus.REVIEW_PROCESSED -> {
            V1GitCICommitCheckState.PENDING
        }
        else -> {
            if (status == BuildStatus.SUCCEED || status == BuildStatus.STAGE_SUCCESS) {
                V1GitCICommitCheckState.SUCCESS
            } else {
                V1GitCICommitCheckState.FAILURE
            }
        }
    }
}
