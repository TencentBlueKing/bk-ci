package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.common.Constansts.BAK_FLAG
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceUseSnapshotsDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import java.lang.Long.bitCount
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MakeMoneyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val historyDao: WorkspaceOpHistoryDao,
    private val workspaceDao: WorkspaceDao,
    private val snapshotsDao: WorkspaceUseSnapshotsDao,
    private val bkConfig: BkConfig,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val windowsResourceConfigService: WindowsResourceConfigService
) {
    data class SaveData(
        val projectId: String,
        val projectName: String,
        val status: WorkspaceStatus,
        val ip: String,
        val bgName: String,
        val machineType: String,
        val creator: String
    )

    companion object {
        private val logger = LoggerFactory.getLogger(MakeMoneyService::class.java)
    }

    /**
     * CMDB API响应数据类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CmdbAssetResponse(
        val result: Boolean?,
        val message: String?,
        val data: CmdbAssetData?
    )

    /**
     * CMDB资产数据类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CmdbAssetData(
        val info: List<CmdbAssetInfo>?
    )

    /**
     * CMDB资产详情类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class CmdbAssetInfo(
        @JsonProperty("bk_inst_name")
        val bkInstName: String,
        @JsonProperty("start_date")
        val startDate: String?
    )

    /*
     * 注意：计算当天是不在计算时间范围内的
     *
     * 计算顺序: a + (c - d - b) + (e - f)
     *
     * a: 当前在用(非PREPARING、非DELETED、非DELIVERING_FAILED)的实例
     * b: 今天新建且成功交付的实例
     * c: 今天删除的实例
     * d: 今天未成功交付且删除的实例
     * e: 昨天删除的实例
     * f: 昨天未成功交付且删除的实例
     * */
    fun makeMoneyLastDay(): Response {
        val now = LocalDateTime.now()
        val lastDay = now.plusDays(-1)
        val b = historyDao.fetchHistoryByData(dslContext, WorkspaceAction.CREATE_SUCCESS, now)
            .map { it.workspaceName }.toSet()
        val c = historyDao.fetchHistoryByData(dslContext, WorkspaceAction.DELETE, now)
            .map { it.workspaceName }.toSet()
        val d = historyDao.fetchHistoryByData(dslContext, WorkspaceAction.DELETE_IN_INITIALIZING, now)
            .map { it.workspaceName }.toSet()
        val e = historyDao.fetchHistoryByData(dslContext, WorkspaceAction.DELETE, lastDay)
            .map { it.workspaceName }.toSet()
        val f = historyDao.fetchHistoryByData(dslContext, WorkspaceAction.DELETE_IN_INITIALIZING, lastDay)
            .map { it.workspaceName }.toSet()
        val aMap = aMap()
        val a = aMap.keys

        val use = (a + c - d - b + e - f).toMutableSet()

        removeBakWorkspace(use)
        save(use, a, aMap, lastDay)
        return makeMoneyLastDayOutput(a, b, c, d, e, f, use)
    }

    private fun removeBakWorkspace(use: MutableSet<String>) {
        use.removeIf { it.contains(BAK_FLAG) }
    }

    private fun makeMoneyLastDayOutput(
        a: Set<String>,
        b: Set<String>,
        c: Set<String>,
        d: Set<String>,
        e: Set<String>,
        f: Set<String>,
        use: Set<String>
    ): Response {
        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("Data")

        val maxRows = maxOf(a.size, b.size, c.size, d.size, e.size, f.size, use.size)
        sheet.createRow(0).let { row ->
            row.createCell(0).setCellValue("当前在用(非PREPARING、非DELETED、非DELIVERING_FAILED)的实例")
            row.createCell(1).setCellValue("今天新建且成功交付的实例")
            row.createCell(2).setCellValue("今天删除的实例")
            row.createCell(3).setCellValue("今天未成功交付且删除的实例")
            row.createCell(4).setCellValue("昨天删除的实例")
            row.createCell(5).setCellValue("昨天未成功交付且删除的实例")
            row.createCell(6).setCellValue("昨天在使用的实例(快照结果)")
        }

        for (i in 0 until maxRows) {
            val row = sheet.createRow(i + 1)

            row.createCell(0).setCellValue(a.elementAtOrNull(i) ?: "")
            row.createCell(1).setCellValue(b.elementAtOrNull(i) ?: "")
            row.createCell(2).setCellValue(c.elementAtOrNull(i) ?: "")
            row.createCell(3).setCellValue(d.elementAtOrNull(i) ?: "")
            row.createCell(4).setCellValue(e.elementAtOrNull(i) ?: "")
            row.createCell(5).setCellValue(f.elementAtOrNull(i) ?: "")
            row.createCell(6).setCellValue(use.elementAtOrNull(i) ?: "")
        }
        for (i in 0 until 7) {
            sheet.trackAllColumnsForAutoSizing()
            sheet.autoSizeColumn(i)
        }
        return Response.ok(
            StreamingOutput { output ->
                workbook.write(output)
                workbook.dispose()
            },
            MediaType.APPLICATION_OCTET_STREAM
        ).header("Content-disposition", "attachment;filename=makeMoneyLastDay.xlsx")
            .build()
    }

    private fun aMap(): MutableMap<String, SaveData> {
        val aMap = mutableMapOf<String, SaveData>()
        var page = 1
        while (true) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, 100)
            val res = workspaceDao.limitFetchWorkspace(
                dslContext = dslContext,
                limit = sqlLimit,
                notStatus = setOf(WorkspaceStatus.PREPARING, WorkspaceStatus.DELETED, WorkspaceStatus.DELIVERING_FAILED)
            )
            if (res.isEmpty()) break
            aMap.putAll(
                res.associateBy({ it.name }, {
                    SaveData(
                        projectId = it.projectId,
                        projectName = it.projectName,
                        status = WorkspaceStatus.load(it.status),
                        ip = it.ip,
                        bgName = it.creatorBgName,
                        creator = it.creator,
                        machineType = ""
                    )
                })
            )
            page += 1
        }
        return aMap
    }

    private fun save(
        use: Set<String>,
        a: MutableSet<String>,
        aMap: MutableMap<String, SaveData>,
        lastDay: LocalDateTime
    ) {
        use.chunked(99).forEach { chunk ->
            val filter = chunk.filter { name -> name !in a }
            val res = workspaceDao.limitFetchWorkspace(
                dslContext = dslContext,
                limit = PageUtil.convertPageSizeToSQLLimit(1, 100),
                workspaceName = filter.toSet()
            ).associateBy({ it.name }, {
                SaveData(
                    projectId = it.projectId,
                    projectName = it.projectName,
                    status = WorkspaceStatus.load(it.status),
                    ip = it.ip,
                    bgName = it.creatorBgName,
                    creator = it.creator,
                    machineType = ""
                )
            })
            // 快照昨天在使用的实例
            aMap.filterKeys { it in chunk }.ifEmpty { null }?.let { save ->
                snapshotsDao.createWorkspaceHistory(dslContext, save, lastDay)
            }
            // 快照昨天用户删除的实例
            res.ifEmpty { null }?.let { save ->
                snapshotsDao.createWorkspaceHistory(dslContext, save, lastDay)
            }
        }
    }

    @Schema(title = "云研发货币化数据")
    private data class Bill(
        @get:Schema(title = "数据源名称")
        @JsonProperty(value = "data_source_bills", required = true)
        val dataSourceBills: SourceBills
    ) {
        @Schema(title = "云研发数据源货币化数据")
        data class SourceBills(
            @get:Schema(title = "数据源名称")
            @JsonProperty(value = "data_source_name", required = true)
            val dataSourceName: String = "云研发服务货币化",
            @get:Schema(title = "货币化数据")
            val bills: List<BillDetail>,
            @get:Schema(title = "是否覆盖账期内的数据后重新导入")
            @JsonProperty(value = "is_overwrite", required = true)
            val overwrite: Boolean,
            @get:Schema(title = "YYYYMM")
            @JsonProperty(value = "month", required = true)
            val month: String
        )

        data class BillDetail(
            @JsonProperty("cost_date")
            @get:Schema(title = "账单周期（月）")
            val costDate: String,
            @JsonProperty("project_id")
            @get:Schema(title = "项目id")
            val projectId: String,
            @get:Schema(title = "项目名称")
            val name: String,
            @JsonProperty("service_type")
            @get:Schema(title = "服务类型")
            val serviceType: String,
            @get:Schema(title = "指标名称")
            val kind: String,
            @JsonProperty("res_id")
            @get:Schema(title = "资源ID")
            val resId: String,
            @JsonProperty("bg_name")
            @get:Schema(title = "云桌面所属BG")
            val bgName: String,
            @get:Schema(title = "IEG的传 1，非IEG 传 0")
            val flag: Int,
            @get:Schema(title = "主机IP")
            val ip: String,
            @get:Schema(title = "使用天数")
            val usage: Int,
            @JsonProperty("daydetail")
            @get:Schema(title = "日明细数据")
            val dayDetail: Map<String, Int>,
            @get:Schema(title = "创建人")
            val creator: String,
            @JsonProperty("machine_type")
            @get:Schema(title = "机型")
            val machineType: String,
            @JsonProperty("hardware_cost")
            @get:Schema(title = "是否计算硬件成本：0或1")
            val hardwareCost: Int,
            @JsonProperty("machine_flag")
            @get:Schema(title = "机型标识：高配开发机、高配美术机")
            val machineFlag: String,
            @JsonProperty("commission_date")
            @get:Schema(title = "启用日期：YYYY-MM")
            val commissionDate: String
        )
    }

    fun bills(year: Int, month: Int, push: Boolean): Response {
        val bills = makeMoneyMonthly(year, month)
        if (push) {
            pushBills(year, month, bills)
        }
        return makeMoneyMonthlyOutput(bills)
    }

    private fun pushBills(year: Int, month: Int, bills: List<Bill.BillDetail>) {
        val date = LocalDate.of(year, month, 14).format(DateTimeFormatter.ofPattern("yyyyMM"))
        val requestBody = JsonUtil.toJson(Bill(Bill.SourceBills(bills = bills, overwrite = true, month = date)))
        logger.info("start pushBills|url:${bkConfig.billsPushUrl}|count=${bills.size}")
        OkhttpUtils.doPost(
            url = bkConfig.billsPushUrl,
            jsonParam = requestBody,
            headers = mapOf("Platform-Key" to bkConfig.billsPlatformKey)
        ).use {
            if (!it.isSuccessful) {
                logger.warn("push bills data failed|code: ${it.code}|response: ${it.body?.string()}")
                throw RemoteServiceException("request bill data failed code: ${it.code},response: ${it.body?.string()}")
            }
            logger.info("push bills|code: ${it.code}|response: ${it.body?.string()}")
        }
    }

    /**
     * 从CMDB API查询资产详情信息，构建实例名到启用日期的映射
     * @return Map<String, String> - Key为bk_inst_name（实例名），Value为格式化后的启用日期（YYYY-MM）
     */
    private fun fetchCmdbAssetInfo(): Map<String, String> {
        return try {
            logger.info("start fetchCmdbAssetInfo|url:${bkConfig.cmdbAssetDetailUrl}")
            val resultMap = mutableMapOf<String, String>()
            var start = 0
            val limit = 500
            var hasMore = true

            while (hasMore) {
                val requestBody = JsonUtil.toJson(
                    mapOf(
                        "bk_biz_id" to (bkConfig.ccBizId ?: 0),
                        "page" to mapOf("start" to start, "limit" to limit),
                        "fields" to listOf("bk_inst_name", "start_date")
                    )
                )

                OkhttpUtils.doPost(
                    url = bkConfig.cmdbAssetDetailUrl,
                    jsonParam = requestBody,
                    headers = mapOf("X-Bkapi-Authorization" to bkConfig.cmdbHeaderStr())
                ).use { response ->
                    if (!response.isSuccessful) {
                        logger.warn("fetchCmdbAssetInfo failed|start: $start|code: ${response.code}|response: ${response.body?.string()}")
                        hasMore = false
                        return@use
                    }

                    val responseBody = response.body?.string() ?: ""
                    logger.info("fetchCmdbAssetInfo page|start: $start|limit: $limit")
                    val cmdbResponse: CmdbAssetResponse = jacksonObjectMapper().readValue(responseBody)

                    if (cmdbResponse.result != true || cmdbResponse.data?.info == null) {
                        logger.warn("fetchCmdbAssetInfo result is false or data is null|start: $start")
                        hasMore = false
                        return@use
                    }

                    val currentPageData = cmdbResponse.data.info
                    if (currentPageData.isEmpty()) {
                        hasMore = false
                    } else {
                        currentPageData.forEach { asset ->
                            resultMap[asset.bkInstName] = formatCommissionDate(asset.startDate)
                        }

                        // 如果返回的数据量小于limit，说明已经是最后一页
                        if (currentPageData.size < limit) {
                            hasMore = false
                        } else {
                            start += limit
                        }
                    }
                }
            }

            logger.info("fetchCmdbAssetInfo completed|total size: ${resultMap.size}")
            resultMap
        } catch (e: Exception) {
            logger.error("fetchCmdbAssetInfo exception", e)
            emptyMap()
        }
    }

    /**
     * 将CMDB返回的启用日期格式化为YYYY-MM格式
     * @param startDate CMDB返回的启用日期（格式：YYYY-MM-DD）
     * @return 格式化后的日期（格式：YYYY-MM），如果输入为空或格式错误则返回空字符串
     */
    private fun formatCommissionDate(startDate: String?): String {
        return try {
            if (startDate.isNullOrBlank()) return ""
            val parts = startDate.split("-")
            if (parts.size >= 2) {
                "${parts[0]}-${parts[1]}"
            } else {
                startDate
            }
        } catch (e: Exception) {
            logger.warn("formatCommissionDate failed|startDate: $startDate", e)
            startDate ?: ""
        }
    }

    private fun makeMoneyMonthly(year: Int, month: Int): List<Bill.BillDetail> {
        // 获取CMDB资产信息
        val cmdbAssetMap = fetchCmdbAssetInfo()
        logger.info("fetchCmdbAssetInfo result size: ${cmdbAssetMap.size}")

        val end = LocalDate.of(year, month, 14)
        val costData = end.format(DateTimeFormatter.ofPattern("yyyyMM"))
        val start = end.plusMonths(-1).plusDays(1)
        val daysBetween = ChronoUnit.DAYS.between(start, end) + 1
        val total = mutableMapOf<String, Long>()
        // 避免数据量太大，一天一天处理
        for (dayIndex in 0 until daysBetween) {
            val date = start.plusDays(dayIndex)
            val workspaces = snapshotsDao.fetchWorkspaceNameDaily(dslContext, date)
            workspaces.forEach { name ->
                total[name] = (total[name] ?: 0L) + (1L shl dayIndex.toInt())
            }
        }
        val dateList = getDateList(start, end)
        val allConfig = windowsResourceConfigService.getAllType(true, null).associateBy { it.id!! }
        val res = mutableListOf<Bill.BillDetail>()
        // 分块处理减少性能压力
        total.keys.chunked(99).forEach { chunk ->

            // 获取实例对应的机型
            val allWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
                dslContext,
                chunk.toSet()
            ).associateBy { it.workspaceName }

            val workspaceInfo = workspaceDao.limitFetchWorkspace(
                dslContext = dslContext,
                limit = PageUtil.convertPageSizeToSQLLimit(1, 100),
                workspaceName = chunk.toSet()
            ).associateBy({ it.name }, {
                SaveData(
                    projectId = it.projectId,
                    projectName = it.projectName,
                    status = WorkspaceStatus.load(it.status),
                    ip = it.ip,
                    bgName = it.creatorBgName,
                    machineType = allWindows[it.name]?.let { i -> allConfig[i.winConfigId.toLong()]?.size } ?: "",
                    creator = it.creator
                )
            })
            chunk.forEach { name ->
                val workspace = workspaceInfo[name]
                val usage = bitCount(checkNotNull(total[name]))
                val dayDetail = dayDetail(checkNotNull(total[name]), dateList)

                // 从CMDB数据中匹配硬件成本信息
                val commissionDate = cmdbAssetMap[name] ?: ""
                val hardwareCost = if (commissionDate.isNotEmpty()) 1 else 0

                // 获取机型标识
                val winConfigId = allWindows[name]?.winConfigId?.toLong()
                val machineFlag = winConfigId?.let { allConfig[it]?.machineFlag } ?: ""

                res.add(
                    Bill.BillDetail(
                        costDate = costData,
                        projectId = workspace?.projectId ?: "ERROR",
                        name = workspace?.projectName ?: "ERROR",
                        serviceType = "云桌面服务",
                        kind = "CLOUD_DESKTOP",
                        resId = name,
                        ip = workspace?.ip ?: "ERROR",
                        usage = usage,
                        dayDetail = dayDetail,
                        bgName = workspace?.bgName ?: "ERROR",
                        flag = if (workspace?.bgName == "IEG互动娱乐事业群" || workspace?.bgName == "子公司组织") 1 else 0,
                        creator = workspace?.creator ?: "ERROR",
                        machineType = workspace?.machineType ?: "ERROR",
                        hardwareCost = hardwareCost,
                        machineFlag = machineFlag,
                        commissionDate = commissionDate
                    )
                )
            }
        }
        return res
    }

    private fun getDateList(start: LocalDate, end: LocalDate): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.format(formatter) }
            .toList()
    }

    /*
     * 返回数据格式参照: {"20241215": 1, "20241216": 1, "20241217": 1, "20241218": 1, "20241219": 1,
     * "20241220": 1, "20241221": 1, "20241222": 1, "20241223": 1, "20241224": 1, "20241225": 1,
     * "20241226": 1, "20241227": 1, "20241228": 1, "20241229": 1, "20241230": 1, "20241231": 1,
     * "20250101": 1, "20250102": 1, "20250103": 1, "20250104": 0, "20250105": 0, "20250106": 0,
     * "20250107": 0, "20250108": 0, "20250109": 0, "20250110": 0, "20250111": 1, "20250112": 0,
     * "20250113": 0, "20250114": 0}
     * */
    private fun dayDetail(number: Long, dataList: List<String>): Map<String, Int> {
        return generateSequence(number) { it shr 1 }
            .take(dataList.size)
            .mapIndexed { index, value ->
                if (value and 1 == 1L) dataList[index] to 1 else dataList[index] to 0
            }
            .toMap()
    }

    private fun makeMoneyMonthlyOutput(
        bills: List<Bill.BillDetail>
    ): Response {
        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("Data")

        sheet.createRow(0).let { row ->
            row.createCell(0).setCellValue("cost_date 账单周期（月）")
            row.createCell(1).setCellValue("project_id 项目id")
            row.createCell(2).setCellValue("name 项目名称")
            row.createCell(3).setCellValue("service_type 服务类型")
            row.createCell(4).setCellValue("kind 指标名称")
            row.createCell(5).setCellValue("res_id 资源ID")
            row.createCell(6).setCellValue("ip 主机IP")
            row.createCell(7).setCellValue("usage 使用天数")
            row.createCell(8).setCellValue("daydetail 日明细数据")
            row.createCell(9).setCellValue("bg_name bg名")
            row.createCell(10).setCellValue("flag")
            row.createCell(11).setCellValue("creator")
            row.createCell(12).setCellValue("machine_type")
            row.createCell(13).setCellValue("hardware_cost")
            row.createCell(14).setCellValue("machine_flag")
            row.createCell(15).setCellValue("commission_date")
        }

        bills.forEachIndexed { index, bill ->
            val row = sheet.createRow(index + 1)

            row.createCell(0).setCellValue(bill.costDate)
            row.createCell(1).setCellValue(bill.projectId)
            row.createCell(2).setCellValue(bill.name)
            row.createCell(3).setCellValue(bill.serviceType)
            row.createCell(4).setCellValue(bill.kind)
            row.createCell(5).setCellValue(bill.resId)
            row.createCell(6).setCellValue(bill.ip)
            row.createCell(7).setCellValue(bill.usage.toString())
            row.createCell(8).setCellValue(JsonUtil.toJson(bill.dayDetail, formatted = false))
            row.createCell(9).setCellValue(bill.bgName)
            row.createCell(10).setCellValue(bill.flag.toString())
            row.createCell(11).setCellValue(bill.creator)
            row.createCell(12).setCellValue(bill.machineType)
            row.createCell(13).setCellValue(bill.hardwareCost.toString())
            row.createCell(14).setCellValue(bill.machineFlag)
            row.createCell(15).setCellValue(bill.commissionDate)
        }
        for (i in 0 until 16) {
            sheet.trackAllColumnsForAutoSizing()
            sheet.autoSizeColumn(i)
        }
        return Response.ok(
            StreamingOutput { output ->
                workbook.write(output)
                workbook.dispose()
            },
            MediaType.APPLICATION_OCTET_STREAM
        ).header("Content-disposition", "attachment;filename=makeMoneyMonthly.xlsx")
            .build()
    }

    fun reduceWorkspaceBills(
        workspaceNames: List<String>,
        startDate: String,
        endDate: String
    ): Boolean {
        logger.info("reduceWorkspaceBills: $workspaceNames, $startDate, $endDate")
        return snapshotsDao.reduceWorkspaceBills(
            dslContext = dslContext,
            workspaceNames = workspaceNames,
            startDate = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            endDate = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
}
