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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.stream.api.user.UserStreamGitResource
import com.tencent.devops.stream.constant.StreamConstant.STREAM_CI_FILE_DIR
import com.tencent.devops.stream.constant.StreamConstant.STREAM_FILE_SUFFIX
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamCommitInfo
import com.tencent.devops.stream.pojo.StreamCreateFileInfo
import com.tencent.devops.stream.pojo.StreamGitMember
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamGitService
import com.tencent.devops.stream.service.StreamGitTransferService
import com.tencent.devops.stream.service.StreamPipelineService
import com.tencent.devops.stream.service.StreamProjectService
import com.tencent.devops.stream.service.StreamRequestService
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.pojo.StreamTriggerLock
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamGitResourceImpl @Autowired constructor(
    private val client: Client,
    private val permissionService: StreamPermissionService,
    private val streamGitService: StreamGitService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamProjectService: StreamProjectService,
    private val streamGitTransferService: StreamGitTransferService,
    private val streamRequestService: StreamRequestService,
    private val streamPipelineService: StreamPipelineService,
    private val redisOperation: RedisOperation
) : UserStreamGitResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserStreamGitResourceImpl::class.java)
    }

    override fun getGitCodeProjectInfo(userId: String, gitProjectId: String): Result<StreamGitProjectInfoWithProject?> {
        if (gitProjectId.isBlank()) {
            return Result(data = null)
        }
        val projectInfo = streamGitService.getProjectInfo(gitProjectId) ?: return Result(null)
        // 增加用户访问记录
        streamProjectService.addUserProjectHistory(
            userId = userId,
            projectId = GitCommonUtils.getCiProjectId(
                gitProjectId = projectInfo.gitProjectId
            )
        )
        val routerTag = client.get(ServiceProjectResource::class).get(
            englishName = GitCommonUtils.getCiProjectId(projectInfo.gitProjectId)
        ).data?.routerTag
        return Result(projectInfo.copy(routerTag = routerTag))
    }

    override fun getGitCodeProjectMembers(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: String?
    ): Result<List<StreamGitMember>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            streamGitTransferService.getProjectMember(
                userId = getOauthUser(userId, isEnableUser = true, gitProjectId = gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                search = search
            )
        )
    }

    override fun getGitCodeCommits(
        userId: String,
        projectId: String,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<StreamCommitInfo>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkStreamPermission(userId, projectId)
        return Result(
            streamGitTransferService.getCommits(
                userId = getOauthUser(userId = userId, isEnableUser = true, gitProjectId = gitProjectId),
                gitProjectId = gitProjectId,
                filePath = filePath,
                branch = branch,
                since = since,
                until = until,
                page = page,
                perPage = pageSize
            )
        )
    }

    override fun gitCodeCreateFile(
        userId: String,
        projectId: String,
        streamCreateFile: StreamCreateFileInfo
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        permissionService.checkStreamPermission(userId, projectId, AuthPermission.CREATE)
        permissionService.checkEnableStream(gitProjectId.toLong())
        val newFile = streamCreateFile.copy(
            filePath = getFilePath(streamCreateFile.filePath)
        )
        val createNewFileIsSuccess = streamGitTransferService.createNewFile(
            userId = getOauthUser(userId = userId, isEnableUser = false, gitProjectId = gitProjectId.toLong()),
            gitProjectId = gitProjectId,
            streamCreateFile = newFile
        )
        if (createNewFileIsSuccess) {
            createNewPipeLine(gitProjectId, newFile, userId)
        }
        return Result(
            createNewFileIsSuccess
        )
    }

    private fun createNewPipeLine(gitProjectId: String, newFile: StreamCreateFileInfo, userId: String) {
        val pipeline = StreamTriggerPipeline(
            gitProjectId = gitProjectId,
            pipelineId = "",
            filePath = newFile.filePath,
            displayName = newFile.filePath,
            enabled = true,
            creator = userId
        )
        // pipelineId可能为blank所以使用filePath为key
        val triggerLock = StreamTriggerLock(
            redisOperation = redisOperation,
            gitProjectId = gitProjectId,
            filePath = pipeline.filePath
        )
        val gitProjectCode = getProjectCode(gitProjectId)
        val realPipeline: StreamTriggerPipeline
        // 避免出现多个触发拿到空的pipelineId后依次进来创建，所以需要在锁后重新获取pipeline
        triggerLock.use {
            triggerLock.lock()
            realPipeline = getRealPipeLine(gitProjectId, pipeline)
            // 优先创建流水线为了前台显示
            if (realPipeline.pipelineId.isBlank()) {
                streamPipelineService.savePipeline(
                    pipeline = realPipeline,
                    userId = userId,
                    gitProjectId = gitProjectId.toLong(),
                    projectCode = gitProjectCode,
                    modelAndSetting = createTriggerModel(gitProjectCode),
                    updateLastModifyUser = true
                )
            }
        }
    }

    private fun getRealPipeLine(gitProjectId: String, pipeline: StreamTriggerPipeline) =
        streamPipelineService.getPipelineByFile(
            gitProjectId = gitProjectId.toLong(),
            filePath = pipeline.filePath
        )?.let {
            StreamTriggerPipeline(it)
        } ?: pipeline

    private fun getProjectCode(gitProjectId: String): String {
        return "git_$gitProjectId"
    }

    private fun createTriggerModel(projectCode: String) = PipelineModelAndSetting(
        model = Model(
            name = StreamPipelineUtils.genBKPipelineName(projectCode),
            desc = "",
            stages = listOf(
                Stage(
                    id = VMUtils.genStageId(1),
                    name = VMUtils.genStageId(1),
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "构建触发",
                            elements = listOf(
                                ManualTriggerElement(
                                    name = "手动触发",
                                    id = "T-1-1-1"
                                )
                            )
                        )
                    )
                )
            )
        ),
        setting = PipelineSetting()
    )

    // 默认在.ci目录下，.yml后缀
    private fun getFilePath(filePath: String): String {
        var newPath = filePath
        if (!filePath.startsWith("$STREAM_CI_FILE_DIR/")) {
            newPath = "$STREAM_CI_FILE_DIR/$newPath"
        }
        if (!filePath.endsWith(STREAM_FILE_SUFFIX)) {
            newPath = "${newPath}$STREAM_FILE_SUFFIX"
        }
        return newPath
    }

    override fun getGitCodeBranches(
        userId: String,
        projectId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: StreamBranchesOrder?,
        sort: StreamSortAscOrDesc?
    ): Result<List<String>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            streamGitTransferService.getProjectBranches(
                userId = getOauthUser(userId = userId, isEnableUser = true, gitProjectId = gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                orderBy = orderBy,
                sort = sort,
                search = search
            )
        )
    }

    override fun getTriggerUser(
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<String>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        return Result(
            streamRequestService.getRequestBuildTriggers(
                projectId = gitProjectId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    // 看是否使用stream 开启人的id
    private fun getOauthUser(userId: String, isEnableUser: Boolean, gitProjectId: Long): String {
        return if (isEnableUser) {
            val setting = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
            setting.enableUserId
        } else {
            userId
        }
    }
}
