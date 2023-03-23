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

package com.tencent.devops.process.service.ipt

import com.tencent.devops.artifactory.api.service.ServiceIptResource
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NOT_PERMISSION_DOWNLOAD
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.repository.api.ServiceGitCommitResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Suppress("LongParameterList")
@Service
class IptRepoService @Autowired constructor(
    private val client: Client,
    private val authPermissionApi: AuthPermissionApi
) {

    fun getCommitBuildArtifactorytInfo(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        commitId: String,
        filePath: String?
    ): IptBuildArtifactoryInfo {
        checkPermission(projectId, pipelineId, userId)

        val buildId2 = getBuildByCommitId(projectId, pipelineId, commitId)
            ?: throw NotFoundException("can not find build for commit")

        val searchFiles = if (filePath != null) listOf(filePath) else null
        val searchProperty = SearchProps(searchFiles, mapOf("buildId" to buildId2, "pipelineId" to pipelineId))
        val fileList = client.get(ServiceIptResource::class)
            .searchFileAndProperty(userId, projectId, searchProperty).data?.records ?: listOf()

        logger.info("getCommitBuildArtifactorytInfo: $projectId|$pipelineId|$buildId|" +
            "beq=${buildId == buildId2}|$userId|$commitId")
        return IptBuildArtifactoryInfo(buildId2, fileList)
    }

    private fun getBuildByCommitId(projectId: String, pipelineId: String, commitId: String): String? {
        return client.get(ServiceGitCommitResource::class).queryCommitInfo(pipelineId, commitId).data?.buildId
    }

    private fun checkPermission(projectId: String, pipelineId: String, userId: String) {
        val result = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = BSPipelineAuthServiceCode(),
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            permission = AuthPermission.DOWNLOAD,
            resourceCode = pipelineId
        )
        if (!result) throw PermissionForbiddenException(
            MessageUtil.getMessageByLocale(
                messageCode = USER_NOT_PERMISSION_DOWNLOAD,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(userId, projectId, pipelineId)
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IptRepoService::class.java)
    }
}
