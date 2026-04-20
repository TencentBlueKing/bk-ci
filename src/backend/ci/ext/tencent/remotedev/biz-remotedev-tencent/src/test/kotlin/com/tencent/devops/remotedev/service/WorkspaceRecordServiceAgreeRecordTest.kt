package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.RemoteDevBkRepoConfig
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceRecordTicketDao
import com.tencent.devops.remotedev.dao.WorkspaceRecordUserApprovalDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkspaceRecordServiceAgreeRecordTest {

    private val dslContext: DSLContext = mockk()
    private val workspaceDao: WorkspaceDao = mockk()
    private val permissionService: PermissionService = mockk()
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao = mockk()
    private val workspaceCommon: WorkspaceCommon = mockk()

    private lateinit var service: WorkspaceRecordService

    @BeforeEach
    fun setUp() {
        service = WorkspaceRecordService(
            dslContext = dslContext,
            bkRepoConfig = mockk(),
            workspaceWindowsDao = mockk(),
            startAppLinkDao = mockk(),
            workspaceRecordUserApprovalDao = mockk(),
            workspaceDao = workspaceDao,
            workspaceJoinDao = mockk(),
            workspaceRecordTicketDao = mockk(),
            dispatchWorkspaceDao = mockk(),
            remotedevBkRepoClient = mockk(),
            bkItsmService = mockk(),
            windowsResourceConfigService = mockk(),
            permissionService = permissionService,
            configCacheService = mockk(),
            featureSwitchService = mockk(),
            redisOperation = mockk(),
            workspaceSharedDao = mockk(),
            workspaceOpHistoryDao = workspaceOpHistoryDao,
            workspaceCommon = workspaceCommon
        )
    }

    private fun buildWorkspaceRecord(
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): WorkspaceRecord {
        val now = LocalDateTime.now()
        return WorkspaceRecord(
            workspaceId = 1L,
            projectId = "test-project",
            workspaceName = TEST_WORKSPACE,
            displayName = "Test",
            usageTime = 0,
            sleepingTime = 0,
            createUserId = TEST_USER,
            creatorBgName = "",
            creatorDeptName = "",
            creatorCenterName = "",
            creatorGroupName = "",
            status = WorkspaceStatus.RUNNING,
            createTime = now,
            updateTime = now,
            lastStatusUpdateTime = now,
            workspaceMountType = WorkspaceMountType.DEVCLOUD,
            workspaceSystemType = WorkspaceSystemType.WINDOWS_GPU,
            ownerType = ownerType,
            remark = null,
            labels = null,
            ip = null
        )
    }

    @Test
    fun `agreeRecord should succeed for personal workspace with permission`() {
        val workspace = buildWorkspaceRecord(WorkspaceOwnerType.PERSONAL)
        every {
            workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE
            )
        } returns workspace
        every {
            permissionService.checkUserPermission(TEST_USER, TEST_WORKSPACE)
        } returns true
        every {
            workspaceCommon.getOpHistory(OpHistoryCopyWriting.AGREE_RECORD)
        } returns "同意开启云录制"
        every {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE,
                operator = TEST_USER,
                action = WorkspaceAction.AGREE_RECORD,
                actionMessage = "同意开启云录制"
            )
        } returns Unit

        service.agreeRecord(TEST_USER, TEST_WORKSPACE)

        verify(exactly = 1) {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE,
                operator = TEST_USER,
                action = WorkspaceAction.AGREE_RECORD,
                actionMessage = "同意开启云录制"
            )
        }
    }

    @Test
    fun `agreeRecord should succeed for public workspace without permission check`() {
        val workspace = buildWorkspaceRecord(WorkspaceOwnerType.PROJECT_PUBLIC)
        every {
            workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE
            )
        } returns workspace
        every {
            workspaceCommon.getOpHistory(OpHistoryCopyWriting.AGREE_RECORD)
        } returns "同意开启云录制"
        every {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE,
                operator = "other-user",
                action = WorkspaceAction.AGREE_RECORD,
                actionMessage = "同意开启云录制"
            )
        } returns Unit

        service.agreeRecord("other-user", TEST_WORKSPACE)

        verify(exactly = 0) {
            permissionService.checkUserPermission(any(), any())
        }
        verify(exactly = 1) {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE,
                operator = "other-user",
                action = WorkspaceAction.AGREE_RECORD,
                actionMessage = "同意开启云录制"
            )
        }
    }

    @Test
    fun `agreeRecord should throw FORBIDDEN for personal workspace without permission`() {
        val workspace = buildWorkspaceRecord(WorkspaceOwnerType.PERSONAL)
        every {
            workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = TEST_WORKSPACE
            )
        } returns workspace
        every {
            permissionService.checkUserPermission("no-access-user", TEST_WORKSPACE)
        } returns false

        val ex = assertThrows(ErrorCodeException::class.java) {
            service.agreeRecord("no-access-user", TEST_WORKSPACE)
        }
        assertEquals(ErrorCodeEnum.FORBIDDEN.errorCode, ex.errorCode)
    }

    @Test
    fun `agreeRecord should throw WORKSPACE_NOT_FIND when workspace does not exist`() {
        every {
            workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = "non-existent"
            )
        } returns null

        val ex = assertThrows(ErrorCodeException::class.java) {
            service.agreeRecord(TEST_USER, "non-existent")
        }
        assertEquals(ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode, ex.errorCode)
    }

    companion object {
        private const val TEST_USER = "test-user"
        private const val TEST_WORKSPACE = "test-workspace"
    }
}
