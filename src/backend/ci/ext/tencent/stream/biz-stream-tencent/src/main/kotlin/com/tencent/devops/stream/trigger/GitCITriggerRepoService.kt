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

package com.tencent.devops.stream.trigger

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.config.StreamStorageBean
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamScmService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexCondition")
@Service
class GitCITriggerRepoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamStorageBean: StreamStorageBean,
    private val gitCISettingDao: StreamBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val streamScmService: StreamScmService,
    private val mergeConflictCheck: MergeConflictCheck,
    private val triggerExceptionService: TriggerExceptionService,
    private val tokenService: StreamGitTokenService,
    private val gitCITriggerService: GitCITriggerService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerRepoService::class.java)
    }

    // 通用不区分projectId，多流水线触发
    fun repoTriggerBuild(
        triggerPipelineList: List<StreamRepoHookEvent>?,
        gitRequestEvent: GitRequestEvent,
        event: GitEvent
    ): Boolean? {
        val start = LocalDateTime.now().timestampmilli()

        if (triggerPipelineList.isNullOrEmpty()) {
            logger.info("repo trigger pipeline list is empty ,skip it")
            return true
        }

        gitPipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = null,
            pipelineIds = triggerPipelineList.map { it.pipelineId }
        ).map {
            GitProjectPipeline(
                gitProjectId = it.gitProjectId,
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator,
                latestBuildInfo = null,
                latestBuildBranch = null
            )
        }.forEach { gitProjectPipeline ->
            val gitRequestEventForHandle = repoTriggerChangeGitRequestEvent(gitRequestEvent, gitProjectPipeline)
            triggerExceptionService.handle(
                requestEvent = gitRequestEventForHandle,
                gitEvent = event,
                basicSetting = null
            ) {
                triggerPerPipeline(
                    gitProjectPipeline = gitProjectPipeline,
                    gitRequestEventForHandle = gitRequestEventForHandle,
                    event = event
                )
            }
        }

        streamStorageBean.pipelineAndConflictTime(LocalDateTime.now().timestampmilli() - start)
        return true
    }

    private fun triggerPerPipeline(
        gitProjectPipeline: GitProjectPipeline,
        gitRequestEventForHandle: GitRequestEventForHandle,
        event: GitEvent
    ): Boolean {
        // 剔除不触发的情形
        gitCISettingDao.getSetting(dslContext, gitProjectPipeline.gitProjectId)?.let { setting ->
            try {
                CheckStreamSetting.checkGitProjectConf(gitRequestEventForHandle, event, setting)
            } catch (triggerException: TriggerException) {
                return false
            }

            // 校验mr请求是否产生冲突，已合并的无需检查
            if (event is GitMergeRequestEvent &&
                event.object_attributes.action != TGitMergeActionKind.MERGE.value &&
                !mergeConflictCheck.checkMrConflict(
                    projectId = gitProjectPipeline.gitProjectId,
                    gitRequestEventForHandle = gitRequestEventForHandle,
                    event = event,
                    path2PipelineExists = mapOf(gitProjectPipeline.filePath to gitProjectPipeline),
                    gitProjectConf = setting,
                    gitToken = handleGetToken(gitRequestEventForHandle)!!
                )
            ) {
                return false
            }

            return gitCITriggerService.matchAndTriggerPipeline(
                gitRequestEventForHandle = gitRequestEventForHandle,
                event = event,
                path2PipelineExists = mapOf(gitProjectPipeline.filePath to gitProjectPipeline),
                gitProjectConf = setting
            )

        } ?: return false
    }

    private fun repoTriggerChangeGitRequestEvent(
        gitRequestEvent: GitRequestEvent,
        gitProjectPipeline: GitProjectPipeline
    ): GitRequestEventForHandle {
        return GitRequestEventForHandle(
            id = gitRequestEvent.id,
            // 不管Mr怎么样，远程仓库触发都是指向主库
            gitProjectId = gitProjectPipeline.gitProjectId,
            branch = streamScmService.getProjectInfoRetry(
                token = tokenService.getToken(gitProjectPipeline.gitProjectId),
                gitProjectId = gitProjectPipeline.gitProjectId.toString(),
                useAccessToken = true
            ).defaultBranch!!,
            userId = gitRequestEvent.userId,
            checkRepoTrigger = true,
            gitRequestEvent = gitRequestEvent
        )
    }

    private fun handleGetToken(
        gitRequestEventForHandle: GitRequestEventForHandle,
        isMrEvent: Boolean = false
    ): String? {
        return triggerExceptionService.handleErrorCode(
            request = gitRequestEventForHandle,
            action = { tokenService.getToken(getProjectId(gitRequestEventForHandle, isMrEvent)) }
        )
    }

    // 获取项目ID，兼容没有source字段的旧数据，和fork库中源项目id不同的情况
    private fun getProjectId(gitRequestEventForHandle: GitRequestEventForHandle, isMrEvent: Boolean = false): Long {
        with(gitRequestEventForHandle) {
            return if (isMrEvent &&
                gitRequestEvent.sourceGitProjectId != null &&
                gitRequestEvent.sourceGitProjectId != gitProjectId &&
                !checkRepoTrigger
            ) {
                gitRequestEvent.sourceGitProjectId!!
            } else {
                gitProjectId
            }
        }
    }
}
