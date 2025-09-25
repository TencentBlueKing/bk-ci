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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("/op/repo/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPRepositoryConfigResource {

    @Operation(summary = "批量添加目标代码源的组织架构")
    @POST
    @Path("/{scmCode}/dept")
    fun addDept(
        @Parameter(description = "userId", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "需要添加的代码源管理的组织架构", required = true)
        deptList: List<RepositoryConfigVisibility>? = null
    ): Result<Boolean>

    @Operation(summary = "批量删除目标代码源的组织架构")
    @DELETE
    @Path("/{scmCode}/dept")
    fun deleteDept(
        @Parameter(description = "scmCode", required = true)
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "需要删除的代码源管理的组织架构", required = true)
        deptList: List<Int>? = null
    ): Result<Boolean>
}
