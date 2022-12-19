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

package com.tencent.devops.common.web.security.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.auth.api.util.PermissionUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

class PermissionAuthFilter(
    private val actions: List<CodeCCAuthAction>
) : ContainerRequestFilter {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PermissionAuthFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val authExPermissionApi = SpringContextUtil.getBean(AuthExPermissionApi::class.java)
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)

        val user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID)

        // 如果是管理员就直接校验通过
        if (authExPermissionApi.isAdminMember(user)) {
            return
        }


        val taskId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_TASK_ID)
        val projectId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_PROJECT_ID)
        if (user.isNullOrBlank() || taskId.isNullOrBlank() || projectId.isNullOrBlank()) {
            logger.error("insufficient param info! user: $user, taskId: $taskId, projectId: $projectId")
            throw UnauthorizedException("insufficient param info!")
        }

        val taskCreateFrom = authTaskService.getTaskCreateFrom(taskId.toLong())
        logger.info("task create from: $taskCreateFrom, user: $user， projectId: $projectId")
        val result = when (taskCreateFrom) {
            ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() -> {
                val pipelineAuthResults: MutableList<BkAuthExResourceActionModel> = mutableListOf()
                // 工蜂CI项目没有在蓝鲸权限中心注册过，需要走Oauth鉴权与工蜂权限对齐
                if (Pattern.compile("^git_[0-9]+$").matcher(projectId).find()) {
                    logger.info("auth gongfeng ci project, taskId: {} | projectId: {} | user: {}", taskId, projectId, user)
                    val isPass = authExPermissionApi.validateGongfengPermission(user, taskId, projectId, actions)
                    pipelineAuthResults.add(BkAuthExResourceActionModel("pipeline_auth", null, null,
                            isPass))
                } else {
                    // 普通流水线在蓝鲸权限中心鉴权
                    val codeccActions = PermissionUtil.getCodeCCPermissionsFromActions(actions)
                    val pipelinePermissionAuthResult = authExPermissionApi.validateTaskBatchPermission(
                            user,
                            taskId,
                            projectId,
                            codeccActions
                    )
                    var pipelineAuthPass = true
                    pipelinePermissionAuthResult.forEach {
                        if (it.isPass == false) {
                            pipelineAuthPass = false
                        }
                    }

                    if (pipelineAuthPass) {
                        pipelineAuthResults.add(BkAuthExResourceActionModel("pipeline_auth", null, null,
                                true))
                    } else {
                        pipelineAuthResults.add(BkAuthExResourceActionModel("pipeline_auth", null, null,
                                false))
                    }
                }
                pipelineAuthResults
            }
            ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() -> {
                if (!authExPermissionApi.validateGongfengPermission(user, taskId, projectId, actions)) {
                    logger.error("empty validate result: $user")
                    throw CodeCCException(CommonMessageCode.PERMISSION_DENIED, arrayOf(user))
                } else {
                    logger.info("gongfeng authorization pass, task id: $taskId")
                    return
                }
            }
            else -> {
                val codeccActions = PermissionUtil.getCodeCCPermissionsFromActions(actions)
                authExPermissionApi.validateTaskBatchPermission(
                    user,
                    taskId,
                    projectId,
                    codeccActions
                )
            }


        }


        if (result.isNullOrEmpty()) {
            logger.error("empty validate result: $user")
            throw UnauthorizedException("unauthorized user permission!")
        }
        result.forEach {
            if (it.isPass == true) {
                return
            }
        }
        logger.error("validate permission fail! user: $user")
        throw UnauthorizedException("unauthorized user permission!")
    }
}