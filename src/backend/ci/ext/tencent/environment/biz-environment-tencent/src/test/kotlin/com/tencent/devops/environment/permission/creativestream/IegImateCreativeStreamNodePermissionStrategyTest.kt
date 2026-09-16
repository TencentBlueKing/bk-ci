package com.tencent.devops.environment.permission.creativestream

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleQueryItem
import com.tencent.devops.environment.service.openworkspace.IegImateShareAccessService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IegImateCreativeStreamNodePermissionStrategyTest {

    private val nodeTypeService: CreativeStreamNodeTypeService = mockk(relaxed = true)
    private val shareAccessService: IegImateShareAccessService = mockk()
    private lateinit var strategy: IegImateCreativeStreamNodePermissionStrategy

    @BeforeEach
    fun setUp() {
        strategy = IegImateCreativeStreamNodePermissionStrategy(
            nodeTypeService = nodeTypeService,
            iegImateShareAccessService = shareAccessService
        )
        every { nodeTypeService.getNodeType(PROJECT_ID, NODE_ID) } returns CreativeStreamNodeType.IEG_IMATE
        every { nodeTypeService.getWorkspaceName(PROJECT_ID, NODE_ID) } returns WORKSPACE_NAME
    }

    @Test
    fun `admin role grants edit and delete`() {
        every {
            shareAccessService.getRole(USER_ID, WORKSPACE_NAME)
        } returns IegImateShareAccessService.ADMIN_ROLE

        assertTrue(check(AuthPermission.VIEW))
        assertTrue(check(AuthPermission.EDIT))
        assertTrue(check(AuthPermission.DELETE))
        assertFalse(check(AuthPermission.CREATE))
    }

    @Test
    fun `member role grants view use list only`() {
        every { shareAccessService.getRole(USER_ID, WORKSPACE_NAME) } returns "user"

        assertTrue(check(AuthPermission.VIEW))
        assertTrue(check(AuthPermission.USE))
        assertTrue(check(AuthPermission.LIST))
        assertFalse(check(AuthPermission.EDIT))
        assertFalse(check(AuthPermission.DELETE))
    }

    @Test
    fun `null role denies permission`() {
        every { shareAccessService.getRole(USER_ID, WORKSPACE_NAME) } returns null

        assertFalse(check(AuthPermission.USE))
    }

    @Test
    fun `missing workspace denies permission`() {
        every { nodeTypeService.getWorkspaceName(PROJECT_ID, NODE_ID) } returns null

        assertFalse(check(AuthPermission.USE))
        verify(exactly = 0) { shareAccessService.getRole(any(), any()) }
    }

    @Test
    fun `prefetch only queries ieg imate nodes`() {
        every { nodeTypeService.getNodeType(PROJECT_ID, 1L) } returns CreativeStreamNodeType.IEG_IMATE
        every { nodeTypeService.getNodeType(PROJECT_ID, 2L) } returns CreativeStreamNodeType.CLOUD_DESKTOP
        every { nodeTypeService.getNodeType(PROJECT_ID, 3L) } returns CreativeStreamNodeType.IEG_IMATE
        every { nodeTypeService.getWorkspaceName(PROJECT_ID, 1L) } returns "ws-1"
        every { nodeTypeService.getWorkspaceName(PROJECT_ID, 3L) } returns "ws-3"
        every { shareAccessService.prefetchRoles(any()) } returns Unit

        strategy.prefetch(userId = USER_ID, projectId = PROJECT_ID, nodeIds = listOf(1L, 2L, 3L))

        verify {
            shareAccessService.prefetchRoles(
                listOf(
                    WorkspaceShareRoleQueryItem(userId = USER_ID, workspaceName = "ws-1"),
                    WorkspaceShareRoleQueryItem(userId = USER_ID, workspaceName = "ws-3")
                )
            )
        }
    }

    private fun check(permission: AuthPermission): Boolean {
        return strategy.checkPermission(
            userId = USER_ID,
            projectId = PROJECT_ID,
            nodeId = NODE_ID,
            permission = permission
        )
    }

    companion object {
        private const val USER_ID = "alice"
        private const val PROJECT_ID = "project-1"
        private const val NODE_ID = 11L
        private const val WORKSPACE_NAME = "workspace-frontend-01"
    }
}
