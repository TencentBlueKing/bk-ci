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
 *
 */

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.constant.HTTP_403
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.service.pipeline.PipelineYamlVersionResolver
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlFacadeService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFacadeService::class.java)
    }

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        return pipelineYamlService.getPipelineYamlVo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun yamlExistInDefaultBranch(
        projectId: String,
        pipelineId: String
    ): Boolean {
        return pipelineYamlService.yamlExistInDefaultBranch(
            projectId = projectId,
            pipelineIds = listOf(pipelineId)
        )[pipelineId] ?: false
    }

    /**
     * 构建yaml流水线触发变量
     */
    fun buildYamlManualParamMap(projectId: String, pipelineId: String): Map<String, BuildParameters>? {
        val pipelineYamlInfo = pipelineYamlInfoDao.get(
            dslContext = dslContext, projectId = projectId, pipelineId = pipelineId
        ) ?: return null
        return mutableMapOf(
            BK_REPO_WEBHOOK_HASH_ID to BuildParameters(BK_REPO_WEBHOOK_HASH_ID, pipelineYamlInfo.repoHashId),
            PIPELINE_WEBHOOK_BRANCH to BuildParameters(
                PIPELINE_WEBHOOK_BRANCH, pipelineYamlInfo.defaultBranch ?: ""
            )
        )
    }

    /**
     * 获取pac流水线指定分支的版本信息
     * 通过解析分支下文件md5值获取对应的版本信息
     */
    fun getPipelineYamlVersion(
        projectId: String,
        pipelineId: String,
        branch: String,
        yamlParams: MutableMap<String, BuildParameters> = mutableMapOf()
    ): Int? {
        // 不是PAC流水线
        val yamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return null
        return pipelineYamlVersionResolver.resolvePipelineRefVersion(
            projectId = projectId,
            repoHashId = yamlInfo.repoHashId,
            filePath = yamlInfo.filePath,
            ref = branch
        ).let {
            // 记录当前分支信息
            yamlParams[BK_REPO_GIT_WEBHOOK_BRANCH] = BuildParameters(key = BK_REPO_GIT_WEBHOOK_BRANCH, value = branch)
            it
        }
    }

    /**
     * 获取代码库关联信息
     */
    fun getRepository(projectId: String, repoHashId: String): Repository {
        return client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryType = RepositoryType.ID,
            repositoryId = repoHashId
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.GIT_NOT_FOUND,
            params = arrayOf(repoHashId)
        )
    }

    fun getServiceRepository(
        projectId: String,
        repository: Repository
    ) = try {
        client.get(ServiceScmRepositoryApiResource::class).getServerRepository(
            projectId = projectId,
            authRepository = AuthRepository(repository)
        ).data
    } catch (ignored: RemoteServiceException) {
        throw when (ignored.httpStatus) {
            // 目标仓库被删除
            HTTP_404 -> ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                params = arrayOf(repository.projectName)
            )

            HTTP_401, HTTP_403 -> ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_USER_NO_PUSH_PERMISSION,
                params = arrayOf(repository.userName, repository.projectName)
            )

            else -> ignored
        }
    } catch (ignored: Exception) {
        throw ignored
    }

    fun getServiceBranch(
        projectId: String,
        repository: Repository,
        page: Int,
        pageSize: Int,
        search: String?
    ) = try {
        client.get(ServiceScmRepositoryApiResource::class).listBranches(
            projectId = projectId,
            authRepository = AuthRepository(repository),
            page = page,
            pageSize = pageSize,
            search = search
        ).data
    } catch (ignored: Exception) {
        logger.warn("failed to get service branch", ignored)
        null
    }

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String
    ) = pipelineYamlService.getPipelineYamlInfo(
        projectId = projectId,
        pipelineId = pipelineId
    )
}
