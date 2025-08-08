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

package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.template.ApproveReq
import com.tencent.devops.store.pojo.template.OpTemplateResp
import com.tencent.devops.store.pojo.template.enums.OpTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.pojo.template.enums.TemplateTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE_TEMPLATE", description = "OP-流水线-模版")
@Path("/op/pipeline/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTemplateResource {

    @Operation(summary = "获取市场模版")
    @GET
    @Path("/list")
    fun listTemplates(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @Parameter(description = "模版状态", required = false)
        @QueryParam("templateStatus")
        templateStatus: TemplateStatusEnum?,
        @Parameter(description = "模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateTypeEnum?,
        @Parameter(description = "模版分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴", required = false)
        @QueryParam("category")
        category: String?,
        @Parameter(description = "功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "是否最新", required = false)
        @QueryParam("latestFlag")
        latestFlag: Boolean?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: OpTemplateSortTypeEnum ? = OpTemplateSortTypeEnum.UPDATE_TIME,
        @Parameter(description = "排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<OpTemplateResp>

    @Operation(summary = "审核模版")
    @Path("/{templateId}/approve")
    @PUT
    fun approveTemplate(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "审核模版请求报文")
        approveReq: ApproveReq
    ): Result<Boolean>
}
