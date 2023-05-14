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

package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.api.constant.CommonMessageCode.NOT_MEMBER_AND_NOT_OPEN_SOURCE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission

object ActionTypeUtils {
    private val executeAction = ExecuteAction()
    private val downloadAction = DownloadAction()
    private val viewAction = ViewAction()
    private val projectViewAction = WebCheckAction()

    fun getActionType(action: String): PermissionAction? {
        val authPermission = AuthPermission.get(action)
        return when {
            executeAction.permissionSet().contains(authPermission) -> {
                executeAction
            }
            downloadAction.permissionSet().contains(authPermission) -> {
                downloadAction
            }
            viewAction.permissionSet().contains(authPermission) -> {
                viewAction
            }
            projectViewAction.permissionSet().contains(authPermission) -> {
                projectViewAction
            }
            else -> null
        }
    }

    interface PermissionAction {
        fun permissionSet(): Set<AuthPermission>

        fun permissionCheck(isProjectMember: Boolean, isPublicProject: Boolean, isDevelopUp: Boolean): Boolean
    }

    class ExecuteAction : PermissionAction {
        override fun permissionSet(): Set<AuthPermission> {
            val actions = mutableSetOf<AuthPermission>()
            actions.add(AuthPermission.CREATE)
            actions.add(AuthPermission.DEPLOY)
            actions.add(AuthPermission.EDIT)
            actions.add(AuthPermission.DELETE)
            actions.add(AuthPermission.USE)
            actions.add(AuthPermission.EXECUTE)
            actions.add(AuthPermission.ENABLE)
            actions.add(AuthPermission.MANAGE)
            return actions
        }

        /**
         * 执行类操作,必须为项目的developer以上
         */
        override fun permissionCheck(
            isProjectMember: Boolean,
            isPublicProject: Boolean,
            isDevelopUp: Boolean
        ): Boolean {
            if (isProjectMember && isDevelopUp) {
                return true
            }
            return false
        }
    }

    class DownloadAction : PermissionAction {
        override fun permissionSet(): Set<AuthPermission> {
            val actions = mutableSetOf<AuthPermission>()
            actions.add(AuthPermission.DOWNLOAD)
            return actions
        }

        /**
         * 下载类操作,必须为项目的成员
         */
        override fun permissionCheck(
            isProjectMember: Boolean,
            isPublicProject: Boolean,
            isDevelopUp: Boolean
        ): Boolean {
            if (isProjectMember) {
                return true
            }
            return false
        }
    }

    class ViewAction : PermissionAction {
        override fun permissionSet(): Set<AuthPermission> {
            val actions = mutableSetOf<AuthPermission>()
            actions.add(AuthPermission.VIEW)
            actions.add(AuthPermission.SHARE)
            actions.add(AuthPermission.LIST)
            return actions
        }

        /**
         * 查看类操作规则：
         * 1. 项目成员可看
         * 2. 非项目成员,公共项目可看
         * 3. 非项目成员，闭源项目不可看
         */
        override fun permissionCheck(
            isProjectMember: Boolean,
            isPublicProject: Boolean,
            isDevelopUp: Boolean
        ): Boolean {
            if (isProjectMember) {
                return true
            } else if (isPublicProject) {
                return true
            }
            return false
        }
    }

    class WebCheckAction : PermissionAction {
        override fun permissionSet(): Set<AuthPermission> {
            val actions = mutableSetOf<AuthPermission>()
            actions.add(AuthPermission.WEB_CHECK)
            return actions
        }

        /**
         * 页面按钮操作规则：
         * 1. 项目成员可操作
         * 2. 非项目成员,公共项目校验不通过
         * 3. 非项目成员，闭源项目抛异常
         */
        override fun permissionCheck(
            isProjectMember: Boolean,
            isPublicProject: Boolean,
            isDevelopUp: Boolean
        ): Boolean {
            if (isProjectMember) {
                return true
            } else if (isPublicProject) {
                return false
            } else {
                throw ErrorCodeException(errorCode = NOT_MEMBER_AND_NOT_OPEN_SOURCE)
            }
            return false
        }
    }
}
