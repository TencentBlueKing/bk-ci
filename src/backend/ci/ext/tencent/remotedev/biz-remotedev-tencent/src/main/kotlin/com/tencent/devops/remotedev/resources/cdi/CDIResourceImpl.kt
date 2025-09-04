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

package com.tencent.devops.remotedev.resources.cdi

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.cdi.CDIResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class CDIResourceImpl @Autowired constructor(
    private val client: Client,
    private val workspaceService: WorkspaceService,
    private val checkTokenService: ClientTokenService,
    private val notifyControl: NotifyControl
) : CDIResource {
    companion object {
        private val logger = LoggerFactory.getLogger(CDIResourceImpl::class.java)
    }

    override fun getWorkspaceDetail(
        token: String,
        storeCode: String,
        userId: String,
        workspaceName: String
    ): Result<WeSecProjectWorkspace> {
        logger.info("getWorkspaceDetail|$userId|$storeCode|$workspaceName")
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspaceName,
                notStatus = null,
                hasCurrentUser = true
            ).firstOrNull() ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                params = arrayOf(
                    "This workspace was not found"
                )
            )
        )
    }

    override fun getLoginUserId(
        token: String,
        storeCode: String,
        userId: String,
        workspaceName: String
    ): Result<String> {
        logger.info("getLoginUserId|$userId|$storeCode|$workspaceName")
        return Result(kotlin.runCatching {
            checkNotNull(
                workspaceService.getWorkspaceDetail(
                    userId = "cdi",
                    workspaceName = workspaceName,
                    checkPermission = false
                )?.currentLoginUser?.first()
            )
        }.fold(
            { it },
            {
                logger.warn("getLoginUserId failed|$workspaceName", it)
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf(
                        "Currently, the cloud desktop login user cannot be obtained. " +
                            "You can try to log out of the cloud desktop and log in again."
                    )
                )
            }
        )
        )
    }

    override fun messageRegister(
        token: String,
        storeCode: String,
        userId: String,
        workspaceName: String,
        data: WorkspaceDesktopNotifyData
    ): Result<Boolean> {
        logger.info("messageRegister|$userId|$storeCode|$workspaceName|$data")
        val ws = workspaceService.getWorkspaceDetail(
            userId = "cdi",
            workspaceName = workspaceName,
            checkPermission = false
        ) ?: run {
            logger.warn("messageRegister get workspace detail failed|$workspaceName")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                params = arrayOf(
                    "This workspace was not found"
                )
            )
        }
        data.userIdList.forEach { user ->
            val check = client.get(ServiceProjectAuthResource::class).isProjectUser(
                token = checkTokenService.getSystemToken(),
                userId = user,
                projectCode = ws.projectId
            ).data == true
            if (!check) {
                logger.warn("messageRegister check user in project(${ws.projectId}) failed|$user")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf("$user must be a member of ${ws.projectId}")
                )
            }
        }

        notifyControl.notify4User(
            userIds = data.userIdList,
            notifyType = setOf(data.dataType),
            bodyParams = mutableMapOf(
                "operator" to data.operator,
                "messageContent" to data.data,
                "messageStartTime" to data.messageStartTime.toString(),
                "messageEndTime" to data.messageEndTime.toString(),
                "clientMsg" to data.data,
                "notifyTemplateCode" to data.notifyTemplateCode
            )
        )
        return Result(true)
    }
}
