/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.security.permission

import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.util.SecurityUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory

@Aspect
class PermissionAspect(
    private val permissionCheckHandler: PermissionCheckHandler
) {

    /**
     * type=repo: 要求接口路径上必须使用标准格式/{projectId}/{repoName}，否则无法解析到仓库信息
     * type=node: 要求必须解析过ArtifactInfo，否则无法解析到仓库信息
     */
    @Around("@annotation(com.tencent.bkrepo.common.security.permission.Permission)")
    @Throws(Throwable::class)
    fun around(point: ProceedingJoinPoint): Any? {
        val signature = point.signature as MethodSignature
        val method = signature.method
        val permission = method.getAnnotation(Permission::class.java)
        val userId = SecurityUtils.getUserId()

        return try {
            permissionCheckHandler.onPermissionCheck(userId, permission)
            permissionCheckHandler.onPermissionCheckSuccess()
            point.proceed()
        } catch (exception: PermissionException) {
            logger.warn("User[${SecurityUtils.getPrincipal()}] check permission [${permission.string()}] failed.")
            permissionCheckHandler.onPermissionCheckFailed(exception)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionAspect::class.java)
    }
}
