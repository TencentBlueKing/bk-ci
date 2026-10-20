package com.tencent.devops.remotedev.service.expert

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpertSupportServiceGenerateTokenTest {

    private val dslContext: DSLContext = mockk(relaxed = true)
    private val expertSupportDao: ExpertSupportDao = mockk()
    private val workspaceJoinDao: WorkspaceJoinDao = mockk()
    private val windowsResourceZoneDao: WindowsResourceZoneDao = mockk()
    private val tokenGenerator: ExpertSupportTokenGenerator = mockk()

    private lateinit var service: ExpertSupportService

    private val userId = "eastjiao"
    private val workspaceName = "GZ2.1.2.3.4"

    @BeforeEach
    fun setUp() {
        val realService = ExpertSupportService(
            dslContext = dslContext,
            client = mockk(relaxed = true),
            expertSupportDao = expertSupportDao,
            workspaceCommon = mockk(relaxed = true),
            workspaceSharedDao = mockk(relaxed = true),
            workspaceDao = mockk(relaxed = true),
            remoteDevSettingDao = mockk(relaxed = true),
            workspaceOpHistoryDao = mockk(relaxed = true),
            permissionService = mockk(relaxed = true),
            streamBridge = mockk(relaxed = true),
            notifyControl = mockk(relaxed = true),
            workspaceJoinDao = workspaceJoinDao,
            remoteDevService = mockk(relaxed = true),
            startCloudClient = mockk(relaxed = true),
            bkNodemanService = mockk(relaxed = true),
            configCacheService = mockk(relaxed = true),
            remoteDevServiceFactory = mockk(relaxed = true),
            workspaceService = mockk(relaxed = true),
            windowsResourceZoneDao = windowsResourceZoneDao,
            expertSupportTokenGenerator = tokenGenerator
        )
        service = spyk(realService)
    }

    @Test
    fun `generateUserSupportToken should store record and return token`() {
        val record = mockRecord(zoneId = "GZ2", hostIp = "GZ2.1.2.3.4", winConfigId = 8)
        every {
            workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName)
        } returns record
        every { windowsResourceZoneDao.fetchAny(dslContext, "GZ2") } returns null
        every { windowsResourceZoneDao.fetchAny(dslContext, "GZ") } returns zoneConfig("广州")

        val dataSlot = slot<CreateSupportData>()
        every { service.createSupportNew(userId, capture(dataSlot)) } returns 555L
        every { tokenGenerator.generate(userId, 555L) } returns "mock-token"

        val token = service.generateUserSupportToken(userId, workspaceName)

        assertEquals("mock-token", token)
        val captured = dataSlot.captured
        assertEquals("广州", captured.city)
        assertEquals("瘦终端一键求助", captured.content)
        assertEquals(userId, captured.creator)
        assertEquals(workspaceName, captured.workspaceName)
        assertEquals("8", captured.machineType)
        verify(exactly = 1) { tokenGenerator.generate(userId, 555L) }
    }

    @Test
    fun `resolveCity should use exact short name match when available`() {
        val record = mockRecord(zoneId = "SZ", hostIp = "SZ.1.2.3.4", winConfigId = 1)
        every {
            workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName)
        } returns record
        every { windowsResourceZoneDao.fetchAny(dslContext, "SZ") } returns zoneConfig("深圳")

        val dataSlot = slot<CreateSupportData>()
        every { service.createSupportNew(userId, capture(dataSlot)) } returns 1L
        every { tokenGenerator.generate(any(), any()) } returns "t"

        service.generateUserSupportToken(userId, workspaceName)

        assertEquals("深圳", dataSlot.captured.city)
    }

    @Test
    fun `resolveCity should fallback to short name when zone not found`() {
        val record = mockRecord(zoneId = "XX9", hostIp = "XX9.1.2.3.4", winConfigId = 1)
        every {
            workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName)
        } returns record
        every { windowsResourceZoneDao.fetchAny(dslContext, any()) } returns null

        val dataSlot = slot<CreateSupportData>()
        every { service.createSupportNew(userId, capture(dataSlot)) } returns 1L
        every { tokenGenerator.generate(any(), any()) } returns "t"

        service.generateUserSupportToken(userId, workspaceName)

        assertEquals("XX9", dataSlot.captured.city)
    }

    @Test
    fun `generateUserSupportToken should throw when workspace not found`() {
        every {
            workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName)
        } returns null

        assertThrows(ErrorCodeException::class.java) {
            service.generateUserSupportToken(userId, workspaceName)
        }
    }

    @Test
    fun `generateUserSupportToken should store personal record when workspaceName is null`() {
        assertPersonalSupport(workspaceName = null)
    }

    @Test
    fun `generateUserSupportToken should store personal record when workspaceName is blank`() {
        assertPersonalSupport(workspaceName = "  ")
    }

    private fun assertPersonalSupport(workspaceName: String?) {
        every {
            expertSupportDao.addSupport(
                dslContext = dslContext,
                projectId = "",
                hostIp = "",
                creator = userId,
                status = ExpertSupportStatus.CREATE,
                content = "瘦终端一键求助",
                workspaceName = "",
                city = "",
                machineType = "",
                info = any()
            )
        } returns 666L
        every { tokenGenerator.generate(userId, 666L) } returns "personal-token"

        val token = service.generateUserSupportToken(userId, workspaceName)

        assertEquals("personal-token", token)
        verify(exactly = 0) { workspaceJoinDao.fetchAnyWindowsWorkspace(any(), any()) }
        verify(exactly = 0) { service.createSupportNew(any(), any()) }
    }

    private fun mockRecord(zoneId: String, hostIp: String, winConfigId: Int): WorkspaceRecordWithWindows {
        val record = mockk<WorkspaceRecordWithWindows>()
        every { record.projectId } returns "test-project"
        every { record.workspaceName } returns workspaceName
        every { record.hostIp } returns hostIp
        every { record.zoneId } returns zoneId
        every { record.winConfigId } returns winConfigId
        return record
    }

    private fun zoneConfig(zone: String) = WindowsResourceZoneConfig(
        id = 1L,
        available = true,
        zone = zone,
        zoneShortName = zone,
        description = "",
        type = WindowsResourceZoneConfigType.DEFAULT,
        provider = "IEG_BKCI"
    )
}
