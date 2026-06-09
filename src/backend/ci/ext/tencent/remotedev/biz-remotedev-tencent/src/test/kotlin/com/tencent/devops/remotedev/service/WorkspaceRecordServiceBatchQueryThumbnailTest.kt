package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 单测 [WorkspaceRecordService.batchQueryThumbnailWorkspaces]
 *
 * 覆盖：
 * - enable=true / enable=false 透传
 * - pageSize 超限截断为 MAX_PAGE_SIZE
 * - 空结果场景
 * - page<1 归一化为 1
 */
class WorkspaceRecordServiceBatchQueryThumbnailTest {

    private val dslContext: DSLContext = mockk()
    private val workspaceJoinDao: WorkspaceJoinDao = mockk()

    private lateinit var service: WorkspaceRecordService

    @BeforeEach
    fun setUp() {
        service = WorkspaceRecordService(
            dslContext = dslContext,
            bkRepoConfig = mockk(),
            workspaceWindowsDao = mockk(),
            startAppLinkDao = mockk(),
            workspaceRecordUserApprovalDao = mockk(),
            workspaceDao = mockk(),
            workspaceJoinDao = workspaceJoinDao,
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
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should return page when enable is true`() {
        val names = listOf("ins-aaa", "ins-bbb")
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
        } returns 2L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 10,
                offset = 0
            )
        } returns names

        val page = service.batchQueryThumbnailWorkspaces(
            enable = true,
            page = 1,
            pageSize = 10
        )

        assertEquals(2L, page.count)
        assertEquals(1, page.page)
        assertEquals(10, page.pageSize)
        assertEquals(1, page.totalPages)
        assertEquals(names, page.records)
        verify(exactly = 1) {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 10,
                offset = 0
            )
        }
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should pass enable false through to DAO`() {
        val names = listOf("ins-closed-1")
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, false)
        } returns 1L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = false,
                limit = 20,
                offset = 0
            )
        } returns names

        val page = service.batchQueryThumbnailWorkspaces(
            enable = false,
            page = 1,
            pageSize = 20
        )

        assertEquals(1L, page.count)
        assertEquals(names, page.records)
        verify(exactly = 1) {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = false,
                limit = 20,
                offset = 0
            )
        }
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should cap pageSize to MAX_PAGE_SIZE`() {
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
        } returns 0L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 1000,
                offset = 0
            )
        } returns emptyList()

        val page = service.batchQueryThumbnailWorkspaces(
            enable = true,
            page = 1,
            pageSize = 99999
        )

        assertEquals(1000, page.pageSize)
        verify(exactly = 1) {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 1000,
                offset = 0
            )
        }
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should normalize page less than 1 to 1`() {
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
        } returns 0L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 50,
                offset = 0
            )
        } returns emptyList()

        val page = service.batchQueryThumbnailWorkspaces(
            enable = true,
            page = 0,
            pageSize = 50
        )

        assertEquals(1, page.page)
        verify(exactly = 1) {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 50,
                offset = 0
            )
        }
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should return empty page when no records`() {
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
        } returns 0L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 10,
                offset = 0
            )
        } returns emptyList()

        val page = service.batchQueryThumbnailWorkspaces(
            enable = true,
            page = 1,
            pageSize = 10
        )

        assertEquals(0L, page.count)
        assertEquals(0, page.totalPages)
        assertEquals(emptyList<String>(), page.records)
    }

    @Test
    fun `batchQueryThumbnailWorkspaces should compute offset for non-first page`() {
        val names = listOf("ins-ccc", "ins-ddd")
        every {
            workspaceJoinDao.countThumbnailWorkspaces(dslContext, true)
        } returns 25L
        every {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 10,
                offset = 20
            )
        } returns names

        val page = service.batchQueryThumbnailWorkspaces(
            enable = true,
            page = 3,
            pageSize = 10
        )

        assertEquals(25L, page.count)
        assertEquals(3, page.page)
        assertEquals(3, page.totalPages)
        assertEquals(names, page.records)
        verify(exactly = 1) {
            workspaceJoinDao.fetchThumbnailWorkspaceNames(
                dslContext = dslContext,
                enable = true,
                limit = 10,
                offset = 20
            )
        }
    }
}
