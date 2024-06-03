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

package com.tencent.devops.process.strategy.bus.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.strategy.bus.IUserPipelinePermissionCheckStrategy
import org.springframework.stereotype.Component

@Component
class UserArchivedPipelinePermissionCheckStrategy : IUserPipelinePermissionCheckStrategy {

    override fun checkUserPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ) {
        val pipelinePermissionService = SpringContextUtil.getBean(PipelinePermissionService::class.java)
        val language = I18nUtil.getLanguage()
        if (permission == AuthPermission.ARCHIVE) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language = language,
                    params = arrayOf(userId, projectId, permission.getI18n(language), pipelineId)
                )
            )
        } else {
            // 归档流水线除了归档流水线权限外，其它权限都统一划归到管理已归档流水线权限
            val archivedPipelinePermission = AuthPermission.MANAGE_ARCHIVED_PIPELINE
            if (!pipelinePermissionService.checkPipelinePermission(
                    userId = userId,
                    projectId = projectId,
                    permission = archivedPipelinePermission,
                    authResourceType = AuthResourceType.PROJECT
                )
            ) {
                throw PermissionForbiddenException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = CommonMessageCode.USER_NO_PIPELINE_PERMISSION,
                        language = language,
                        params = arrayOf(archivedPipelinePermission.getI18n(language))
                    )
                )
            }
        }
    }
}
