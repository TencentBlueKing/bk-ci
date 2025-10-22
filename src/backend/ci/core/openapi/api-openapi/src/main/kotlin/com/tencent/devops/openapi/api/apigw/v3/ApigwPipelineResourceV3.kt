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
package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.openapi.BkApigwApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_PIPELINE_V3", description = "OPENAPI-流水线资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v3")
interface ApigwPipelineResourceV3 {

    @Deprecated("Replace with createPipeline")
    @Operation(summary = "新建流水线编排", tags = ["v3_app_pipeline_create", "v3_user_pipeline_create"])
    @POST
    @Path("")
    fun create(
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
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model
    ): Result<PipelineId>

    @Deprecated("Replace with editPipeline")
    @Operation(summary = "编辑流水线编排", tags = ["v3_app_pipeline_edit", "v3_user_pipeline_edit"])
    @PUT
    @Path("/{pipelineId}/")
    fun edit(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(
            description = "流水线模型", required = true, examples = [
                ExampleObject(
                    description = "如果我想更改流水线启动变量param的默认值为value2",
                    value = """
                            {
                                "name": "更改流水线启动变量默认值",
                                "stages": [{
                                    "containers": [{
                                        "@type": "trigger",
                                        "elements": [{
                                            "@type": "manualTrigger",
                                            "...": "..."
                                        }],
                                        "params": [{
                                            "id": "param",
                                            "required": true,
                                            "type": "STRING",
                                            "defaultValue": "value2",
                                            "desc": "",
                                            "readOnly": false
                                        }]
                                    }],
                                    "...": "..."
                                }, {
                                    "containers": [{}],
                                    "...": "..."
                                }],
                                "...": "..."
                            }
                                """
                ),
                ExampleObject(
                    description = "如果我想启用或是更改job互斥组配置",
                    value = """
                            {
                                "stages": [{
                                    "containers": [{
                                        "@type": "trigger",
                                        "...": "..."
                                    }],
                                    "...": "..."
                                }, {
                                    "containers": [{
                                        "@type" : "vmBuild",
                                        "name": "想要更改互斥组配置的job",
                                        "elements": [{
                                            "...": "..."
                                        }],
                                        "...": "...",
                                        "mutexGroup": {
                                            "enable": true,
                                            "mutexGroupName": "huchizu",
                                            "queueEnable": true,
                                            "timeout": 900,
                                            "queue": 5
                                        }
                                    }],
                                    "...": "..."
                                }],
                                "...": "..."
                            }
                        """
                ),
                ExampleObject(
                    description = "一般先通过接口(比如v3_app_pipeline_get)拿到编排，再根据自己的需求更改后上传更新",
                    value = """
                            {
                            "name": "一个非常简单的例子",
                            "desc": "",
                            "stages": [{
                                "containers": [{
                                    "@type": "trigger",
                                    "id": "0",
                                    "name": "构建触发",
                                    "elements": [{
                                        "@type": "manualTrigger",
                                        "name": "手动触发",
                                        "id": "T-1-1-1",
                                        "canElementSkip": true,
                                        "useLatestParameters": false,
                                        "executeCount": 1,
                                        "version": "1.*",
                                        "classType": "manualTrigger",
                                        "elementEnable": true,
                                        "atomCode": "manualTrigger",
                                        "taskAtom": ""
                                    }],
                                    "params": [],
                                    "containerId": "0",
                                    "containerHashId": "c-ccef587f17cd421a8a4e6aadc02777c6",
                                    "executeCount": 0,
                                    "matrixGroupFlag": false,
                                    "classType": "trigger"
                                }],
                                "id": "stage-1",
                                "name": "stage-1",
                                "tag": ["28ee946a59f64949a74f3dee40a1bda4"],
                                "fastKill": false,
                                "finally": false
                            }, {
                                "containers": [{
                                    "@type": "vmBuild",
                                    "id": "1",
                                    "name": "构建环境-LINUX",
                                    "elements": [{
                                        "@type": "linuxScript",
                                        "name": "Bash",
                                        "id": "e-efc1874f0cae44a0b56eba8b113b83f8",
                                        "scriptType": "SHELL",
                                        "script": "echo \"我只是为了测试\"",
                                        "continueNoneZero": false,
                                        "enableArchiveFile": false,
                                        "archiveFile": "",
                                        "additionalOptions": {
                                            "enable": true,
                                            "continueWhenFailed": false,
                                            "manualSkip": false,
                                            "retryWhenFailed": false,
                                            "retryCount": 1,
                                            "manualRetry": false,
                                            "timeout": 900,
                                            "runCondition": "PRE_TASK_SUCCESS",
                                            "pauseBeforeExec": false,
                                            "subscriptionPauseUser": "devops",
                                            "otherTask": "",
                                            "customVariables": [{
                                                "key": "param1",
                                                "value": ""
                                            }],
                                            "customCondition": "",
                                            "enableCustomEnv": false,
                                            "customEnv": [{
                                                "key": "param1",
                                                "value": ""
                                            }]
                                        },
                                        "executeCount": 1,
                                        "version": "1.*",
                                        "classType": "linuxScript",
                                        "elementEnable": true,
                                        "atomCode": "linuxScript",
                                        "taskAtom": ""
                                    }],
                                    "baseOS": "LINUX",
                                    "vmNames": [],
                                    "maxQueueMinutes": 60,
                                    "maxRunningMinutes": 900,
                                    "buildEnv": {},
                                    "dispatchType": {
                                        "buildType": "PUBLIC_DEVCLOUD",
                                        "value": "tlinux3_ci",
                                        "imageType": "BKSTORE",
                                        "credentialId": "",
                                        "credentialProject": "",
                                        "imageCode": "tlinux3_ci",
                                        "imageVersion": "2.*",
                                        "imageName": "tlinux3-CI镜像",
                                        "dockerBuildVersion": "tlinux3_ci",
                                        "imagePublicFlag": false,
                                        "imageRDType": "",
                                        "recommendFlag": true
                                    },
                                    "showBuildResource": false,
                                    "enableExternal": false,
                                    "containerId": "1",
                                    "containerHashId": "c-758f14c0c5e644e1b70f1bf37a1cb5a5",
                                    "executeCount": 0,
                                    "jobId": "job_biS",
                                    "matrixGroupFlag": false,
                                    "nfsSwitch": false,
                                    "classType": "vmBuild"
                                }],
                                "id": "stage-2",
                                "name": "stage-2",
                                "tag": ["28ee946a59f64949a74f3dee40a1bda4"],
                                "fastKill": false,
                                "finally": false,
                                "checkIn": {
                                    "manualTrigger": false,
                                    "timeout": 24
                                },
                                "checkOut": {
                                    "manualTrigger": false,
                                    "timeout": 24
                                }
                            }],
                            "labels": [],
                            "instanceFromTemplate": false,
                            "pipelineCreator": "devops",
                            "events": {},
                            "latestVersion": 6
                        }
                        """
                )
            ]
        )
        pipeline: Model
    ): Result<Boolean>

    @Operation(
        summary = "导入新流水线, 包含流水线编排和设置",
        tags = ["v3_user_pipeline_upload", "v3_app_pipeline_upload"]
    )
    @POST
    @Path("/pipeline_upload")
    fun uploadPipeline(
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
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting
    ): Result<PipelineId>

    @Operation(summary = "更新流水线编排和设置", tags = ["v3_user_pipeline_update", "v3_app_pipeline_update"])
    @PUT
    @Path("/{pipelineId}/pipeline_update")
    fun updatePipeline(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线模型与设置", required = true)
        @Valid
        modelAndSetting: PipelineModelAndSetting
    ): Result<DeployPipelineResult>

    @Operation(summary = "获取流水线编排", tags = ["v3_app_pipeline_get", "v3_user_pipeline_get"])
    @GET
    @Path("/{pipelineId}/")
    fun get(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Model>

    @Operation(summary = "批量获取流水线编排与配置", tags = ["v3_app_pipeline_batch_get", "v3_user_pipeline_batch_get"])
    @POST
    @Path("/batchGet")
    fun getBatch(
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
        @Parameter(description = "流水线ID列表", required = true)
        pipelineIds: List<String>
    ): Result<List<Pipeline>>

    @Operation(summary = "复制流水线编排", tags = ["v3_user_pipeline_copy", "v3_app_pipeline_copy"])
    @POST
    @Path("/{pipelineId}/copy")
    fun copy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线COPY", required = true)
        pipeline: PipelineCopy
    ): Result<PipelineId>

    @Operation(summary = "删除流水线编排", tags = ["v3_user_pipeline_delete", "v3_app_pipeline_delete"])
    @DELETE
    @Path("/{pipelineId}/")
    fun delete(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "获取项目的流水线列表", tags = ["v3_user_pipeline_list", "v3_app_pipeline_list"])
    @GET
    @Path("")
    fun getListByUser(
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
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<Pipeline>>

    @Operation(summary = "获取流水线状态", tags = ["v3_app_pipeline_status", "v3_user_pipeline_status"])
    @GET
    @Path("/{pipelineId}/status")
    fun status(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Pipeline?>

    @Operation(summary = "流水线重命名", tags = ["v3_user_pipeline_rename", "v3_app_pipeline_rename"])
    @POST
    @Path("/{pipelineId}/rename")
    fun rename(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线名称", required = true)
        name: PipelineName
    ): Result<Boolean>

    @Operation(summary = "还原流水线编排", tags = [])
    @PUT
    @Path("/{pipelineId}/restore")
    fun restore(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "更新流水线设置", tags = ["v3_app_pipeline_setting_update", "v3_user_pipeline_setting_update"])
    @PUT
    @Path("/{pipelineId}/setting_update")
    fun saveSetting(
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
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>
}
