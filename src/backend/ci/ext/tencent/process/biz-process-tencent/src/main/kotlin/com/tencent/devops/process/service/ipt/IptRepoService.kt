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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class IptRepoService @Autowired constructor(
    private val client: Client,
    private val authPermissionApi: AuthPermissionApi,
    private val dslContext: DSLContext,
    private val pipelineBuildVarDao: PipelineBuildVarDao
) {

    fun getCommitBuildArtifactorytInfo(
        projectId: String,
        pipelineId: String,
        userId: String,
        commitId: String,
        filePath: String?
    ): IptBuildArtifactoryInfo {
        logger.info("get commit build artifactory info: $projectId, $pipelineId, $userId, $commitId")
        checkPermission(projectId, pipelineId, userId)

        val buildId = getBuildByCommitId(projectId, pipelineId, commitId)
            ?: throw NotFoundException("can not find build for commit")

        val searchFiles = if (filePath != null) listOf(filePath) else null
        val searchProperty = SearchProps(searchFiles, mapOf("buildId" to buildId, "pipelineId" to pipelineId))
        val fileList = client.get(ServiceIptResource::class)
            .searchFileAndProperty(userId, projectId, searchProperty).data?.records ?: listOf()

        return IptBuildArtifactoryInfo(buildId, fileList)
    }

    private fun getBuildByCommitId(projectId: String, pipelineId: String, commitId: String): String? {
        val headCommits = pipelineBuildVarDao.getVarsByProjectAndPipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            key = "DEVOPS_GIT_REPO_HEAD_COMMIT_ID",
            value = commitId,
            offset = 0,
            limit = 1
        )
        return headCommits.firstOrNull()?.buildId
    }

    private fun checkPermission(projectId: String, pipelineId: String, userId: String) {
        val result = authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = BSPipelineAuthServiceCode(),
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            permission = AuthPermission.DOWNLOAD
        )
        if (!result) throw PermissionForbiddenException("用户($userId)在工程($projectId)下没有流水线${pipelineId}下载构建权限")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
