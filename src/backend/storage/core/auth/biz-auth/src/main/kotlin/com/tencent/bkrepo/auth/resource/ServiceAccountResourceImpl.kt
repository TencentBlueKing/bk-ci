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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServiceAccountResource
import com.tencent.bkrepo.auth.pojo.Account
import com.tencent.bkrepo.auth.pojo.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.CredentialSet
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceAccountResourceImpl @Autowired constructor(
    private val accountService: AccountService
) : ServiceAccountResource {

    override fun listAccount(): Response<List<Account>> {
        val accountList = accountService.listAccount()
        return ResponseBuilder.success(accountList)
    }

    override fun createAccount(request: CreateAccountRequest): Response<Account?> {
        return ResponseBuilder.success(accountService.createAccount(request))
    }

    override fun updateAccount(appId: String, locked: Boolean): Response<Boolean> {
        accountService.updateAccountStatus(appId, locked)
        return ResponseBuilder.success(true)
    }

    override fun deleteAccount(appId: String): Response<Boolean> {
        accountService.deleteAccount(appId)
        return ResponseBuilder.success(true)
    }

    override fun getCredential(appId: String): Response<List<CredentialSet>> {
        val credential = accountService.listCredentials(appId)
        return ResponseBuilder.success(credential)
    }

    override fun createCredential(appId: String): Response<List<CredentialSet>> {
        val result = accountService.createCredential(appId)
        return ResponseBuilder.success(result)
    }

    override fun deleteCredential(appId: String, accesskey: String): Response<List<CredentialSet>> {
        val result = accountService.deleteCredential(appId, accesskey)
        return ResponseBuilder.success(result)
    }

    override fun updateCredential(appId: String, accesskey: String, status: CredentialStatus): Response<Boolean> {
        accountService.updateCredentialStatus(appId, accesskey, status)
        return ResponseBuilder.success(true)
    }

    override fun checkCredential(accesskey: String, secretkey: String): Response<String?> {
        val result = accountService.checkCredential(accesskey, secretkey)
        return ResponseBuilder.success(result)
    }
}
