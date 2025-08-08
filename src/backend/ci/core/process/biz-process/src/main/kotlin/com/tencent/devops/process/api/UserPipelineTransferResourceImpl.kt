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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PositionBody
import com.tencent.devops.common.pipeline.pojo.transfer.PositionResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferResponse
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineTransferResource
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.TransferResponseResult
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.strategy.context.UserPipelinePermissionCheckContext
import com.tencent.devops.process.strategy.factory.UserPipelinePermissionCheckStrategyFactory
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineTransferResourceImpl @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val transferService: PipelineTransferYamlService
) : UserPipelineTransferResource {

    override fun transfer(
        userId: String,
        projectId: String,
        pipelineId: String?,
        actionType: TransferActionType,
        archiveFlag: Boolean?,
        data: TransferBody
    ): Result<TransferResponseResult> {
        if (pipelineId != null) {
            val userPipelinePermissionCheckStrategy =
                UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
            UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        }
        val editPermission = pipelineId?.let {
            pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = it,
                permission = AuthPermission.EDIT
            )
        }
        val response = try {
            transferService.transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                actionType = actionType,
                data = data,
                editPermission = editPermission,
                archiveFlag = archiveFlag
            )
        } catch (e: PipelineTransferException) {
            val elementMsg = I18nUtil.getCodeLanMessage(
                messageCode = e.errorCode,
                params = e.params,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                defaultMessage = e.defaultMessage
            )
            TransferResponse(yamlSupported = false, yamlInvalidMsg = elementMsg)
        }
        return Result(TransferResponseResult(response))
    }

    override fun modelTaskTransfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        data: Element
    ): Result<String> {
        val permission = AuthPermission.VIEW
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(transferService.modelTaskTransfer(userId, projectId, pipelineId, data))
    }

    override fun yamlTaskTransfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String
    ): Result<Element> {
        val permission = AuthPermission.VIEW
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        return Result(transferService.yamlTaskTransfer(userId, projectId, pipelineId, yaml))
    }

    override fun position(
        userId: String,
        projectId: String,
        line: Int,
        column: Int,
        yaml: PositionBody
    ): Result<PositionResponse> {
        return Result(
            transferService.position(
                userId = userId,
                projectId = projectId,
                line = line - 1,
                column = column - 1,
                yaml = yaml.yaml
            )
        )
    }

    override fun modelTaskInsert(
        userId: String,
        projectId: String,
        pipelineId: String,
        line: Int,
        column: Int,
        data: ElementInsertBody
    ): Result<ElementInsertResponse> {
        return Result(
            transferService.modelTaskInsert(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                line = line - 1,
                column = column - 1,
                data = data
            )
        )
    }
}
