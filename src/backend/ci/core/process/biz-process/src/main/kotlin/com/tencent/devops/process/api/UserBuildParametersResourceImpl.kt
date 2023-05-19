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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserBuildParametersResource
import com.tencent.devops.process.pojo.BuildFormRepositoryValue
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_VMSEQ_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.store.pojo.app.BuildEnvParameters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNUSED")
@RestResource
class UserBuildParametersResourceImpl @Autowired constructor(
    private val client: Client
) : UserBuildParametersResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserBuildParametersResourceImpl::class.java)
    }

    override fun getCommonBuildParams(userId: String): Result<List<BuildEnvParameters>> {
        return Result(
            data = listOf(
                BuildEnvParameters(
                    name = PIPELINE_START_USER_NAME,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_START_USER_NAME, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_START_TYPE,
                    desc = MessageUtil.getMessageByLocale(
                        PIPELINE_START_TYPE,
                        I18nUtil.getLanguage(userId),
                        arrayOf(StartType.values().joinToString("/") { it.name })
                    )
                ),
                BuildEnvParameters(
                    name = PIPELINE_BUILD_NUM,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_BUILD_NUM, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PROJECT_NAME,
                    desc = MessageUtil.getMessageByLocale(PROJECT_NAME, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_ID,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_ID, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_NAME,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_NAME, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_BUILD_ID,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_BUILD_ID, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_VMSEQ_ID,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_VMSEQ_ID, I18nUtil.getLanguage(userId))
                ),
                BuildEnvParameters(
                    name = PIPELINE_ELEMENT_ID,
                    desc = MessageUtil.getMessageByLocale(PIPELINE_ELEMENT_ID, I18nUtil.getLanguage(userId))
                )
            )
        )
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
}
