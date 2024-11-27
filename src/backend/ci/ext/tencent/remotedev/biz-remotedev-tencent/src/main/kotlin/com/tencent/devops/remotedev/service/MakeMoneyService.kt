package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceUseSnapshotsDao
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import java.time.LocalDateTime
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
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
    private val snapshotsDao: WorkspaceUseSnapshotsDao
) {
    data class SaveData(
        val projectId: String,
        val projectName: String,
        val status: WorkspaceStatus
    )

    companion object {
        private val logger = LoggerFactory.getLogger(MakeMoneyService::class.java)
    }

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

        val use = a + c - d - b + e - f

        save(use, a, aMap, lastDay)
        return output(a, b, c, d, e, f, use)
    }

    private fun output(
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
        ).header("Content-disposition", "attachment;filename=InstanceManagement.xlsx")
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
            aMap.putAll(res.associateBy({ it.name }, {
                SaveData(
                    it.projectId, it.projectName, WorkspaceStatus.load(it.status)
                )
            }))
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
            ).associateBy({ it.name }, { SaveData(it.projectId, it.projectName, WorkspaceStatus.load(it.status)) })
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
}
