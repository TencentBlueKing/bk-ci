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

package com.tencent.devops.project.resources

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectAuthCallBackResource
import com.tencent.devops.project.constant.ProjectMessageCode.ERROR_AUTH_CALLBACK_METHOD_NOT_SUPPORT
import com.tencent.devops.project.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectAuthCallBackResourceImpl @Autowired constructor(
    val authProjectService: AuthProjectService
) : ServiceProjectAuthCallBackResource {
    override fun projectInfo(token: String, callBackInfo: CallbackRequestDTO): Result<CallbackBaseResponseDTO> {
        val method = callBackInfo.method
        val page = callBackInfo.page
        val callbackBaseResponseDTO = when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                authProjectService.getProjectList(page, token)
            }
            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                val attribute = callBackInfo.filter.attributeList
                authProjectService.getProjectInfo(ids, token, attribute)
            }
            CallbackMethodEnum.SEARCH_INSTANCE -> {
                authProjectService.searchProjectInstances(callBackInfo.filter.keyword, page, token)
            }
            else ->
                throw ErrorCodeException(
                    errorCode = ERROR_AUTH_CALLBACK_METHOD_NOT_SUPPORT,
                    params = arrayOf(method.method),
                    defaultMessage = "iam callback method ${method.method} not support"
                )
        }
        return Result(callbackBaseResponseDTO)
    }
}
