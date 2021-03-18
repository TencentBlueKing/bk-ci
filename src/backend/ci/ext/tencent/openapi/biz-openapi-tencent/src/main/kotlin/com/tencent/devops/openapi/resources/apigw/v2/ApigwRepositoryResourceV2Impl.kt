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
package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v2.ApigwRepositoryResourceV2
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLEncoder

@RestResource
class ApigwRepositoryResourceV2Impl @Autowired constructor(private val client: Client) : ApigwRepositoryResourceV2 {
    override fun listByProject(
        appCode: String?,
        apigwType: String?,
        organizationType: String,
        organizationId: Int,
        projectId: String,
        repositoryType: ScmType?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RepositoryInfo>?> {
        logger.info("get  repostitories list in project:,projectId=$projectId,repositoryType:$repositoryType,organizationId=$organizationId,organizationType=$organizationType")
        val verify = client.get(ServiceTxProjectResource::class)
            .verifyProjectByOrganization(projectId, organizationType, organizationId).data
        return if (verify != null && verify) {
            client.get(ServiceRepositoryResource::class).listByProject(
                projectId = projectId,
                repositoryType = repositoryType,
                page = page,
                pageSize = pageSize
            )
        } else {
            Result(data = null)
        }
    }

    override fun getAuthUrl(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        repoHashId: String?
    ): Result<String> {
        logger.info("getAuthUrl projectId[$projectId] userId[$userId] repoHashId[$repoHashId]")
        val authParams = mapOf(
            "projectId" to projectId,
            "userId" to userId,
            "repoId" to if (!repoHashId.isNullOrBlank()) HashUtil.decodeOtherIdToLong(repoHashId!!).toString() else "",
            "randomStr" to "BK_DEVOPS__${RandomStringUtils.randomAlphanumeric(8)}"
        )
        val authParamJsonStr = URLEncoder.encode(JsonUtil.toJson(authParams), "UTF-8")
        return client.get(ServiceGitRepositoryResource::class).getAuthUrl(authParamJsonStr)
    }

    override fun gitGet(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<String?> {
        logger.info("gitGet userId[$userId]")
        val gitToken = client.get(ServiceOauthResource::class).gitGet(userId)
        if (gitToken?.data == null) {
            return Result(HTTP_404)
        }
        return Result(gitToken.data!!.accessToken)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwRepositoryResourceV2Impl::class.java)
    }
}
