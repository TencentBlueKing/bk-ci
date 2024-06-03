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

package com.tencent.devops.log.strategy.bus.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.log.service.LogPermissionService
import com.tencent.devops.log.strategy.bus.IUserLogPermissionCheckStrategy
import org.springframework.stereotype.Component

@Component
class UserNormalLogPermissionCheckStrategy : IUserLogPermissionCheckStrategy {

    override fun checkUserLogPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission
    ) {
        val logPermissionService = SpringContextUtil.getBean(LogPermissionService::class.java)
        val language = I18nUtil.getLanguage()
        if (!logPermissionService.verifyUserLogPermission(
                projectCode = projectId,
                pipelineId = pipelineId,
                userId = userId,
                permission = permission
            )
        ) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                params = arrayOf(userId, projectId, permission.getI18n(language), pipelineId)
            )
        }
    }
}
