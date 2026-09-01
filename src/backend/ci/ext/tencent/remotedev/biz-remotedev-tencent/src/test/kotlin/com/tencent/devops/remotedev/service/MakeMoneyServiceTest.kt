package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceUseSnapshotsDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.service.MakeMoneyService.CmdbAssetDetail
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MakeMoneyServiceTest {

    private lateinit var service: MakeMoneyService

    @BeforeEach
    fun setUp() {
        val bkConfig = mockk<BkConfig>()
        every { bkConfig.cmdbAssetDetailUrl } returns CMDB_URL
        every { bkConfig.ccBizId } returns 2
        every { bkConfig.cmdbHeaderStr() } returns "{}"
        mockkObject(OkhttpUtils)

        service = MakeMoneyService(
            dslContext = mockk<DSLContext>(),
            historyDao = mockk<WorkspaceOpHistoryDao>(),
            workspaceDao = mockk<WorkspaceDao>(),
            snapshotsDao = mockk<WorkspaceUseSnapshotsDao>(),
            bkConfig = bkConfig,
            workspaceWindowsDao = mockk<WorkspaceWindowsDao>(),
            windowsResourceConfigService = mockk<WindowsResourceConfigService>()
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

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

    @Test
    @DisplayName("CMDB分页应按实例ID稳定排序并按limit推进偏移量")
    fun `fetchCmdbAssetInfo should use stable sort and limit offset`() {
        val requestBodySlot = slot<String>()
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = capture(requestBodySlot),
                headers = any()
            )
        } returnsMany listOf(
            buildResponse(
                body = cmdbResponse(
                    count = 501,
                    assets = fullPageAssets()
                )
            ),
            buildResponse(
                body = cmdbResponse(
                    count = 501,
                    assets = listOf(asset(ip = "10.0.2.1"))
                )
            )
        )

        val result = service.fetchCmdbAssetInfo()
        val lastRequest = requestBodySlot.captured

        assertEquals(501, result.size)
        assertEquals("bk_inst_id", readPageField(lastRequest, "sort"))
        assertEquals("500", readPageField(lastRequest, "start"))
    }

    @Test
    @DisplayName("CMDB中间页HTTP失败时不应返回部分数据")
    fun `fetchCmdbAssetInfo should discard partial data on http failure`() {
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = any(),
                headers = any()
            )
        } returnsMany listOf(
            buildResponse(
                body = cmdbResponse(
                    count = 501,
                    assets = fullPageAssets()
                )
            ),
            buildResponse(code = 500, body = "internal error")
        )

        val exception = assertThrows(RemoteServiceException::class.java) {
            service.fetchCmdbAssetInfo()
        }
        assertEquals("fetch CMDB asset failed, start=500, code=500", exception.errorMessage)
    }

    @Test
    @DisplayName("CMDB中间页业务失败时不应返回部分数据")
    fun `fetchCmdbAssetInfo should discard partial data on business failure`() {
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = any(),
                headers = any()
            )
        } returnsMany listOf(
            buildResponse(
                body = cmdbResponse(
                    count = 501,
                    assets = fullPageAssets()
                )
            ),
            buildResponse(body = """{"result":false,"message":"busy","data":null}""")
        )

        val exception = assertThrows(RemoteServiceException::class.java) {
            service.fetchCmdbAssetInfo()
        }
        assertEquals("fetch CMDB asset failed, start=500, message=busy", exception.errorMessage)
    }

    @Test
    @DisplayName("CMDB非末页返回短页时应终止本轮拉取")
    fun `fetchCmdbAssetInfo should fail on unexpected short page`() {
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = any(),
                headers = any()
            )
        } returns buildResponse(
            body = cmdbResponse(
                count = 501,
                assets = listOf(asset(ip = "10.0.0.1"))
            )
        )

        assertThrows(RemoteServiceException::class.java) {
            service.fetchCmdbAssetInfo()
        }
    }

    @Test
    @DisplayName("CMDB分页期间总数变化时应终止本轮拉取")
    fun `fetchCmdbAssetInfo should fail when count changes`() {
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = any(),
                headers = any()
            )
        } returnsMany listOf(
            buildResponse(
                body = cmdbResponse(
                    count = 501,
                    assets = fullPageAssets()
                )
            ),
            buildResponse(
                body = cmdbResponse(
                    count = 502,
                    assets = listOf(
                        asset(ip = "10.0.2.1"),
                        asset(ip = "10.0.2.2")
                    )
                )
            )
        )

        assertThrows(RemoteServiceException::class.java) {
            service.fetchCmdbAssetInfo()
        }
    }

    @Test
    @DisplayName("CMDB结果应过滤空IP并按IP去重")
    fun `fetchCmdbAssetInfo should filter blank ip and deduplicate`() {
        every {
            OkhttpUtils.doPost(
                url = CMDB_URL,
                jsonParam = any(),
                headers = any()
            )
        } returns buildResponse(
            body = cmdbResponse(
                count = 3,
                assets = listOf(
                    asset(ip = "10.0.0.1", property = "旧物管"),
                    asset(ip = "", property = "空IP"),
                    asset(ip = "10.0.0.1", property = "新物管")
                )
            )
        )

        val result = service.fetchCmdbAssetInfo()

        assertEquals(1, result.size)
        assertEquals("新物管", result.getValue("10.0.0.1").propertyManagementBelongs)
    }

    private fun readPageField(requestBody: String, field: String): String {
        return jacksonObjectMapper()
            .readTree(requestBody)
            .path("page")
            .path(field)
            .asText()
    }

    private fun cmdbResponse(count: Int, assets: List<String>): String {
        return """{"result":true,"message":"ok","data":{"count":$count,"info":[${assets.joinToString()}]}}"""
    }

    private fun fullPageAssets(): List<String> {
        return (1..500).map { index ->
            asset(ip = "10.0.1.$index")
        }
    }

    private fun asset(
        ip: String,
        property: String = "蓝盾云研发"
    ): String {
        return """{"cds_ip":"$ip","start_date":"2025-08-01","property_management_belongs":"$property"}"""
    }

    private fun buildResponse(
        code: Int = 200,
        body: String
    ): Response {
        return Response.Builder()
            .code(code)
            .message(if (code == 200) "OK" else "ERROR")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url(CMDB_URL).build())
            .body(body.toResponseBody(null))
            .build()
    }

    companion object {
        private const val CMDB_URL = "https://cmdb.example.com/api/assets"
    }
}
