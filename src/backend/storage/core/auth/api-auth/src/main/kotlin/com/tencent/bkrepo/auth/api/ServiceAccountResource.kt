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

package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AUTH_ACCOUNT_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_API_ACCOUNT_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_SERVICE_ACCOUNT_PREFIX
import com.tencent.bkrepo.auth.pojo.account.Account
import com.tencent.bkrepo.auth.pojo.account.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.token.CredentialSet
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.common.api.constant.AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Api(tags = ["SERVICE_ACCOUNT"], description = "服务-账号接口")
@Primary
@FeignClient(AUTH_SERVICE_NAME, contextId = "ServiceAccountResource")
@RequestMapping(AUTH_ACCOUNT_PREFIX, AUTH_SERVICE_ACCOUNT_PREFIX, AUTH_API_ACCOUNT_PREFIX)
interface ServiceAccountResource {

    @ApiOperation("查询所有账号账号")
    @GetMapping("/list")
    fun listAccount(): Response<List<Account>>

    @ApiOperation("创建账号")
    @PostMapping("/create")
    fun createAccount(
        @RequestBody request: CreateAccountRequest
    ): Response<Account?>

    @ApiOperation("更新账号状态账号")
    @PutMapping("/{appId}/{locked}")
    fun updateAccount(
        @ApiParam(value = "账户id")
        @PathVariable appId: String,
        @ApiParam(value = "账户id")
        @PathVariable locked: Boolean
    ): Response<Boolean>

    @ApiOperation("删除账号")
    @DeleteMapping("/delete/{appId}")
    fun deleteAccount(
        @ApiParam(value = "账户id")
        @PathVariable appId: String
    ): Response<Boolean>

    @ApiOperation("获取账户下的ak/sk对")
    @GetMapping("/credential/list/{appId}")
    fun getCredential(
        @ApiParam(value = "账户id")
        @PathVariable appId: String
    ): Response<List<CredentialSet>>

    @ApiOperation("创建ak/sk对")
    @PostMapping("/credential/{appId}")
    fun createCredential(
        @ApiParam(value = "账户id")
        @PathVariable appId: String
    ): Response<List<CredentialSet>>

    @ApiOperation("删除ak/sk对")
    @DeleteMapping("/credential/{appId}/{accesskey}")
    fun deleteCredential(
        @ApiParam(value = "账户id")
        @PathVariable appId: String,
        @ApiParam(value = "账户id")
        @PathVariable accesskey: String
    ): Response<List<CredentialSet>>

    @ApiOperation("更新ak/sk对状态")
    @PutMapping("/credential/{appId}/{accesskey}/{status}")
    fun updateCredential(
        @ApiParam(value = "账户id")
        @PathVariable appId: String,
        @ApiParam(value = "accesskey")
        @PathVariable accesskey: String,
        @ApiParam(value = "status")
        @PathVariable status: CredentialStatus
    ): Response<Boolean>

    @ApiOperation("校验ak/sk")
    @GetMapping("/credential/{accesskey}/{secretkey}")
    fun checkCredential(
        @ApiParam(value = "accesskey")
        @PathVariable accesskey: String,
        @ApiParam(value = "secretkey")
        @PathVariable secretkey: String
    ): Response<String?>
}
