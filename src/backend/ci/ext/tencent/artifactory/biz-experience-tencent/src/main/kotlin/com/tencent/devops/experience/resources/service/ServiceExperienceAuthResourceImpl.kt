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
 *
 */

package com.tencent.devops.experience.resources.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.service.ServiceExperienceAuthResource
import com.tencent.devops.experience.service.AuthExperienceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceExperienceAuthResourceImpl @Autowired constructor(
    val authExperienceService: AuthExperienceService
) : ServiceExperienceAuthResource {

    override fun experienceTaskInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authExperienceService.getExperienceTask(
                    projectId = projectId,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt(),
                    token = token
                )
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authExperienceService.getExperienceTaskInfo(ids, token)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authExperienceService.searchExperienceTask(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt(),
                    token = token
                )
            }
        }
        return null
    }

    override fun experienceGroup(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: "" // FETCH_INSTANCE_INFO场景下iam不会传parentId
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return authExperienceService.getExperienceGroup(
                    projectId = projectId,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt(),
                    token = token
                )
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return authExperienceService.getExperienceGroupInfo(ids, token)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                return authExperienceService.searchExperienceGroup(
                    projectId = projectId,
                    keyword = callBackInfo.filter.keyword,
                    limit = page.offset.toInt(),
                    offset = page.limit.toInt(),
                    token = token
                )
            }
        }
        return null
    }
}
