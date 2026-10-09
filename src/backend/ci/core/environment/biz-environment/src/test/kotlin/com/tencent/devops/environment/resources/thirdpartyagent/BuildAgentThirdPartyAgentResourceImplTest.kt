package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.thirdpartyagent.AgentInstallSessionDao
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.service.thirdpartyagent.AgentInstallSessionRuntimeService
import com.tencent.devops.environment.service.thirdpartyagent.ImportService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartyAgentMgrService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuildAgentThirdPartyAgentResourceImplTest {
    private val manager: ThirdPartyAgentMgrService = mockk()
    private val importer: ImportService = mockk()
    private val runtime: AgentInstallSessionRuntimeService = mockk()
    private val resource = BuildAgentThirdPartyAgentResourceImpl(
        manager, mockk(), importer, runtime, mockk(), mockk()
    )
    private val agentId = HashUtil.encodeLongId(1L)
    private val startInfo = ThirdPartyAgentStartInfo("host", "127.0.0.1", "linux", "worker", "master")

    @BeforeEach
    fun setUp() {
        every { manager.agentStartup("project", agentId, "secret", startInfo) } returns AgentStatus.UN_IMPORT_OK
        val agent = mockk<ThirdPartyAgent>()
        every { agent.createUser } returns "creator"
        every { agent.masterVersion } returns "master"
        every { manager.getAgent("project", agentId) } returns AgentResult(AgentStatus.UN_IMPORT_OK, agent)
        every { importer.importAgent("creator", "project", agentId, "master") } returns true
        every { runtime.processAgentStartup("project", agentId, startInfo) } returns Unit
    }

    @Test
    fun `busy import fails the handshake and next startup retries import`() {
        every { importer.importAgent(any(), any(), any(), any()) } returns false

        assertNotEquals(0, startup().status)
        verify { runtime wasNot Called }

        every { importer.importAgent(any(), any(), any(), any()) } returns true
        assertEquals(0, startup().status)
        verify(exactly = 2) { importer.importAgent("creator", "project", agentId, "master") }
        verify(exactly = 1) { runtime.processAgentStartup("project", agentId, startInfo) }
    }

    @Test
    fun `imported agent continues startup when optional session lookup fails`() {
        every { manager.agentStartup(any(), any(), any(), any()) } returns AgentStatus.IMPORT_OK
        val sessionDao = mockk<AgentInstallSessionDao>()
        every { sessionDao.findUnfinishedByAgentId(any(), any(), any()) } throws IllegalStateException("db down")
        val sessionRuntime = AgentInstallSessionRuntimeService(
            mockk(), sessionDao, mockk(), mockk(), mockk(), mockk(), mockk(), mockk()
        )
        val actualResource = BuildAgentThirdPartyAgentResourceImpl(
            manager, mockk(), importer, sessionRuntime, mockk(), mockk()
        )

        val result = actualResource.agentStartup("project", agentId, "secret", startInfo)
        assertEquals(0, result.status)
        assertEquals(AgentStatus.IMPORT_OK, result.data)
        verify { importer wasNot Called }
    }

    @Test
    fun `temporary import failure requests retry before applying session`() {
        every { importer.importAgent(any(), any(), any(), any()) } throws IllegalStateException("db down")

        assertThrows(IllegalStateException::class.java) { startup() }
        verify { runtime wasNot Called }
    }

    @Test
    fun `import permission failures preserve the existing API error behavior`() {
        every { importer.importAgent(any(), any(), any(), any()) } throws PermissionForbiddenException("no permission")

        assertThrows(PermissionForbiddenException::class.java) { startup() }
        verify { runtime wasNot Called }
    }

    @Test
    fun `deleted agent does not import or apply a session`() {
        every { manager.agentStartup(any(), any(), any(), any()) } returns AgentStatus.DELETE

        val result = startup()
        assertEquals(0, result.status)
        assertEquals(AgentStatus.DELETE, result.data)
        verify { importer wasNot Called }
        verify { runtime wasNot Called }
    }

    @Test
    fun `missing import target does not acknowledge startup`() {
        every { manager.getAgent("project", agentId) } returns AgentResult(AgentStatus.DELETE, null)

        assertThrows(NotFoundException::class.java) { startup() }
        verify { importer wasNot Called }
        verify { runtime wasNot Called }
    }

    private fun startup() = resource.agentStartup("project", agentId, "secret", startInfo)
}
