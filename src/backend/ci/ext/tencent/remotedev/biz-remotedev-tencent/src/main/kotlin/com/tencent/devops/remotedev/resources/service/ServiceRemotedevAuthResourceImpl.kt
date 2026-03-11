/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.resources.service

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemotedevAuthResource
import com.tencent.devops.remotedev.service.AuthWorkspaceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class ServiceRemotedevAuthResourceImpl @Autowired constructor(
    private val authWorkspaceService: AuthWorkspaceService
) : ServiceRemotedevAuthResource {

    override fun workspaceInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        return authWorkspaceService.workspaceInfo(callBackInfo, token)
    }

    override fun workspaceGroupInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        return authWorkspaceService.workspaceGroupInfo(callBackInfo, token)
    }
}
