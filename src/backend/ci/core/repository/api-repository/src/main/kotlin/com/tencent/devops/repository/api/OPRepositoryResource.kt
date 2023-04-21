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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("/op/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPRepositoryResource {
    @ApiOperation("用于对数据库表填充哈希值")
    @POST
    @Path("/addhashid")
    fun addHashId()

    @ApiOperation("修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateGitDomain(
        @ApiParam(value = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @ApiParam(value = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @ApiParam(value = "灰度项目列表,多个用,分割", required = true)
        @QueryParam("grayProject")
        grayProject: String?,
        @ApiParam(value = "灰度权重", required = true)
        @QueryParam("grayWeight")
        grayWeight: Int?,
        @ApiParam(value = "灰度白名单,多个用,分割", required = true)
        @QueryParam("grayWhiteProject")
        grayWhiteProject: String?
    ): Result<Boolean>

    @ApiOperation("更新git项目ID")
    @POST
    @Path("/updateGitProjectId")
    fun updateGitProjectId()
}
