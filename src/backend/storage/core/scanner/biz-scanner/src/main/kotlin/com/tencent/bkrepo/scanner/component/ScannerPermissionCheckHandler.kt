/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.component

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.permission.PermissionCheckHandler
import com.tencent.bkrepo.common.security.permission.Principal
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.scanner.model.SubScanTaskDefinition
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping

@Primary
@Component
class ScannerPermissionCheckHandler(
    private val permissionManager: PermissionManager
) : PermissionCheckHandler {

    override fun onPermissionCheck(userId: String, permission: Permission) {
        when (permission.type) {
            ResourceType.PROJECT -> checkProjectPermission(permission)
            else -> {}
        }
    }

    override fun onPrincipalCheck(userId: String, principal: Principal) {
        permissionManager.checkPrincipal(userId, principal.type)
    }

    fun checkProjectPermission(projectId: String, action: PermissionAction) {
        permissionManager.checkProjectPermission(action, projectId)
    }

    fun checkNodePermission(
        projectId: String,
        repoName: String,
        fullPath: String,
        action: PermissionAction,
        anonymous: Boolean = false
    ) {
        val repoDetail = repoDetail(projectId, repoName)
        permissionManager.checkNodePermission(
            action,
            projectId,
            repoName,
            fullPath,
            public = repoDetail.public,
            anonymous = anonymous
        )
    }

    fun checkSubtaskPermission(subtask: SubScanTaskDefinition, action: PermissionAction) {
        with(subtask) {
            checkNodePermission(projectId, repoName, fullPath, action)
        }
    }

    private fun checkProjectPermission(permission: Permission) {
        val uriAttribute = HttpContextHolder
            .getRequest()
            .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
        require(uriAttribute is Map<*, *>)
        val projectId = uriAttribute[PROJECT_ID]?.toString() ?: throw PermissionException()
        checkProjectPermission(projectId, permission.action)
    }

    private fun repoDetail(projectId: String, repoName: String) =
        ArtifactContextHolder.getRepoDetail(ArtifactContextHolder.RepositoryId(projectId, repoName))
}
