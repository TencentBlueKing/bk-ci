package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.pojo.agent.DockerInitFileInfo
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.ProjectScope
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpgradeServiceTest {

    private val mockAgentPropsScope: AgentPropsScope = mockk()
    private val mockAgentScope: AgentScope = mockk()
    private val mockProjectScope: ProjectScope = mockk()
    private val service = UpgradeService(
        dslContext = mockk(),
        thirdPartyAgentDao = mockk(),
        agentPropsScope = mockAgentPropsScope,
        agentScope = mockAgentScope,
        projectScope = mockProjectScope
    )

    private val projectId = "test-project"
    private val agentId = "test-agentid"
    private val os = "MACOS"
    private val arch = "amd64"

    @BeforeEach
    fun setUp() {
        every { mockAgentPropsScope.getWorkerVersion() } returns "curr-test-worker-version"
        every { mockAgentPropsScope.getAgentVersion() } returns "curr-test-go-version"
        every { mockAgentPropsScope.getJdkVersion(os, arch) } returns "curr-test-jdk-version"
        every { mockAgentPropsScope.getDockerInitFileMd5() } returns "curr-docker-init-file-md5"
        every { mockAgentScope.checkCanUpgrade(agentId) } returns false

        every { mockAgentScope.checkLockUpgrade(any(), any()) } returns false
        every { mockAgentScope.checkForceUpgrade(any(), any()) } returns false

        every { mockProjectScope.checkDenyUpgradeProject(any(), any()) } returns false
        every { mockProjectScope.checkInPriorityUpgradeProjectOrEmpty(any(), any()) } returns true
    }

    @Test
    fun `无额外条件，未升级`() {
        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(false, false, false, false)
        )
    }

    @Test
    fun `无额外条件，升级`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns true

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(true, true, true, true)
        )
    }

    @Test
    fun `LockAgent无法升级`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns true
        every { mockAgentScope.checkLockUpgrade(any(), any()) } returns true
        every { mockAgentScope.checkForceUpgrade(any(), any()) } returns true

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(false, false, false, false)
        )
    }

    @Test
    fun `Force强制升级`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns false
        every { mockAgentScope.checkLockUpgrade(any(), any()) } returns false
        every { mockAgentScope.checkForceUpgrade(any(), any()) } returns true

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(true, true, true, true)
        )
    }

    @Test
    fun `项目拒绝升级`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns true
        every { mockProjectScope.checkDenyUpgradeProject(any(), any()) } returns true
        every { mockProjectScope.checkInPriorityUpgradeProjectOrEmpty(any(), any()) } returns true

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(false, false, false, false)
        )
    }

    @Test
    fun `项目拒绝升级，非优先`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns true
        every { mockProjectScope.checkDenyUpgradeProject(any(), any()) } returns false
        every { mockProjectScope.checkInPriorityUpgradeProjectOrEmpty(any(), any()) } returns false

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(false, false, false, false)
        )
    }

    @Test
    fun `项目允许升级`() {
        every { mockAgentScope.checkCanUpgrade(agentId) } returns true
        every { mockProjectScope.checkDenyUpgradeProject(any(), any()) } returns false
        every { mockProjectScope.checkInPriorityUpgradeProjectOrEmpty(any(), any()) } returns true

        val data = service.checkUpgradeNew(
            projectId = projectId,
            agentId = agentId,
            props = AgentProps(
                arch = arch,
                jdkVersion = listOf("", "", "test-jdk-version"),
                userProps = null,
                dockerInitFileInfo = DockerInitFileInfo("", false),
                exitError = null,
                osVersion = null
            ),
            os = os,
            info = ThirdPartyAgentUpgradeByVersionInfo(
                "test-worker-version",
                "test-go-version",
                listOf("", "", "test-jdk-version"),
                DockerInitFileInfo("", true)
            )
        ).data

        Assertions.assertEquals(
            data, UpgradeItem(true, true, true, true)
        )
    }
}