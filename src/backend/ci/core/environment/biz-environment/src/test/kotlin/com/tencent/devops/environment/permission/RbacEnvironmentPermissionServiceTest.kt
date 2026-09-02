package com.tencent.devops.environment.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.environment.permission.creativestream.CreativeStreamNodePermissionHandler
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RbacEnvironmentPermissionServiceTest {
    private val client: Client = mockk()
    private val tokenService: ClientTokenService = mockk()
    private val creativeHandler: CreativeStreamNodePermissionHandler = mockk()
    private val service = RbacEnvironmentPermissionService(
        client = client,
        tokenCheckService = tokenService,
        creativeStreamNodePermissionHandler = creativeHandler
    )

    @AfterEach
    fun tearDown() {
        ChannelContext.clear()
    }

    @Test
    fun `creative stream node resource type routes list methods to permission handler`() {
        ChannelContext.setChannel(ChannelCode.BS.name)
        every {
            creativeHandler.listNodePermissions(
                userId = "user",
                projectId = "project",
                nodeIds = null,
                permissions = any()
            )
        } answers {
            val permissions = arg<Set<AuthPermission>>(3)
            permissions.associateWith { permission ->
                if (permission == AuthPermission.VIEW) setOf(1L) else setOf(2L)
            }
        }
        every {
            creativeHandler.listNodePermissions(
                userId = "user",
                projectId = "project",
                nodeIds = any(),
                permissions = any()
            )
        } answers {
            val permissions = arg<Set<AuthPermission>>(3)
            permissions.associateWith { permission ->
                if (permission == AuthPermission.VIEW) setOf(1L) else setOf(2L)
            }
        }
        val firstNode: TNodeRecord = mockk()
        val secondNode: TNodeRecord = mockk()
        every { firstNode.nodeId } returns 1L
        every { secondNode.nodeId } returns 2L

        assertEquals(
            setOf(1L),
            service.listNodeByPermission(
                userId = "user",
                projectId = "project",
                permission = AuthPermission.VIEW,
                resourceType = AuthResourceType.CREATIVE_STREAM_NODE
            )
        )
        assertEquals(
            listOf(HashUtil.encodeLongId(2L)),
            service.listNodeByPermissions(
                userId = "user",
                projectId = "project",
                permissions = setOf(AuthPermission.EDIT),
                resourceType = AuthResourceType.CREATIVE_STREAM_NODE
            )[AuthPermission.EDIT]
        )
        assertEquals(
            listOf(firstNode),
            service.listNodePermission(
                userId = "user",
                projectId = "project",
                nodeRecordList = listOf(firstNode, secondNode),
                authPermission = AuthPermission.VIEW,
                resourceType = AuthResourceType.CREATIVE_STREAM_NODE
            )
        )
    }

    @Test
    fun `creative stream node check delegates but project check keeps rbac`() {
        ChannelContext.setChannel(ChannelCode.BS.name)
        every {
            creativeHandler.checkPermission("user", "project", 1L, AuthPermission.VIEW)
        } returns true
        val authResource: ServicePermissionAuthResource = mockk()
        every { client.get(ServicePermissionAuthResource::class) } returns authResource
        every { tokenService.getSystemToken() } returns "token"
        every {
            authResource.validateUserResourcePermissionByRelation(
                userId = "user",
                token = "token",
                action = any(),
                projectCode = "project",
                resourceCode = "project",
                resourceType = AuthResourceType.PROJECT.value,
                relationResourceType = null
            )
        } returns Result(true)

        assertTrue(
            service.checkNodePermission(
                userId = "user",
                projectId = "project",
                nodeId = 1L,
                permission = AuthPermission.VIEW,
                resourceType = AuthResourceType.CREATIVE_STREAM_NODE
            )
        )
        assertTrue(
            service.checkNodePermission(
                userId = "user",
                projectId = "project",
                permission = AuthPermission.VIEW,
                resourceType = AuthResourceType.CREATIVE_STREAM_NODE
            )
        )
    }

    @Test
    fun `normal channel keeps original rbac list path`() {
        ChannelContext.setChannel(ChannelCode.BS.name)
        val authResource: ServicePermissionAuthResource = mockk()
        every { client.get(ServicePermissionAuthResource::class) } returns authResource
        every { tokenService.getSystemToken() } returns "token"
        every {
            authResource.getUserResourceByPermission(
                userId = "user",
                token = "token",
                action = any(),
                projectCode = "project",
                resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value
            )
        } returns Result(listOf(HashUtil.encodeLongId(1L)))

        assertEquals(
            setOf(1L),
            service.listNodeByPermission(
                userId = "user",
                projectId = "project",
                permission = AuthPermission.VIEW,
                resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE
            )
        )
        verify(exactly = 0) {
            creativeHandler.listNodePermissions(any(), any(), any(), any())
        }
    }
}
