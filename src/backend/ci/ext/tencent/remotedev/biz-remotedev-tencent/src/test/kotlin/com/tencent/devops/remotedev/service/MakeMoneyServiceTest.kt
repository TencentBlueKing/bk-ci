package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceUseSnapshotsDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.service.MakeMoneyService.CmdbAssetDetail
import io.mockk.mockk
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MakeMoneyServiceTest {

    private val service = MakeMoneyService(
        dslContext = mockk<DSLContext>(),
        historyDao = mockk<WorkspaceOpHistoryDao>(),
        workspaceDao = mockk<WorkspaceDao>(),
        snapshotsDao = mockk<WorkspaceUseSnapshotsDao>(),
        bkConfig = mockk<BkConfig>(),
        workspaceWindowsDao = mockk<WorkspaceWindowsDao>(),
        windowsResourceConfigService = mockk<WindowsResourceConfigService>()
    )

    @Test
    @DisplayName("高配机型 + 蓝盾云研发 -> 收取硬件成本")
    fun `hardwareCost should be 1 when high-end machine and BkCloudDev`() {
        val cmdbDetail = CmdbAssetDetail(
            commissionDate = "2025-08",
            propertyManagementBelongs = "蓝盾云研发"
        )
        assertEquals(
            1,
            service.calculateHardwareCost(cmdbDetail, "高配美术机")
        )
    }

    @Test
    @DisplayName("高配开发机 + 蓝盾云研发 -> 收取硬件成本（扩展机型）")
    fun `hardwareCost should be 1 for new high-end machine type`() {
        val cmdbDetail = CmdbAssetDetail(
            commissionDate = "2025-06",
            propertyManagementBelongs = "蓝盾云研发"
        )
        assertEquals(
            1,
            service.calculateHardwareCost(cmdbDetail, "高配开发机")
        )
    }

    @Test
    @DisplayName("高配机型 + 非蓝盾云研发 -> 不收取硬件成本")
    fun `hardwareCost should be 0 when high-end but not BkCloudDev`() {
        val cmdbDetail = CmdbAssetDetail(
            commissionDate = "2025-08",
            propertyManagementBelongs = "其他部门"
        )
        assertEquals(
            0,
            service.calculateHardwareCost(cmdbDetail, "高配美术机")
        )
    }

    @Test
    @DisplayName("非高配机型（machineFlag为空）-> 不收取硬件成本")
    fun `hardwareCost should be 0 when machineFlag is blank`() {
        val cmdbDetail = CmdbAssetDetail(
            commissionDate = "2025-08",
            propertyManagementBelongs = "蓝盾云研发"
        )
        assertEquals(
            0,
            service.calculateHardwareCost(cmdbDetail, "")
        )
    }

    @Test
    @DisplayName("不在CMDB资产系统中 -> 不收取硬件成本")
    fun `hardwareCost should be 0 when not in CMDB`() {
        assertEquals(
            0,
            service.calculateHardwareCost(null, "高配美术机")
        )
    }

    @Test
    @DisplayName("不在CMDB + machineFlag为空 -> 不收取硬件成本")
    fun `hardwareCost should be 0 when not in CMDB and no flag`() {
        assertEquals(
            0,
            service.calculateHardwareCost(null, "")
        )
    }

    @Test
    @DisplayName("物管所属为空字符串 -> 不收取硬件成本")
    fun `hardwareCost should be 0 when propertyManagement is empty`() {
        val cmdbDetail = CmdbAssetDetail(
            commissionDate = "2025-08",
            propertyManagementBelongs = ""
        )
        assertEquals(
            0,
            service.calculateHardwareCost(cmdbDetail, "高配美术机")
        )
    }
}
