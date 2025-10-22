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

package com.tencent.devops.environment.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvironmentAuthResource
import com.tencent.devops.environment.service.AuthEnvService
import com.tencent.devops.environment.service.AuthNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceEnvironmentAuthResourceImpl @Autowired constructor(
    val authNodeService: AuthNodeService,
    val authEnvService: AuthEnvService
) : ServiceEnvironmentAuthResource {

    override fun environmentInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authEnvService.getEnv(projectId, page.offset.toInt(), page.limit.toInt(), token)
            }

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authEnvService.getEnvInfo(ids, token)
            }

            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authEnvService.searchEnv(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.limit.toInt(),
                    offset = page.offset.toInt(),
                    token = token
                )
            }

            else -> {}
        }
        return null
    }

    override fun nodeInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        return when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                authNodeService.getNode(projectId, page.offset.toInt(), page.limit.toInt(), token)
            }

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                authNodeService.getNodeInfo(ids, token)
            }

            CallbackMethodEnum.SEARCH_INSTANCE -> {
                authNodeService.searchNode(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.limit.toInt(),
                    offset = page.offset.toInt(),
                    token = token
                )
            }
            CallbackMethodEnum.LIST_RESOURCE_AUTHORIZATION -> {
                authNodeService.listNodeAuthorization(
                    projectId = projectId,
                    limit = page.limit.toInt(),
                    offset = page.offset.toInt(),
                    token = token
                )
            }
            else -> {
                null
            }
        }
    }
}
