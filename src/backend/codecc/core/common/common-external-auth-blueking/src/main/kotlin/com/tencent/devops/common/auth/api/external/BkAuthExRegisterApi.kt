/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.external.AUTH_PRINCIPAL_TYPE
import com.tencent.devops.common.auth.api.pojo.external.AuthExResponse
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_CODE
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_SECRET
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceNameModel
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleDeleteModel
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleRegModel
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleResourceModel
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExResourceDeleteRequest
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExResourceRegRequest
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class BkAuthExRegisterApi @Autowired constructor(
        private val authPropertiesData: AuthExPropertiesData
) : AuthExRegisterApi {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthExRegisterApi::class.java)
    }

    /**
     * 注册代码检查任务
     */
    override fun registerCodeCCTask(
            user: String,
            taskId: String,
            taskName: String,
            projectId: String
    ): Boolean {
        val resources = BkAuthExResourceNameModel(
                scopeType = authPropertiesData.scopeType!!,
                scopeId = projectId,
                resourceType = authPropertiesData.codeccResourceType!!,
                resourceId = listOf(BkAuthExSingleResourceModel(
                        resourceType = authPropertiesData.codeccResourceType!!,
                        resourceId = taskId
                )),
                resourceName = taskName
        )
        val result = registerResource(
                systemId = authPropertiesData.systemId!!,
                creatorType = AUTH_PRINCIPAL_TYPE,
                creatorId = user,
                resources = listOf(resources)
        )
        if (!result.isSuccess()) {
            logger.error("register resource failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw CodeCCException(CommonMessageCode.PERMISSION_DENIED, arrayOf(user))
        }
        return result.data?.isCreated ?: false
    }


    /**
     * 删除代码检查任务
     */
    override fun deleteCodeCCTask(
            taskId: String,
            projectId: String
    ): Boolean {
        val resources = listOf(
                BkAuthExResourceNameModel(
                        scopeType = authPropertiesData.scopeType!!,
                        scopeId = projectId,
                        resourceType = authPropertiesData.codeccResourceType!!,
                        resourceId = listOf(BkAuthExSingleResourceModel(
                                resourceType = authPropertiesData.codeccResourceType!!,
                                resourceId = taskId
                        ))
                ))
        val result = deleteResource(
                systemId = authPropertiesData.systemId!!,
                resources = resources
        )
        if (!result.isSuccess()) {
            logger.error("delete resource failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("delete resource failed!")
        }
        return result.data?.isDeleted ?: false

    }


    /**
     * 调用api注册资源
     */
    private fun registerResource(
            systemId: String,
            creatorType: String,
            creatorId: String,
            resources: List<BkAuthExResourceNameModel>
    ): AuthExResponse<BkAuthExSingleRegModel> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${authPropertiesData.url}/bkiam/api/v1/perm/systems/$systemId/resources/batch-register"
        val bkAuthExResourceRegRequest = BkAuthExResourceRegRequest(
                creatorType = creatorType,
                creatorId = creatorId,
                resources = resources
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExResourceRegRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to authPropertiesData.codeccCode!!,
                HEADER_APP_SECRET to authPropertiesData.codeccSecret!!))
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<BkAuthExSingleRegModel>>() {})
    }


    /**
     * 调用api删除资源
     */
    private fun deleteResource(
            systemId: String,
            resources: List<BkAuthExResourceNameModel>
    ): AuthExResponse<BkAuthExSingleDeleteModel> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${authPropertiesData.url}/bkiam/api/v1/perm/systems/$systemId/resources/batch-delete"
        val bkAuthExResourceDeleteRequest = BkAuthExResourceDeleteRequest(
                resources = resources
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExResourceDeleteRequest)
        val result = OkhttpUtils.doHttpDelete(url, content, mapOf(
                HEADER_APP_CODE to authPropertiesData.codeccCode!!,
                HEADER_APP_SECRET to authPropertiesData.codeccSecret!!))
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<BkAuthExSingleDeleteModel>>() {})
    }

}