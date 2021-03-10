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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.service.label.PipelineGroupService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class PipelineSettingFacadeService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineGroupService: PipelineGroupService,
    private val client: Client
) {

    fun saveSetting(userId: String, setting: PipelineSetting, checkPermission: Boolean = true): String {
        if (checkPermission) {
            checkEditPermission(
                userId = userId,
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                message = "用户($userId)无权限在工程(${setting.projectId})下编辑流水线(${setting.pipelineId})"
            )
        }

        with(setting) {
            if (pipelineRepositoryService.isPipelineExist(
                    projectId = projectId, excludePipelineId = pipelineId, pipelineName = pipelineName
                )
            ) {
                throw ErrorCodeException(
                    statusCode = Response.Status.CONFLICT.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS,
                    defaultMessage = "流水线名称已被使用"
                )
            }
        }

        pipelineRepositoryService.updatePipelineName(userId, setting.pipelineId, setting.pipelineName)

        val id = pipelineRepositoryService.saveSetting(userId, setting)

        if (checkPermission) {
            pipelinePermissionService.modifyResource(
                projectId = setting.projectId,
                pipelineId = setting.pipelineId,
                pipelineName = setting.pipelineName
            )
        }

        pipelineGroupService.updatePipelineLabel(
            userId = userId,
            pipelineId = setting.pipelineId,
            labelIds = setting.labels
        )
        return id
    }

    fun userGetSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode = ChannelCode.BS
    ): PipelineSetting {
        var settingInfo = pipelineRepositoryService.getSetting(pipelineId)
        val groups = pipelineGroupService.getGroups(userId, projectId, pipelineId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        if (settingInfo == null) {
            val model = client.get(ServicePipelineResource::class).get(userId, projectId, pipelineId, channelCode).data
            val name = model?.name ?: "unknown pipeline name"
            val desc = model?.desc ?: ""
            settingInfo = PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = name,
                desc = desc,
                runLockType = PipelineRunLockType.MULTIPLE,
                successSubscription = Subscription(),
                failSubscription = Subscription(),
                labels = labels
            )
        } else {
            settingInfo.labels = labels
        }
        return settingInfo
    }

    fun getSettingInfo(pipelineId: String): PipelineSetting? {
        return pipelineRepositoryService.getSetting(pipelineId)
    }

    private fun checkEditPermission(userId: String, projectId: String, pipelineId: String, message: String) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(message)
        }
    }

    fun updatePipelineModel(
        userId: String,
        updatePipelineModelRequest: UpdatePipelineModelRequest,
        checkPermission: Boolean = true
    ): Boolean {
        val pipelineModelVersionList = updatePipelineModelRequest.pipelineModelVersionList
        if (checkPermission) {
            pipelineModelVersionList.forEach {
                checkEditPermission(
                    userId = it.creator,
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    message = "Need edit permission"
                )
            }
        }

        pipelineRepositoryService.batchUpdatePipelineModel(
            userId = userId,
            pipelineModelVersionList = pipelineModelVersionList
        )
        return true
    }

    fun rebuildSetting(
        oldSetting: PipelineSetting,
        projectId: String,
        newPipelineId: String,
        pipelineName: String
    ): PipelineSetting {
        return PipelineSetting(
            projectId = projectId,
            pipelineId = newPipelineId,
            pipelineName = pipelineName,
            desc = oldSetting.desc,
            successSubscription = oldSetting.successSubscription,
            failSubscription = oldSetting.failSubscription,
            maxPipelineResNum = oldSetting.maxPipelineResNum,
            maxQueueSize = oldSetting.maxQueueSize,
            hasPermission = oldSetting.hasPermission,
            labels = oldSetting.labels,
            runLockType = oldSetting.runLockType,
            waitQueueTimeMinute = oldSetting.waitQueueTimeMinute
        )
    }
}
