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

package com.tencent.devops.process.service.perm

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PermFixService @Autowired constructor(
    private val serviceCode: PipelineAuthServiceCode,
    private val authProjectApi: AuthProjectApi,
    private val pipelineInfoDao: PipelineInfoDao,
    private val dslContext: DSLContext,
    private val pipelinePermissionService: PipelinePermissionService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(PermFixService::class.java)
    }

    fun checkPermission(userId: String, projectId: String, pipelineId: String) {

        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE
            )) {

            val admin = authProjectApi.getProjectUsers(serviceCode, projectId, BkAuthGroup.MANAGER).firstOrNull()
            if (!admin.isNullOrBlank()) { // 遇到已经丢失权限的更新最后修改人为管理员
                pipelineInfoDao.update(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = admin,
                    updateVersion = false
                )
                LOG.warn("BKSystemErrorMonitor|BAD_USER|$userId|$projectId|$pipelineId|$admin")
            } else {
                LOG.error("BKSystemErrorMonitor|EMPTY_ADMIN|$projectId|$pipelineId")
            }
        }
    }
}
