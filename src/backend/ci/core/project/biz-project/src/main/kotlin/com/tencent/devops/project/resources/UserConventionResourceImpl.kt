/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.user.UserConventionResource
import com.tencent.devops.project.pojo.convention.ConventionAgreeResponse
import com.tencent.devops.project.pojo.convention.ConventionAgreementItem
import com.tencent.devops.project.pojo.convention.ConventionBody
import com.tencent.devops.project.pojo.convention.ConventionCurrentResponse
import com.tencent.devops.project.service.ConventionService
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import java.nio.charset.StandardCharsets

@RestResource
class UserConventionResourceImpl @Autowired constructor(
    private val conventionService: ConventionService
) : UserConventionResource {
    override fun current(userId: String, lang: String?): Result<ConventionCurrentResponse> {
        return Result(conventionService.current(userId, lang))
    }

    override fun agree(userId: String): Result<ConventionAgreeResponse> {
        return Result(conventionService.agree(userId))
    }

    override fun getBody(userId: String, lang: String?): Result<ConventionBody> {
        return Result(conventionService.getBody(userId, lang))
    }

    override fun saveBody(userId: String, request: ConventionBody): Result<ConventionBody> {
        return Result(conventionService.saveBody(userId, request))
    }

    override fun listAgreements(
        userId: String,
        filterUser: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ConventionAgreementItem>> {
        return Result(
            conventionService.listAgreements(
                userId = userId,
                filterUser = filterUser,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun exportAgreements(userId: String, filterUser: String?): Response {
        val csv = conventionService.exportAgreements(userId, filterUser)
        val filename = "%E7%AD%BE%E7%BD%B2%E8%AE%B0%E5%BD%95.csv"
        return Response.ok(csv.toByteArray(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM)
            .header(
                "Content-Disposition",
                "attachment; filename=convention-records.csv; filename*=UTF-8''$filename"
            )
            .build()
    }
}
