package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.AgentInstallSessionDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgentInstallSessionServiceTest {
    private val sessionDao: AgentInstallSessionDao = mockk(relaxed = true)
    private val permissions: EnvironmentPermissionService = mockk()
    private val service = AgentInstallSessionService(
        dslContext = mockk(),
        sessionDao = sessionDao,
        thirdPartyAgentDao = mockk(),
        nodeDao = mockk(),
        nodeTagService = mockk(),
        dynamicEnvMatcher = mockk(),
        slaveGatewayService = mockk(),
        environmentPermissionService = permissions,
        agentUrlService = mockk(),
        redisOperation = mockk(),
        authProjectApi = mockk(),
        pipelineAuthServiceCode = mockk()
    )

    @BeforeEach
    fun setUp() {
        every { permissions.checkNodePermission("viewer", "project", AuthPermission.CREATE) } returns false
        mockkObject(I18nUtil)
        every {
            I18nUtil.getCodeLanMessage(EnvironmentMessageCode.ERROR_NODE_NO_VIEW_PERMISSSION)
        } returns "No view permission"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(I18nUtil)
    }

    @Test
    fun `node VIEW allows reading sessions without CREATE`() {
        assertReadAllowed(AuthPermission.VIEW)
    }

    @Test
    fun `node LIST allows reading sessions without CREATE`() {
        assertReadAllowed(AuthPermission.LIST)
    }

    @Test
    fun `CREATE still allows reading sessions before any nodes exist`() {
        every { permissions.checkNodePermission("viewer", "project", AuthPermission.CREATE) } returns true

        assertDoesNotThrow { service.list("viewer", "project", null, null) }

        verify(exactly = 0) { permissions.listNodeByPermissions(any(), any(), any(), any()) }
    }

    @Test
    fun `no node permissions denies all read endpoints before loading sessions`() {
        every {
            permissions.listNodeByPermissions("viewer", "project", setOf(AuthPermission.LIST, AuthPermission.VIEW))
        } returns mapOf(AuthPermission.LIST to emptyList(), AuthPermission.VIEW to emptyList())

        assertThrows(PermissionForbiddenException::class.java) { service.list("viewer", "project", null, null) }
        assertThrows(PermissionForbiddenException::class.java) { service.get("viewer", "project", "session") }
        assertThrows(PermissionForbiddenException::class.java) { service.listNodes("viewer", "project", "session") }

        verify { sessionDao wasNot Called }
    }

    private fun assertReadAllowed(permission: AuthPermission) {
        every {
            permissions.listNodeByPermissions("viewer", "project", setOf(AuthPermission.LIST, AuthPermission.VIEW))
        } returns mapOf(permission to listOf("node"))

        assertDoesNotThrow { service.list("viewer", "project", null, null) }

        verify(exactly = 0) { permissions.checkNodePermission("viewer", "project", AuthPermission.VIEW) }
        verify(exactly = 0) { permissions.checkNodePermission("viewer", "project", AuthPermission.LIST) }
        verify(exactly = 1) { sessionDao.listByProject(any(), "project", 0, 20) }
    }
}
