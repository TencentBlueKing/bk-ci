package com.tencent.devops.environment.permission.creativestream

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreativeStreamNodePermissionHandlerTest {
    @Test
    fun `list permissions routes mixed node types`() {
        val handler = CreativeStreamNodePermissionHandler(
            dslContext = mockk(),
            nodeDao = mockk(),
            strategies = listOf(
                strategy(100, { it == 1L }) { permission -> permission == AuthPermission.VIEW },
                strategy(200, { it == 2L }) { permission -> permission == AuthPermission.EDIT },
                strategy(Int.MAX_VALUE, { true }) { false }
            )
        )

        val result = handler.listNodePermissions(
            userId = "user",
            projectId = "project",
            nodeIds = setOf(1L, 2L, 3L),
            permissions = setOf(AuthPermission.VIEW, AuthPermission.EDIT)
        )

        assertEquals(setOf(1L), result[AuthPermission.VIEW])
        assertEquals(setOf(2L), result[AuthPermission.EDIT])
    }

    @Test
    fun `single permission routes by node type and fails closed without strategy`() {
        val handler = CreativeStreamNodePermissionHandler(
            dslContext = mockk(),
            nodeDao = mockk(),
            strategies = listOf(
                strategy(100, { it == 1L }) { permission -> permission == AuthPermission.VIEW }
            )
        )

        assertTrue(handler.checkPermission("user", "project", 1L, AuthPermission.VIEW))
        assertFalse(handler.checkPermission("user", "project", 1L, AuthPermission.EDIT))
        assertFalse(handler.checkPermission("user", "project", 2L, AuthPermission.VIEW))
    }

    @Test
    fun `list permissions loads project creative nodes when ids are absent`() {
        val nodeDao: NodeDao = mockk()
        val nodeRecord: TNodeRecord = mockk()
        every { nodeRecord.nodeId } returns 1L
        every { nodeDao.listNodes(any(), "project", NodeType.CREATE) } returns listOf(nodeRecord)
        val handler = CreativeStreamNodePermissionHandler(
            dslContext = mockk(),
            nodeDao = nodeDao,
            strategies = listOf(
                strategy(Int.MAX_VALUE, { true }) { permission -> permission == AuthPermission.VIEW }
            )
        )

        val result = handler.listNodePermissions(
            userId = "user",
            projectId = "project",
            permissions = setOf(AuthPermission.VIEW)
        )

        assertEquals(setOf(1L), result[AuthPermission.VIEW])
    }

    private fun strategy(
        order: Int,
        supports: (Long) -> Boolean,
        checker: (AuthPermission) -> Boolean
    ): CreativeStreamNodePermissionStrategy {
        return object : CreativeStreamNodePermissionStrategy {
            override val order = order

            override fun supports(projectId: String, nodeId: Long): Boolean {
                return supports(nodeId)
            }

            override fun checkPermission(
                userId: String,
                projectId: String,
                nodeId: Long,
                permission: AuthPermission
            ): Boolean {
                return checker(permission)
            }
        }
    }
}
