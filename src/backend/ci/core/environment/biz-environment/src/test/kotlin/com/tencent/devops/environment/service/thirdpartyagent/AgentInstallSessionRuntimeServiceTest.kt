package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.thirdpartyagent.AgentInstallSessionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentInstallSession
import com.tencent.devops.environment.model.AgentInstallSessionConfig
import com.tencent.devops.environment.model.AgentInstallSessionMode
import com.tencent.devops.environment.model.AgentInstallSessionNode
import com.tencent.devops.environment.model.AgentInstallSessionNodeStatus
import com.tencent.devops.environment.model.AgentInstallSessionStatus
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.service.NodeTagService
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AgentInstallSessionRuntimeServiceTest {
    private val dslContext: DSLContext = mockk()
    private val sessionDao: AgentInstallSessionDao = mockk(relaxed = true)
    private val agentDao: ThirdPartyAgentDao = mockk(relaxed = true)
    private val nodeTagService: NodeTagService = mockk(relaxed = true)
    private val service = AgentInstallSessionRuntimeService(
        dslContext, sessionDao, agentDao, mockk(), mockk(), nodeTagService, mockk(), mockk()
    )
    private val agentId = 1L
    private val agentHashId = HashUtil.encodeLongId(agentId)
    private val startInfo = ThirdPartyAgentStartInfo("host", "127.0.0.1", "linux", "worker", "master")
    private val now = LocalDateTime.now()
    private val session = AgentInstallSession(
        id = "session", projectId = "project", mode = AgentInstallSessionMode.FIRST_IMPORT,
        createdBy = "creator", os = "LINUX",
        config = AgentInstallSessionConfig(
            gateway = "gateway", installType = "SERVICE", agentType = "BUILD", parallelTaskCount = 3
        ),
        targetAgentId = null, targetNodeId = null, configFingerprint = "fingerprint", tokenHash = "hash",
        tokenCipher = "cipher", status = AgentInstallSessionStatus.ACTIVE, expiredTime = now.plusDays(1),
        previousSessionId = null, createdTime = now, updatedTime = now
    )
    private var nodeStatus = AgentInstallSessionNodeStatus.INSTALLING
    private var agentNodeId: Long? = 2L

    @BeforeEach
    fun setUp() {
        every { sessionDao.findUnfinishedByAgentId(dslContext, agentId, any()) } answers {
            if (nodeStatus == AgentInstallSessionNodeStatus.SUCCEEDED) null else AgentInstallSessionNode(
                sessionId = session.id, agentId = agentId, status = nodeStatus, createdTime = now, updatedTime = now
            )
        }
        every { sessionDao.getById(dslContext, "project", session.id) } returns session
        every { sessionDao.updateNodeStatus(any(), session.id, agentId, any(), any()) } answers {
            nodeStatus = arg(3)
            true
        }
        every { agentDao.getAgentByProject(dslContext, agentId, "project") } answers {
            TEnvironmentThirdpartyAgentRecord().apply {
                id = agentId
                nodeId = agentNodeId
            }
        }
        val configuration = DSL.using(SQLDialect.MYSQL).configuration()
        every { dslContext.transaction(any<TransactionalRunnable>()) } answers {
            firstArg<TransactionalRunnable>().run(configuration)
        }
    }

    @Test
    fun `startup applies session configuration to imported node`() {
        startup()
        assertEquals(AgentInstallSessionNodeStatus.SUCCEEDED, nodeStatus)
        verify(exactly = 1) { nodeTagService.replaceUserTags("project", 2L, any(), any()) }
    }

    @Test
    fun `non session agent is ignored`() {
        nodeStatus = AgentInstallSessionNodeStatus.SUCCEEDED
        startup()
        verify(exactly = 0) { agentDao.getAgentByProject(any(), any(), any()) }
    }

    @Test
    fun `missing node records failure without failing startup`() {
        agentNodeId = null
        assertDoesNotThrow { startup() }
        assertEquals(AgentInstallSessionNodeStatus.FAILED, nodeStatus)
        verify(exactly = 0) { nodeTagService.replaceUserTags(any(), any(), any(), any()) }
    }

    @Test
    fun `configuration failure is tolerated and next natural startup can recover`() {
        every { nodeTagService.replaceUserTags(any(), any(), any(), any()) } throws
            IllegalStateException("database unavailable")

        assertDoesNotThrow { startup() }
        assertEquals(AgentInstallSessionNodeStatus.FAILED, nodeStatus)

        every { nodeTagService.replaceUserTags(any(), any(), any(), any()) } returns Unit
        startup()
        assertEquals(AgentInstallSessionNodeStatus.SUCCEEDED, nodeStatus)
        verify(exactly = 2) { nodeTagService.replaceUserTags("project", 2L, any(), any()) }
    }

    @Test
    fun `session lookup failure does not affect startup`() {
        every { sessionDao.findUnfinishedByAgentId(any(), any(), any()) } throws IllegalStateException("db down")
        assertDoesNotThrow { startup() }
    }

    @Test
    fun `missing session does not affect startup`() {
        every { sessionDao.getById(dslContext, "project", session.id) } returns null
        assertDoesNotThrow { startup() }
        verify(exactly = 0) { nodeTagService.replaceUserTags(any(), any(), any(), any()) }
    }

    @Test
    fun `invalid session tags only mark the session failed`() {
        every { nodeTagService.replaceUserTags(any(), any(), any(), any()) } throws
            ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("tags"))

        assertDoesNotThrow { startup() }
        assertEquals(AgentInstallSessionNodeStatus.FAILED, nodeStatus)
    }

    private fun startup() = service.processAgentStartup("project", agentHashId, startInfo)
}
