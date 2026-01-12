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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstancePage
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_PROJECT_TEMPLATE_V4", description = "OPENAPI-项目模板资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/templates/templateInstances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwTemplateInstanceResourceV4 {
    @Operation(
        summary = "批量实例化流水线模板",
        tags = ["v4_app_templateInstance_create", "v4_user_templateInstance_create"]
    )
    @POST
    @Path("/")
    fun createTemplateInstances(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @Parameter(description = "模板版本（可通过v3_app_template_list接口获取）", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(
            description = "创建实例", required = true, examples = [
                ExampleObject(
                    description = "如果我想简单的实例化两条无启动变量的流水线1和2",
                    value = """
                        [
                            {
                                "pipelineName": "1",
                                "param": []
                            },
                            {
                                "pipelineName": "2",
                                "param": []
                            }
                        ]"""
                ),
                ExampleObject(
                    description = "如果我想实例化一条带启动变量param1的流水线3",
                    value = """
                        [
                            {
                                "pipelineName": "3",
                                "param": [
                                    {
                                        "id": "param1",
                                        "required": true,
                                        "type": "STRING //可以是其他类型，以实际情况为准",
                                        "defaultValue": "param1的值",
                                        "desc": "",
                                        "readOnly": false
                                    }
                                ]
                            }
                        ]"""
                )
            ]
        )
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet

    @Operation(
        summary = "批量更新流水线模板实例",
        tags = ["v4_user_templateInstance_update", "v4_app_templateInstance_update"]
    )
    @PUT
    @Path("/")
    fun updateTemplateInstances(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @Parameter(description = "版本名（可通过v3_app_template_list接口获取）", required = true)
        @QueryParam("version")
        version: Long,
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(description = "模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @Operation(
        summary = "批量更新流水线模板实例",
        tags = ["v4_user_templateInstance_update_versionName", "v4_app_templateInstance_update_versionName"]
    )
    @PUT
    @Path("/update")
    fun updateTemplateInstances(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @Parameter(description = "版本名", required = true)
        @QueryParam("versionName")
        versionName: String,
        @Parameter(description = "是否应用模板设置")
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean,
        @Parameter(description = "模板实例", required = true)
        instances: List<TemplateInstanceUpdate>
    ): TemplateOperationRet

    @Operation(
        summary = "获取流水线模板的实例列表",
        tags = ["v4_app_templateInstance_get", "v4_user_templateInstance_get"]
    )
    @GET
    @Path("/")
    fun listTemplateInstances(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int? = 1,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = false)
        pageSize: Int? = 20,
        @Parameter(description = "名字搜索的关键字", required = false)
        @QueryParam("searchKey")
        searchKey: String?,
        @Parameter(description = "排序字段", required = false)
        @QueryParam("sortType")
        sortType: TemplateSortTypeEnum?,
        @Parameter(description = "是否降序", required = false)
        @QueryParam("desc")
        desc: Boolean?
    ): Result<TemplateInstancePage>
}
