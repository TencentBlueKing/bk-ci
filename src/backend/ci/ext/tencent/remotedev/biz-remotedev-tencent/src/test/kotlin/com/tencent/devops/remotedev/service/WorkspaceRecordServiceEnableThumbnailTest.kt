package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordTicketType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkspaceRecordServiceEnableThumbnailTest {

    private lateinit var service: WorkspaceRecordService

    @BeforeEach
    fun setUp() {
        service = spyk(
            WorkspaceRecordService(
                dslContext = mockk(),
                bkRepoConfig = mockk(),
                workspaceWindowsDao = mockk(),
                startAppLinkDao = mockk(),
                workspaceRecordUserApprovalDao = mockk(),
                workspaceDao = mockk(),
                workspaceJoinDao = mockk(),
                workspaceRecordTicketDao = mockk(),
                dispatchWorkspaceDao = mockk(),
                remotedevBkRepoClient = mockk(),
                bkItsmService = mockk(),
                windowsResourceConfigService = mockk(),
                permissionService = mockk(),
                configCacheService = mockk(),
                featureSwitchService = mockk(),
                redisOperation = mockk(),
                workspaceSharedDao = mockk(),
                workspaceOpHistoryDao = mockk(),
                workspaceCommon = mockk()
            )
        )
    }

    @Test
    fun `enable true should create ticket then update enable`() {
        every {
            service.saveWorkspaceRecordTicket(any(), any())
        } just runs
        every {
            service.updateWorkspaceRecordTicketEnable(
                any(), any(), any()
            )
        } returns true

        val result = service.enableThumbnail("ins-test", true)

        assertTrue(result)
        verify(exactly = 1) {
            service.saveWorkspaceRecordTicket(
                "ins-test",
                WorkspaceRecordTicketType.THUMBNAIL
            )
        }
        verify(exactly = 1) {
            service.updateWorkspaceRecordTicketEnable(
                workspaceName = "ins-test",
                type = WorkspaceRecordTicketType.THUMBNAIL,
                enable = true
            )
        }
    }

    @Test
    fun `enable false should skip create and only update`() {
        every {
            service.updateWorkspaceRecordTicketEnable(
                any(), any(), any()
            )
        } returns true

        val result = service.enableThumbnail("ins-test", false)

        assertTrue(result)
        verify(exactly = 0) {
            service.saveWorkspaceRecordTicket(any(), any())
        }
        verify(exactly = 1) {
            service.updateWorkspaceRecordTicketEnable(
                workspaceName = "ins-test",
                type = WorkspaceRecordTicketType.THUMBNAIL,
                enable = false
            )
        }
    }

    @Test
    fun `enable false should return false when no record exists`() {
        every {
            service.updateWorkspaceRecordTicketEnable(
                any(), any(), any()
            )
        } returns false

        val result = service.enableThumbnail("ins-nonexistent", false)

        assertFalse(result)
    }

    @Test
    fun `enable true should return true when update succeeds`() {
        every {
            service.saveWorkspaceRecordTicket(any(), any())
        } just runs
        every {
            service.updateWorkspaceRecordTicketEnable(
                any(), any(), any()
            )
        } returns true

        assertTrue(service.enableThumbnail("ins-existing", true))
    }
}
