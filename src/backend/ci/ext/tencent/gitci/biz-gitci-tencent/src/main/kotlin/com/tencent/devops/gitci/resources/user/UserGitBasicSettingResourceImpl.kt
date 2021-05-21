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

package com.tencent.devops.gitci.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.user.UserGitBasicSettingResource
import com.tencent.devops.gitci.constant.GitCIConstant.DEVOPS_PROJECT_PREFIX
import com.tencent.devops.gitci.permission.GitCIV2PermissionService
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.service.GitCIBasicSettingService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGitBasicSettingResourceImpl @Autowired constructor(
    private val gitCIBasicSettingService: GitCIBasicSettingService,
    private val permissionService: GitCIV2PermissionService
) : UserGitBasicSettingResource {

    override fun disableGitCI(userId: String, projectId: String): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIAndOAuthAndEnable(
            userId = userId,
            projectId = projectId,
            gitProjectId = gitProjectId
        )
        return Result(gitCIBasicSettingService.updateProjectSetting(gitProjectId = gitProjectId, enableCi = false))
    }

    override fun getGitCIConf(userId: String, projectId: String): Result<GitCIBasicSetting?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIPermission(userId, projectId)
        return Result(gitCIBasicSettingService.getGitCIConf(gitProjectId))
    }

    override fun saveGitCIConf(userId: String, gitCIBasicSetting: GitCIBasicSetting): Result<Boolean> {
        val gitProjectId = gitCIBasicSetting.gitProjectId
        checkParam(userId)
        permissionService.checkGitCIAndOAuthAndEnable(
            userId = userId,
            projectId = "$DEVOPS_PROJECT_PREFIX$gitProjectId",
            gitProjectId = gitProjectId
        )
        return Result(gitCIBasicSettingService.saveGitCIConf(userId, gitCIBasicSetting))
    }

    override fun updateEnableUser(userId: String, projectId: String, enableUserId: String): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        return Result(
            gitCIBasicSettingService.updateProjectSetting(
                gitProjectId = gitProjectId,
                enableUserId = enableUserId
            )
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
