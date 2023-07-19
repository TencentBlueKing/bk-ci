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

package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_NEED_PARAM_
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.api.BuildAgentBuildResource
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.BuildJobType
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildWithStatus
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugDoneInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugInfo
import com.tencent.devops.dispatch.service.ThirdPartyAgentDockerService
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo

@RestResource
@Suppress("ALL")
class BuildAgentBuildResourceImpl constructor(
    private val thirdPartyAgentBuildService: ThirdPartyAgentService,
    private val thirdPartyAgentDockerService: ThirdPartyAgentDockerService
) : BuildAgentBuildResource {

    override fun startBuild(
        projectId: String,
        agentId: String,
        secretKey: String,
        buildType: String?
    ): AgentResult<ThirdPartyBuildInfo?> {
        checkParam(projectId, agentId, secretKey)
        return thirdPartyAgentBuildService.startBuild(projectId, agentId, secretKey, BuildJobType.toEnum(buildType))
    }

    override fun upgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        version: String?,
        masterVersion: String?
    ): AgentResult<Boolean> {
        checkParam(projectId, agentId, secretKey)
        return thirdPartyAgentBuildService.checkIfCanUpgradeByVersion(
            projectId,
            agentId,
            secretKey,
            version,
            masterVersion
        )
    }

    override fun upgradeNew(
        projectId: String,
        agentId: String,
        secretKey: String,
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem> {
        checkParam(projectId, agentId, secretKey)
        return thirdPartyAgentBuildService.checkIfCanUpgradeByVersionNew(
            projectId = projectId,
            agentId = agentId,
            secretKey = secretKey,
            info = info
        )
    }

    override fun finishUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        success: Boolean
    ): AgentResult<Boolean> {
        checkParam(projectId, agentId, secretKey)
        return thirdPartyAgentBuildService.finishUpgrade(projectId, agentId, secretKey, success)
    }

    override fun workerBuildFinish(
        projectId: String,
        agentId: String,
        secretKey: String,
        buildInfo: ThirdPartyBuildWithStatus
    ): Result<Boolean> {
        checkParam(projectId, agentId, secretKey)
        thirdPartyAgentBuildService.workerBuildFinish(projectId, agentId, secretKey, buildInfo)
        return Result(true)
    }

    override fun dockerStartDebug(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentResult<ThirdPartyDockerDebugInfo?> {
        checkParam(projectId, agentId, secretKey)
        return thirdPartyAgentDockerService.startDockerDebug(projectId, agentId, secretKey)
    }

    override fun dockerStartDebugDone(
        projectId: String,
        agentId: String,
        secretKey: String,
        debugInfo: ThirdPartyDockerDebugDoneInfo
    ): Result<Boolean> {
        checkParam(projectId, agentId, secretKey)
        thirdPartyAgentDockerService.startDockerDebugDone(projectId, agentId, secretKey, debugInfo)
        return Result(true)
    }

    override fun dockerDebugStatus(
        projectId: String,
        agentId: String,
        secretKey: String,
        debugId: Long
    ): Result<String?> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentDockerService.fetchDebugStatus(debugId))
    }

    private fun checkParam(projectId: String, agentId: String, secretKey: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    ERROR_INVALID_PARAM_,
                    I18nUtil.getDefaultLocaleLanguage(),
                    arrayOf("projectId")
                )
            )
        }
        if (agentId.isBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    ERROR_INVALID_PARAM_,
                    I18nUtil.getDefaultLocaleLanguage(),
                    arrayOf("agentId")
                )
            )
        }
        if (secretKey.isBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    ERROR_NEED_PARAM_,
                    I18nUtil.getDefaultLocaleLanguage(),
                    arrayOf("secretKey")
                )
            )
        }
    }
}
