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

package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_INFO
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.enums.VariableType
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildVarResourceImpl @Autowired constructor(
    private val buildVariableService: BuildVariableService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineContextService: PipelineContextService
) : BuildVarResource {

    override fun getBuildVar(buildId: String, projectId: String, pipelineId: String): Result<Map<String, String>> {
        checkParam(buildId = buildId, projectId = projectId, pipelineId = pipelineId)
        checkPermission(projectId = projectId, pipelineId = pipelineId)
        return Result(buildVariableService.getAllVariable(projectId, pipelineId, buildId))
    }

    override fun getContextVariableByName(
        buildId: String,
        projectId: String,
        pipelineId: String,
        containerId: String,
        taskId: String?,
        contextName: String,
        check: Boolean?
    ): Result<String?> {
        checkParam(buildId = buildId, projectId = projectId, pipelineId = pipelineId)
        checkPermission(projectId = projectId, pipelineId = pipelineId)
        val alisName = if (check == true) {
            checkVariable(variableName = contextName)
            if (taskId.isNullOrBlank() && VariableType.valueOf(contextName).alisName.isNotBlank()) {
                throw ParamBlankException("Plugin SDK needs to be upgraded")
            }
            if (VariableType.valueOf(contextName) == VariableType.BK_CI_BUILD_TASK_ID) {
                return Result(taskId)
            }
            VariableType.valueOf(contextName).alisName
        } else ""
        // 如果无法替换上下文预置变量则保持原变量名去查取
        val varName = pipelineContextService.getBuildVarName(contextName) ?: contextName
        val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        val allContext = pipelineContextService.buildContext(
            projectId = projectId,
            buildId = buildId,
            pipelineId = pipelineId,
            stageId = null,
            containerId = containerId,
            taskId = taskId,
            variables = variables
        )
        return Result(variables[varName] ?: allContext[varName] ?: allContext[alisName])
    }

    fun checkPermission(projectId: String, pipelineId: String) {
        val userId = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.lastModifyUser ?: ""
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE
            )
        ) {
            throw PermissionForbiddenException(
                MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_INFO))
                )
            )
        }
    }

    fun checkParam(buildId: String, projectId: String, pipelineId: String) {
        if (StringUtils.isBlank(buildId)) {
            throw ParamBlankException("build Id is null or blank")
        }
        if (StringUtils.isBlank(pipelineId)) {
            throw ParamBlankException("pipeline Id is null or blank")
        }
    }

    fun checkVariable(variableName: String) {
        if (!VariableType.validate(variableName)) {
            throw ParamBlankException("This variable is currently not supported")
        }
    }
}
