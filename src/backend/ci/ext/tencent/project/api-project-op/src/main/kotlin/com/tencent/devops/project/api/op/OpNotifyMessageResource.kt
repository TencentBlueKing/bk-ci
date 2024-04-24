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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.SendEmailForProjectByConditionDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OP_NOTIFY_MESSAGE", description = "OP-消息通知")
@Path("/op/notify/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNotifyMessageResource {
    @Operation(summary = "推动项目关联运营产品-根据项目")
    @POST
    @Path("/sendEmailForRelatedObsByProjectIds")
    fun sendEmailToUserForRelatedObsByProjectIds(
        @Parameter(description = "项目通知请求报文体", required = true)
        projectIds: List<String>
    ): Result<Boolean>

    @Operation(summary = "推动项目关联运营产品-根据条件")
    @POST
    @Path("/sendEmailForRelatedObsByCondition/")
    fun sendEmailForRelatedObsByCondition(
        @Parameter(description = "通过条件对项目进行邮件通知", required = true)
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Result<Boolean>

    @Operation(summary = "查询项目关联运营产品-根据条件")
    @POST
    @Path("/getProjectForRelatedObsByCondition/")
    fun getProjectsForRelatedObsByCondition(
        @Parameter(description = "通过条件对项目进行邮件通知", required = true)
        sendEmailForProjectByConditionDTO: SendEmailForProjectByConditionDTO
    ): Result<Pair<Int, List<String>>>

    @Operation(summary = "项目所属组织架构变更通知")
    @POST
    @Path("/sendEmailForProjectOrganizationChange/")
    fun sendEmailForProjectOrganizationChange(): Result<Boolean>

    @Operation(summary = "项目所属OBS运营产品变更通知")
    @POST
    @Path("/sendEmailForProjectProductChange/")
    fun sendEmailForProjectProductChange(): Result<Boolean>

    @Operation(summary = "检查项目组织架构是否正确")
    @POST
    @Path("/sendEmailForVerifyProjectOrganization/")
    fun sendEmailForVerifyProjectOrganization(): Result<Boolean>
}
