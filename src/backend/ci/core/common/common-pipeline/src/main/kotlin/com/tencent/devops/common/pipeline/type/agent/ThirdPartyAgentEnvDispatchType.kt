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

package com.tencent.devops.common.pipeline.type.agent

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType

data class ThirdPartyAgentEnvDispatchType(
    @JsonProperty("value")
    var envName: String,
    override var workspace: String?,
    // 共享环境时必填，值为提供共享环境的项目id
    var envProjectId: String?,
    override val agentType: AgentType = AgentType.NAME,
    override val dockerInfo: ThirdPartyAgentDockerInfo?,
    override var reusedInfo: ReusedInfo?
) : ThirdPartyAgentDispatch(
    value = envName,
    workspace = workspace,
    agentType = agentType,
    dockerInfo = dockerInfo,
    reusedInfo = reusedInfo
) {
    override fun cleanDataBeforeSave() {
        this.envName = this.envName.trim()
        this.envProjectId = this.envProjectId?.trim()
        this.workspace = this.workspace?.trim()
    }

    override fun replaceField(variables: Map<String, String>) {
        envName = EnvUtils.parseEnv(envName, variables)
        envProjectId = EnvUtils.parseEnv(envProjectId, variables)
        if (!workspace.isNullOrBlank()) {
            workspace = EnvUtils.parseEnv(workspace!!, variables)
        }
        dockerInfo?.replaceField(variables)
    }

    override fun buildType() = BuildType.valueOf(BuildType.THIRD_PARTY_AGENT_ENV.name)
}
