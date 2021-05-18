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
import com.tencent.devops.common.auth.api.pojo.external.AuthExResponse
import com.tencent.devops.common.auth.api.pojo.external.KEY_BACKEND_ACCESS_TOKEN
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleDeleteModel
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleRegModel
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExResourceDeleteRequest
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExResourceRegRequest
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

class InternalAuthExRegisterApi @Autowired constructor(
        private val authPropertiesData: AuthExPropertiesData,
        private val redisTemplate: RedisTemplate<String, String>
) : AuthExRegisterApi {
    /**
     * 注册代码检查任务
     */
    override fun registerCodeCCTask(user: String, taskId: String, taskName: String, projectId: String): Boolean {
        val result = registerResource(
                projectCode = projectId,
                serviceCode = authPropertiesData.codeccServiceCode,
                resourceCode = taskId,
                resourceName = taskName,
                resourceType = authPropertiesData.codeccResourceType,
                creator = user
        )
        if (result.code != 0) {
            logger.error("register resource failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw CodeCCException(CommonMessageCode.PERMISSION_DENIED, arrayOf(user))
        }
        return true
    }

    /**
     * 删除代码检查任务
     */
    override fun deleteCodeCCTask(taskId: String, projectId: String): Boolean {
        val result = deleteResource(
                projectCode = projectId,
                serviceCode = authPropertiesData.codeccServiceCode,
                resourceCode = taskId,
                resourceType = authPropertiesData.codeccResourceType
        )
        if (result.code != 0) {
            logger.error("delete resource failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("delete resource failed!")
        }
        return true
    }

    /**
     * 调用api注册资源
     */
    private fun registerResource(
            projectCode: String,
            serviceCode: String?,
            resourceCode: String,
            resourceName: String,
            resourceType: String?,
            creator: String
    ): AuthExResponse<BkAuthExSingleRegModel> {
        val url = authPropertiesData.url + "/resource?access_token=" + getBackendAccessToken()
        val authExResourceRegRequest = InternalAuthExResourceRegRequest(
                projectCode = projectCode,
                serviceCode = serviceCode,
                resourceCode = resourceCode,
                resourceName = resourceName,
                resourceType = resourceType,
                creator = creator
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(authExResourceRegRequest)
        val result = OkhttpUtils.doHttpPost(url, content, emptyMap())
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<String>>() {})
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthExRegisterApi::class.java)
    }

    /**
     * 调用api删除资源
     */
    private fun deleteResource(
            projectCode: String,
            serviceCode: String?,
            resourceCode: String,
            resourceType: String?
    ): AuthExResponse<BkAuthExSingleDeleteModel> {
        val url = "${authPropertiesData.url}/resource?access_token=" + getBackendAccessToken()
        val bkAuthExResourceDeleteRequest = InternalAuthExResourceDeleteRequest(
                projectCode = projectCode,
                serviceCode = serviceCode,
                resourceCode = resourceCode,
                resourceType = resourceType
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExResourceDeleteRequest)
        val result = OkhttpUtils.doHttpDelete(url, content, emptyMap())
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<String>>() {})
    }

    /**
     * 查询非用户态Access Token
     */
    private fun getBackendAccessToken(): String {
        return redisTemplate.opsForValue().get(KEY_BACKEND_ACCESS_TOKEN)
    }
}