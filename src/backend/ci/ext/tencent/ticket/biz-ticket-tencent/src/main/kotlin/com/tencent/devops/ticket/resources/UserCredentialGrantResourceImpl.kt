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
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.ticket.api.UserCredentialGrantResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.service.CredentialService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCredentialGrantResourceImpl @Autowired constructor(
    private val credentialService: CredentialService
) : UserCredentialGrantResource {
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
        val authGroupList = mutableListOf<BkAuthGroup>()
        authGroupList.add(BkAuthGroup.MANAGER)
        authGroupList.add(BkAuthGroup.DEVELOPER)
        authGroupList.add(BkAuthGroup.TESTER)
        credentialService.userCreate(userId, projectId, credential, authGroupList)
        return Result(true)
    }
}
