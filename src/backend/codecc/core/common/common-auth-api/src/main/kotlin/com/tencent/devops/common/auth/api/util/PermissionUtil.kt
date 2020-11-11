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


package com.tencent.devops.common.auth.api.util

import com.tencent.devops.common.auth.api.pojo.external.AuthRole
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PermissionUtil {
    private val logger: Logger = LoggerFactory.getLogger(PermissionUtil::class.java)
    /**
     * 根据角色列表获取权限列表
     */
    fun getCodeCCPermissionsFromActions(
            actions: List<CodeCCAuthAction>)
            : MutableSet<String> {
        var permissions = mutableSetOf<String>()
        for (action in actions) {
            permissions.add(action.actionName)
        }
        return permissions
    }

    /**
     * 根据角色列表获取权限列表
     */
    fun getPipelinePermissionsFromActions(
            actions: List<CodeCCAuthAction>)
            : MutableSet<String> {
        var codeccPermissions = getCodeCCPermissionsFromActions(actions)
        var pipelinePermissions = mutableSetOf<String>()
        for (role in AuthRole.values().reversedArray()) {
            val permissions = getNamesFromCodeccPermissions(role.codeccActions)
            if (permissions.containsAll(codeccPermissions)) {
                for (pipelinePermission in role.pipelineActions) {
                    pipelinePermissions.add(pipelinePermission.actionName)
                }
                logger.info("pipeline auth role: {}, pipeline permissions: {}", role.roleName, pipelinePermissions)
                break
            }
        }
        return pipelinePermissions
    }

    /**
     * 获取权限对应的名称清单
     */
    fun getNamesFromCodeccPermissions(codeCCAuthActions: List<CodeCCAuthAction>)
            : MutableSet<String> {
        var names = mutableSetOf<String>()
        for (codeccPermission in codeCCAuthActions) {
            names.add(codeccPermission.actionName)
        }
        return names
    }
}