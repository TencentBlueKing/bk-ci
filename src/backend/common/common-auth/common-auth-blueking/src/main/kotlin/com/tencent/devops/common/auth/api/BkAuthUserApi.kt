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

package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.auth.api.pojo.BkResourceUserRequest
import com.tencent.devops.common.auth.api.pojo.BkResourceUserResponse
import com.tencent.devops.common.auth.api.utils.AuthUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BkAuthUserApi @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val authUtils: AuthUtils
) {
    /**
     * 根据资源查询用户
     */
    fun getUserByResources(
        scopeType: String,
        scopeId: String,
        actionId: BkAuthPermission,
        resourceType: BkAuthResourceType,
        resourceIds: Set<String>,
        systemId: String,
        projectCode: String,
        projectSecret: String
    ): List<String> {
        val uri = "/bkiam/api/v1/perm/systems/$systemId/resources-perms-principals/search"

//        logger.info("开始调用权限中心根据资源查询用户，uri:$uri , systemId= $systemId")

        try {
            val resources = resourceIds.map { BkResourceUserRequest.ResourceId(it, resourceType.value) }
            val requestBean = BkResourceUserRequest(
                scopeType, scopeId,
                listOf(BkResourceUserRequest.ResourcesAction(actionId.value, resources, resourceType.value))
            )

            val requestBeanString = objectMapper.writeValueAsString(requestBean)
            // 发送请求
            val responseBody =
                authUtils.doAuthPostRequest(uri, JSONObject(requestBeanString), projectCode, projectSecret)
            val responseBean =
                jacksonObjectMapper().readValue<BkResourceUserResponse>(responseBody.toString())

            if (!responseBean.result) {
                logger.error("bkiam get user by resource failed, msg: ${responseBean.message}")
                return emptyList()
            }
//            logger.info("结束调用权限中心根据资源查询用户，uri:$uri , systemId= $systemId")

            val resData = responseBean.data!!.getOrNull(0) ?: return emptyList()
            return resData.principals!!.map { it.principalId }
        } catch (ignored: Exception) {
            logger.error("bkiam, getUser by Resources exception, msg: $ignored")
            throw RemoteServiceException(ignored.message!!)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthUserApi::class.java)
    }
}