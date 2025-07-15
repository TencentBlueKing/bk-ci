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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserBuildParametersResource
import com.tencent.devops.process.pojo.BuildFormRepositoryValue
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.common.pipeline.pojo.BuildEnvParameters
import com.tencent.devops.common.pipeline.pojo.BuildParameterGroup
import com.tencent.devops.process.pojo.pipeline.PipelineBuildParamFormProp
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.webhook.TriggerBuildParamUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNUSED")
@RestResource
class UserBuildParametersResourceImpl @Autowired constructor(
    private val client: Client,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val scmProxyService: ScmProxyService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) : UserBuildParametersResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserBuildParametersResourceImpl::class.java)
        private val paramToContext = PipelineVarUtil.contextVarMap().map {
            it.value to it.key
        }.toMap()
    }

    override fun getCommonBuildParams(userId: String): Result<List<BuildEnvParameters>> {
        return Result(TriggerBuildParamUtils.getBasicBuildParams())
    }

    override fun getCommonParams(userId: String): Result<List<BuildParameterGroup>> {
        return Result(
            listOf(
                BuildParameterGroup(
                    name = TriggerBuildParamUtils.getBasicParamName(),
                    params = TriggerBuildParamUtils.getBasicBuildParams().map {
                        it.copy(name = paramToContext[it.name] ?: it.name)
                    }.sortedBy { it.name }
                ),
                BuildParameterGroup(
                    name = TriggerBuildParamUtils.getJobParamName(),
                    params = TriggerBuildParamUtils.getJobBuildParams()
                ),
                BuildParameterGroup(
                    name = TriggerBuildParamUtils.getStepParamName(),
                    params = TriggerBuildParamUtils.getStepBuildParams()
                )
            )
        )
    }

    override fun getTriggerParams(
        userId: String,
        atomCodeList: List<String?>
    ): Result<List<BuildParameterGroup>> {
        val buildParameterGroups = mutableListOf<BuildParameterGroup>()
        atomCodeList.filterNotNull().distinct().forEach { atomCode ->
            buildParameterGroups.addAll(
                TriggerBuildParamUtils.getTriggerParamNameMap(
                    atomCode = atomCode
                )
            )
        }
        return Result(buildParameterGroups)
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
            .distinctBy { it.key }
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

    override fun listRepositoryHashId(
        userId: String,
        projectId: String,
        repositoryType: String?,
        permission: Permission,
        aliasName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<BuildFormRepositoryValue>> {
        return Result(
            listRepositoryInfo(
                userId = userId,
                projectId = projectId,
                repositoryType = repositoryType,
                page = page,
                pageSize = pageSize,
                aliasName = aliasName
            ).map { BuildFormRepositoryValue(id = it.repositoryHashId!!, name = it.aliasName) }
        )
    }

    override fun listPermissionPipeline(
        userId: String,
        projectId: String,
        permission: com.tencent.devops.process.pojo.Permission,
        excludePipelineId: String?,
        pipelineName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<BuildFormValue>> {
        val pipelineList = pipelineListFacadeService.hasPermissionList(
            userId = userId,
            projectId = projectId,
            permission = permission,
            excludePipelineId = excludePipelineId,
            filterByPipelineName = pipelineName,
            page = page,
            pageSize = pageSize
        ).records
        return Result(
            pipelineList.map { BuildFormValue(it.pipelineName, it.pipelineName) }
        )
    }

    override fun listGitRefs(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        search: String?
    ): Result<List<BuildFormValue>> {
        val repositoryConfig = RepositoryConfigUtils.buildConfig(repositoryId, repositoryType)
        return Result(
            getGitRefs(
                projectId = projectId,
                repositoryConfig = repositoryConfig,
                search = search
            ).map { BuildFormValue(it, it) }
        )
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
        return result.distinct()
    }

    override fun buildParamFormProp(
        userId: String,
        projectId: String,
        pipelineId: String,
        includeConst: Boolean?,
        includeNotRequired: Boolean?,
        version: Int?,
        isTemplate: Boolean?
    ): Result<List<PipelineBuildParamFormProp>> {
        val buildParamFormProp = pipelineBuildFacadeService.getBuildParamFormProp(
            projectId = projectId,
            pipelineId = pipelineId,
            includeConst = includeConst,
            includeNotRequired = includeNotRequired,
            userId = userId,
            version = version,
            isTemplate = isTemplate
        )
        return Result(buildParamFormProp)
    }
}
