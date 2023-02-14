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

package com.tencent.devops.environment.resources.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.OpThirdPartyAgentUpgradeResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.JDKInfo
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.ProjectScope
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2018/5/9
 */
@RestResource
class OpThirdPartyAgentUpgradeResourceImpl @Autowired constructor(
    private val agentPropsScope: AgentPropsScope,
    private val agentScope: AgentScope,
    private val projectScope: ProjectScope
) : OpThirdPartyAgentUpgradeResource {

    override fun setJDKVersionList(osArchJDKVersionSet: Set<JDKInfo>): Result<Boolean> {
        return agentPropsScope.setJdkVersions(osArchJDKVersionSet)
    }

    override fun setMaxParallelUpgradeCount(maxParallelUpgradeCount: Int): Result<Boolean> {
        agentPropsScope.setMaxParallelUpgradeCount(maxParallelUpgradeCount)
        return Result(true)
    }

    override fun getMaxParallelUpgradeCount(): Result<Int> {
        return Result(agentPropsScope.getMaxParallelUpgradeCount())
    }

    override fun setAgentWorkerVersion(version: String): Result<Boolean> {
        agentPropsScope.setAgentWorkerVersion(version)
        return Result(true)
    }

    override fun setMasterVersion(version: String): Result<Boolean> {
        agentPropsScope.setMasterVersion(version)
        return Result(true)
    }

    override fun getAgentWorkerVersion(): Result<String> {
        return Result(agentPropsScope.getWorkerVersion())
    }

    override fun getAgentMasterVersion(): Result<String> {
        return Result(agentPropsScope.getAgentVersion())
    }

    override fun setForceUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        return agentScope.setUpgradeAgents(
            ids = agentIds,
            upgradeKey = AgentScope.UpgradeKey.FORCE_UPGRADE,
            type = AgentUpgradeType.find(agentUpgradeType)
        )
    }

    override fun unsetForceUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        agentScope.unsetUpgradeAgents(
            ids = agentIds,
            upgradeKey = AgentScope.UpgradeKey.FORCE_UPGRADE,
            type = AgentUpgradeType.find(agentUpgradeType)
        )
        return Result(true)
    }

    override fun getAllForceUpgradeAgents(agentUpgradeType: String?): Result<Set<Long>> {
        return Result(
            agentScope.getAllUpgradeAgents(
                upgradeKey = AgentScope.UpgradeKey.FORCE_UPGRADE,
                type = AgentUpgradeType.find(agentUpgradeType)
            )
        )
    }

    override fun cleanAllForceUpgradeAgents(agentUpgradeType: String?): Result<Boolean> {
        return Result(
            data = agentScope.cleanAllUpgradeAgents(
                upgradeKey = AgentScope.UpgradeKey.FORCE_UPGRADE,
                type = AgentUpgradeType.find(agentUpgradeType)
            )
        )
    }

    override fun setLockUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        return agentScope.setUpgradeAgents(
            ids = agentIds,
            upgradeKey = AgentScope.UpgradeKey.LOCK_UPGRADE,
            type = AgentUpgradeType.find(agentUpgradeType)
        )
    }

    override fun unsetLockUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        return agentScope.unsetUpgradeAgents(
            ids = agentIds,
            upgradeKey = AgentScope.UpgradeKey.LOCK_UPGRADE,
            type = AgentUpgradeType.find(agentUpgradeType)
        )
    }

    override fun getAllLockUpgradeAgents(agentUpgradeType: String?): Result<Set<Long>> {
        return Result(
            agentScope.getAllUpgradeAgents(
                upgradeKey = AgentScope.UpgradeKey.LOCK_UPGRADE,
                type = AgentUpgradeType.find(agentUpgradeType)
            )
        )
    }

    override fun cleanAllLockUpgradeAgents(agentUpgradeType: String?): Result<Boolean> {
        return Result(
            data = agentScope.cleanAllUpgradeAgents(
                upgradeKey = AgentScope.UpgradeKey.LOCK_UPGRADE,
                type = AgentUpgradeType.find(agentUpgradeType)
            )
        )
    }

    override fun setPriorityUpgradeAgentProjects(projectIds: List<String>): Result<Boolean> {
        return projectScope.setUpgradeProjects(
            upgradeKey = ProjectScope.UpgradeKey.PRIORITY_PROJECT,
            projectIds = projectIds.filter { it.isNotBlank() }.toSet()
        )
    }

    override fun unsetPriorityUpgradeAgentProjects(projectIds: List<String>): Result<Boolean> {
        return projectScope.unsetUpgradeProjects(
            upgradeKey = ProjectScope.UpgradeKey.PRIORITY_PROJECT,
            projectIds = projectIds.filter { it.isNotBlank() }.toSet()
        )
    }

    override fun getAllPriorityUpgradeAgentProjects(): Result<Set<String>> {
        return Result(projectScope.getAllUpgradeProjects(ProjectScope.UpgradeKey.PRIORITY_PROJECT))
    }

    override fun cleanAllPriorityUpgradeAgentProjects(): Result<Boolean> {
        return Result(projectScope.cleanAllUpgradeProjects(ProjectScope.UpgradeKey.PRIORITY_PROJECT))
    }

    override fun setDenyUpgradeAgentProjects(projectIds: List<String>): Result<Boolean> {
        return projectScope.setUpgradeProjects(
            upgradeKey = ProjectScope.UpgradeKey.DENY_PROJECT,
            projectIds = projectIds.filter { it.isNotBlank() }.toSet()
        )
    }

    override fun unsetDenyUpgradeAgentProjects(projectIds: List<String>): Result<Boolean> {
        return projectScope.unsetUpgradeProjects(
            upgradeKey = ProjectScope.UpgradeKey.DENY_PROJECT,
            projectIds = projectIds.filter { it.isNotBlank() }.toSet()
        )
    }

    override fun getAllDenyUpgradeAgentProjects(): Result<Set<String>> {
        return Result(projectScope.getAllUpgradeProjects(ProjectScope.UpgradeKey.DENY_PROJECT))
    }

    override fun cleanAllDenyUpgradeAgentProjects(): Result<Boolean> {
        return Result(projectScope.cleanAllUpgradeProjects(ProjectScope.UpgradeKey.DENY_PROJECT))
    }
}
