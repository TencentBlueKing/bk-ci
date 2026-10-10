package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.TpaLock
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportServiceTest {
    private val agentDao: ThirdPartyAgentDao = mockk()
    private val nodeDao: NodeDao = mockk()
    private val service = ImportService(
        mockk(), mockk(), agentDao, nodeDao, mockk(), mockk(), mockk(), mockk(), mockk()
    )
    private val agentId = HashUtil.encodeLongId(1L)

    @BeforeEach
    fun setUp() {
        mockkConstructor(TpaLock::class)
        every { anyConstructed<TpaLock>().tryLock() } returns true
        every { anyConstructed<TpaLock>().unlock() } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(TpaLock::class)
    }

    @Test
    fun `busy lock returns incomplete without trying to import`() {
        every { anyConstructed<TpaLock>().tryLock() } returns false

        assertFalse(service.importAgent("creator", "project", agentId, "master"))
        verify { agentDao wasNot Called }
    }

    @Test
    fun `already imported agent reports completion`() {
        every { agentDao.getAgentByProject(any(), 1L, "project") } returns
            TEnvironmentThirdpartyAgentRecord().apply {
                status = AgentStatus.IMPORT_OK.status
                nodeId = 2L
                projectId = "project"
            }
        every { nodeDao.get(any(), "project", 2L) } returns
            TNodeRecord().apply { nodeStatus = NodeStatus.NORMAL.name }

        assertTrue(service.importAgent("creator", "project", agentId, "master"))
    }

    @Test
    fun `database error propagates for startup retry`() {
        every { agentDao.getAgentByProject(any(), 1L, "project") } throws IllegalStateException("db down")

        assertThrows(IllegalStateException::class.java) {
            service.importAgent("creator", "project", agentId, "master")
        }
    }
}
