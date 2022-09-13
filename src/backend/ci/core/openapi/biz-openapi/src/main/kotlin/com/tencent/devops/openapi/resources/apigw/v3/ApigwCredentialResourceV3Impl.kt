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
package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwCredentialResourceV3
import com.tencent.devops.ticket.api.UserCredentialResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCredentialResourceV3Impl @Autowired constructor(private val client: Client) :
    ApigwCredentialResourceV3 {

    @BkTimed(extraTags = ["operate", "get"])
    override fun list(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<Page<CredentialWithPermission>> {
        logger.info("OPENAPI_CREDENTIAL_V3|$userId|list|$projectId|$credentialTypesString|$page|$pageSize|$keyword")
        return client.get(UserCredentialResource::class).list(
            userId = userId,
            projectId = projectId,
            credentialTypesString = credentialTypesString,
            page = page ?: 1,
            pageSize = pageSize ?: 20,
            keyword = null
        )
    }

//    override fun list(
//        appCode: String?,
//        apigwType: String?,
//        projectId: String,
//        page: Int?,
//        pageSize: Int?
//    ): Result<Page<Credential>> {
//        logger.info("get all credential of project($projectId)")
//        return client.get(ServiceCredentialResource::class).list(
//            projectId = projectId,
//            page = page,
//            pageSize = pageSize
//        )
//    }

    @BkTimed(extraTags = ["operate", "create"])
    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        credential: CredentialCreate
    ): Result<Boolean> {
        logger.info("OPENAPI_CREDENTIAL_V3|$userId|create|$projectId|$credential")
        return client.get(UserCredentialResource::class).create(
            userId = userId,
            projectId = projectId,
            credential = credential
        )
    }

    @BkTimed(extraTags = ["operate", "get"])
    override fun get(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        credentialId: String
    ): Result<CredentialWithPermission> {
        logger.info("OPENAPI_CREDENTIAL_V3|$userId|get|$projectId|$credentialId")
        return client.get(UserCredentialResource::class).get(
            userId = userId,
            projectId = projectId,
            credentialId = credentialId
        )
    }

    override fun edit(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        credentialId: String,
        credential: CredentialUpdate
    ): Result<Boolean> {
        logger.info("OPENAPI_CREDENTIAL_V3|$userId|edit|$projectId|$credentialId|$credential")
        return client.get(UserCredentialResource::class).edit(
            userId = userId,
            projectId = projectId,
            credentialId = credentialId,
            credential = credential
        )
    }

    override fun delete(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        credentialId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_CREDENTIAL_V3|$userId|delete|$projectId|$credentialId")
        return client.get(UserCredentialResource::class).delete(
            userId = userId,
            projectId = projectId,
            credentialId = credentialId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCredentialResourceV3Impl::class.java)
    }
}
