/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.api.service

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_IAM_TOKEN
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_CALLBACK_REMOTEDEV", description = "iam回调remotedev接口")
@Path("/open/workspace/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemotedevAuthResource {

    @POST
    @Path("/")
    @Operation(summary = "iam工作空间回调接口")
    fun workspaceInfo(
        @Parameter(description = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @Parameter(description = "token")
        token: String
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/workspaceGroupInfo")
    @Operation(summary = "iam工作空间分组回调接口")
    fun workspaceGroupInfo(
        @Parameter(description = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @Parameter(description = "token")
        token: String
    ): CallbackBaseResponseDTO?
}
