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

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.pipeline.AppModelDetail
import com.tencent.devops.process.service.app.AppBuildService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.stream.api.service.ServiceGitForAppResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
@SuppressWarnings("SwallowedException", "TooGenericExceptionCaught", "ThrowsCount")
class AppPipelineBuildTencentResourceImpl @Autowired constructor(
    private val appBuildService: AppBuildService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val client: Client,
    private val bkTag: BkTag,
    private val scmProxyService: ScmProxyService
) : AppPipelineBuildTencentResource {

    @Value("\${gitCI.tag:#{null}}")
    private val gitCI: String? = null

    override fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<AppModelDetail> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        // 对特殊的buildid进行处理。
        var buildIdReal = when (buildId) {
            "latest" -> {
                pipelineRuntimeService.getLatestBuildId(projectId, pipelineId)
            }

            "latestSucceeded" -> {
                pipelineRuntimeService.getLatestSucceededBuildId(projectId, pipelineId)
            }

            "latestFailed" -> {
                pipelineRuntimeService.getLatestFailedBuildId(projectId, pipelineId)
            }

            "latestFinished" -> {
                pipelineRuntimeService.getLatestFinishedBuildId(projectId, pipelineId)
            }

            else -> {
                buildId
            }
        }
        if (buildIdReal == null) {
            buildIdReal = buildId
        }

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS
        val data = appBuildService.getBuildDetail(userId, projectId, pipelineId, buildIdReal, channelCode)
        if (channelCode == ChannelCode.GIT) {
            bkTag.invokeByTag(gitCI) {
                try {
                    client.get(ServiceGitForAppResource::class)
                        .getGitCIPipeline(projectId, pipelineId).data?.displayName
                } catch (e: Exception) {
                    null
                }
            }?.let { data.pipelineName = it }
        }
        return Result(data)
    }

    override fun listRepoRefs(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        search: String?
    ): Result<List<BuildFormValue>> {
        val repositoryConfig = RepositoryConfigUtils.buildConfig(repositoryId, repositoryType)
        val repoScmType = scmProxyService.getRepo(
            projectId = projectId,
            repositoryConfig = repositoryConfig
        ).getScmType()
        val formValues = when (repoScmType) {
            // Git库需要拉分支和Tag
            in listOf(ScmType.CODE_GIT, ScmType.CODE_TGIT, ScmType.CODE_GITLAB, ScmType.GITHUB) -> {
                getGitRefs(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig,
                    search = search
                )
            }

            // Svn库仅拉分支
            ScmType.CODE_SVN -> {
                scmProxyService.listBranches(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig,
                    search = search
                ).data ?: listOf()
            }

            else -> {
                throw IllegalArgumentException("Unknown repo type($repoScmType)")
            }
        }.map { BuildFormValue(it, it) }
        return Result(formValues)
    }

    override fun listRepositoryAliasName(
        userId: String,
        projectId: String,
        repositoryType: String?,
        permission: Permission,
        aliasName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<BuildFormValue>> {
        return Result(
            listRepositoryInfo(
                userId = userId,
                projectId = projectId,
                repositoryType = repositoryType,
                page = page,
                pageSize = pageSize,
                aliasName = aliasName
            ).map { BuildFormValue(it.aliasName, it.aliasName) }
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun listRepositoryInfo(
        userId: String,
        projectId: String,
        repositoryType: String?,
        page: Int?,
        pageSize: Int?,
        aliasName: String?
    ) = try {
        client.get(ServiceRepositoryResource::class).hasPermissionList(
            userId = userId,
            projectId = projectId,
            permission = Permission.LIST,
            repositoryType = repositoryType,
            page = page,
            pageSize = pageSize,
            aliasName = aliasName
        ).data?.records ?: emptyList()
    } catch (ignore: Exception) {
        logger.warn("[$userId|$projectId] Fail to get the repository list", ignore)
        emptyList()
    }

    private fun getGitRefs(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        search: String?
    ): List<String> {
        val result = mutableListOf<String>()
        val branches = scmProxyService.listBranches(
            projectId = projectId,
            repositoryConfig = repositoryConfig,
            search = search
        ).data ?: listOf()
        val tags = scmProxyService.listTags(
            projectId = projectId,
            repositoryConfig = repositoryConfig,
            search = search
        ).data ?: listOf()
        result.addAll(branches)
        result.addAll(tags)
        return result
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AppPipelineBuildTencentResourceImpl::class.java)
    }
}
