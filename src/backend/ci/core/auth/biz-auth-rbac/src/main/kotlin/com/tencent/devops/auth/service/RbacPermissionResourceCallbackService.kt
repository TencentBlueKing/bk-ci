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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.devops.auth.service.iam.PermissionResourceCallbackService
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectAuthCallBackResource

class RbacPermissionResourceCallbackService constructor(
    private val client: Client,
    private val authResourceService: AuthResourceService
) : PermissionResourceCallbackService {

    override fun getProject(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO {
        return client.get(ServiceProjectAuthCallBackResource::class).projectInfo(
            token = token,
            callBackInfo = callBackInfo
        ).data!!
    }

    override fun getInstanceByResource(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val resourceType = callBackInfo.type
        return when (method) {
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                fetchInstance(
                    resourceType = resourceType,
                    iamResourceCodes = ids
                )
            }
            else ->
                null
        }
    }

    private fun fetchInstance(
        resourceType: String,
        iamResourceCodes: List<String>
    ): FetchInstanceInfoResponseDTO {
        val instanceInfoDTOList = authResourceService.listByIamCodes(
            resourceType = resourceType,
            iamResourceCodes = iamResourceCodes
        ).map {
            val entity = InstanceInfoDTO()
            entity.id = it.iamResourceCode
            entity.displayName = it.resourceName
            entity
        }
        val result = FetchInstanceInfo()

        if (instanceInfoDTOList.isEmpty()) {
            return result.buildFetchInstanceFailResult()
        }
        return result.buildFetchInstanceResult(instanceInfoDTOList)
    }
}
