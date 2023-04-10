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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.builds.BuildArtifactoryFileTaskResource
import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.artifactory.service.FileTaskService
import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_HAVE_PROJECT_PERMISSIONS
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildArtifactoryFileTaskResourceImpl @Autowired constructor(
    private val client: Client,
    private val fileTaskService: FileTaskService
) : BuildArtifactoryFileTaskResource {

    override fun createFileTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        createFileTaskReq: CreateFileTaskReq
    ): Result<String> {
        checkUserPermission(userId, projectId)
        return Result(fileTaskService.createFileTask(userId, projectId, pipelineId, buildId, createFileTaskReq))
    }

    override fun getStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<FileTaskInfo?> {
        checkUserPermission(userId, projectId)
        return Result(fileTaskService.getStatus(userId, projectId, pipelineId, buildId, taskId))
    }

    override fun clearFileTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Result<Boolean> {
        checkUserPermission(userId, projectId)
        return Result(fileTaskService.clearFileTask(userId, projectId, pipelineId, buildId, taskId))
    }

    fun checkUserPermission(userId: String, projectId: String) {
        val projectSet = client.get(ServiceProjectResource::class).list(userId).data!!.map { it.projectCode }.toSet()
        if (!projectSet.contains(projectId)) {
            throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = USER_NOT_HAVE_PROJECT_PERMISSIONS,
                        params = arrayOf(userId, projectId)
                ),
                params = arrayOf("user[$userId]->project[$projectId]")
            )
        }
    }
}
