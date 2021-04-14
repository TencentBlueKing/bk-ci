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

package com.tencent.bkrepo.auth.service.bkauth

import com.tencent.bkrepo.auth.pojo.enums.BkAuthPermission
import com.tencent.bkrepo.auth.pojo.enums.BkAuthResourceType
import com.tencent.bkrepo.auth.pojo.enums.BkAuthServiceCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ci 流水线权限查询
 */
@Service
class BkAuthPipelineService(
    private val bkAuthService: BkAuthService
) {
    fun listPermissionedPipelines(uid: String, projectId: String): List<String> {
        return bkAuthService.getUserResourceByPermission(
            user = uid,
            serviceCode = BkAuthServiceCode.PIPELINE,
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            permission = BkAuthPermission.LIST,
            supplier = null,
            retryIfTokenInvalid = true
        )
    }

    fun hasPermission(uid: String, projectId: String, pipelineId: String): Boolean {
        logger.info("hasPermission: uid: $uid, projectId: $projectId, pipelineId: $pipelineId")
        return bkAuthService.validateUserResourcePermission(
            user = uid,
            serviceCode = BkAuthServiceCode.PIPELINE,
            resourceType = BkAuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            resourceCode = pipelineId,
            permission = BkAuthPermission.DOWNLOAD,
            retryIfTokenInvalid = true
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPipelineService::class.java)
    }
}
