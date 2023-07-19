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

package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.auth.api.callback.OpenAuthResourceCallBackResource
import com.tencent.devops.auth.service.iam.PermissionResourceCallbackService
import com.tencent.devops.common.api.exception.TokenForbiddenException
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpenAuthResourceCallBackResourceImpl @Autowired constructor(
    private val permissionResourceCallbackService: PermissionResourceCallbackService,
    private val authTokenApi: AuthTokenApi
) : OpenAuthResourceCallBackResource {

    override fun projectInfo(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO {
        logger.info("callBackInfo: $callBackInfo, token: $token")
        if (!authTokenApi.checkToken(token)) {
            logger.warn("auth token check fail: $token")
            throw TokenForbiddenException("auth token check fail")
        }
        return permissionResourceCallbackService.getProject(callBackInfo, token)
    }

    override fun resourceList(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        logger.info("callBackInfo: $callBackInfo, token: $token")
        if (!authTokenApi.checkToken(token)) {
            logger.warn("auth token check fail: $token")
            throw TokenForbiddenException("auth token check fail")
        }
        return permissionResourceCallbackService.getInstanceByResource(
            callBackInfo = callBackInfo,
            token = token
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenAuthResourceCallBackResourceImpl::class.java)
    }
}
