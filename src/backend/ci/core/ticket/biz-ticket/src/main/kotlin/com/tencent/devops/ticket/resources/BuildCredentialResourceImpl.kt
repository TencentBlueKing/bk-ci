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

package com.tencent.devops.ticket.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.ticket.api.BuildCredentialResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildCredentialResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : BuildCredentialResource {
    @SensitiveApiPermission("get_credential")
    @BkTimed(extraTags = ["operate", "get"])
    override fun get(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        credentialId: String,
        publicKey: String,
        taskId: String?,
        oldTaskId: String?
    ): Result<CredentialInfo?> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqId")
        }
        if (vmName.isBlank()) {
            throw ParamBlankException("Invalid vmName")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (publicKey.isBlank()) {
            throw ParamBlankException("Invalid publicKey")
        }
        return Result(
            credentialService.buildGet(
                projectId = projectId,
                buildId = buildId,
                credentialId = credentialId,
                publicKey = publicKey,
                taskId = taskId ?: oldTaskId
            )
        )
    }

    @SensitiveApiPermission("get_credential")
    @BkTimed(extraTags = ["operate", "get"])
    override fun getAcrossProject(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        credentialId: String,
        targetProjectId: String,
        publicKey: String
    ): Result<CredentialInfo?> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqId")
        }
        if (vmName.isBlank()) {
            throw ParamBlankException("Invalid vmName")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (publicKey.isBlank()) {
            throw ParamBlankException("Invalid publicKey")
        }
        return Result(
            credentialService.buildGetAcrossProject(projectId, targetProjectId, buildId, credentialId, publicKey)
        )
    }

    @SensitiveApiPermission("get_credential")
    @BkTimed(extraTags = ["operate", "get"])
    override fun getDetail(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        vmName: String,
        taskId: String?,
        oldTaskId: String?,
        credentialId: String
    ): Result<Map<String, String>> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqId")
        }
        if (vmName.isBlank()) {
            throw ParamBlankException("Invalid vmName")
        }
        if (credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        // 这里兼容下旧版本sdk的header
        return Result(credentialService.buildGetDetail(
            projectId = projectId,
            buildId = buildId,
            taskId = taskId ?: oldTaskId,
            credentialId = credentialId
        ))
    }

    override fun create(userId: String, projectId: String, credential: CredentialCreate): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (credential.credentialId.isBlank()) {
            throw ParamBlankException("Invalid credentialId")
        }
        if (credential.v1.isBlank()) {
            throw ParamBlankException("Invalid credential")
        }
        credentialService.userCreate(userId, projectId, credential, null)
        return Result(true)
    }
}
