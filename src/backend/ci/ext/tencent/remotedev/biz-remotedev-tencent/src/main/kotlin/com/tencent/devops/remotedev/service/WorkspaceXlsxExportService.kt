/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.display
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class WorkspaceXlsxExportService @Autowired constructor(
    private val workspaceService: WorkspaceService
) {

    fun exportProjectWorkspaceListOp(
        userId: String,
        data: ProjectWorkspaceFetchData
    ): Response {
        val pageNotNull = data.page ?: 1
        val pageSizeNotNull = data.pageSize ?: 6666

        val search = with(data) {
            WorkspaceSearch(
                projectId = projectId?.let { listOf(it) },
                workspaceName = workspaceName?.let { listOf(it) },
                workspaceSystemType = systemType?.let { listOf(it) },
                ips = ips,
                owner = owner?.let { listOf(it) },
                status = status?.let { listOf(it) },
                zoneShortName = zoneId?.let { listOf(it) },
                size = machineType?.let { listOf(it) },
                expertSupId = expertSupId?.let { listOf(it) },
                onFuzzyMatch = true
            )
        }

        val result = workspaceService.limitFetchProjectWorkspace(
            queryType = QueryType.OP,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull),
            search = search
        )

        val records = workspaceService.parseWorkspaceList(
            userId = userId,
            result = result,
            enableExportSup = true,
            expertSupId = data.expertSupId
        )
        // 创建表
        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("实例管理")
        // 创建标题
        val titleRow = sheet.createRow(0)
        titleList.forEachIndexed { index, s ->
            titleRow.createCell(index).setCellValue(s)
        }
        // 创建内容
        var offset = 1
        records.forEach { record ->
            val ip = record.hostName?.split(".")?.let {
                it.subList(1, it.size).joinToString(separator = ".")
            }
            val row = sheet.createRow(offset)
            row.createCell(0).setCellValue(record.projectId)
            row.createCell(1).setCellValue(record.workspaceName)
            row.createCell(2).setCellValue(record.status?.display())
            row.createCell(3).setCellValue(ip)
            row.createCell(4).setCellValue(record.workspaceSystemType.name)
            row.createCell(5).setCellValue(record.winConfig?.size)
            row.createCell(6).setCellValue(record.zoneConfig?.zone)
            row.createCell(7).setCellValue(record.createUserId)
            row.createCell(8).setCellValue(record.owner)
            row.createCell(9).setCellValue(record.viewers?.joinToString(","))
            offset++
        }
        // 调整宽度
        titleList.forEachIndexed { index, _ ->
            sheet.trackAllColumnsForAutoSizing()
            sheet.autoSizeColumn(index)
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

    fun exportProjectWorkspaceListWeb(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Response {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val search = WorkspaceSearch(
            projectId = listOf(projectId)
        )
        val result = workspaceService.limitFetchProjectWorkspace(
            queryType = QueryType.WEB,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull),
            search = search
        )

        val records = workspaceService.parseWorkspaceList(
            userId = userId,
            result = result,
            enableExportSup = false,
            expertSupId = null
        )

        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("实例管理")
        // 创建标题
        val titleRow = sheet.createRow(0)
        webTitleList.forEachIndexed { index, s ->
            titleRow.createCell(index).setCellValue(s)
        }
        // 创建内容
        var offset = 1
        records.forEach { record ->
            val row = sheet.createRow(offset)
            row.createCell(0).setCellValue(record.workspaceName)
            row.createCell(1).setCellValue(record.hostName)
            row.createCell(2).setCellValue(record.status?.display())
            row.createCell(3).setCellValue(record.winConfig?.size)
            row.createCell(4).setCellValue(record.zoneConfig?.zone)
            row.createCell(5).setCellValue(record.macAddress)
            row.createCell(6).setCellValue(record.owner)
            row.createCell(7).setCellValue(record.viewers?.joinToString(";"))
            offset++
        }
        // 调整宽度
        webTitleList.forEachIndexed { index, _ ->
            sheet.trackAllColumnsForAutoSizing()
            sheet.autoSizeColumn(index)
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

    fun exportProjectWorkspaceListUser(
        userId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch?
    ): Response {

        val records = workspaceService.getWorkspaceList(userId, page, pageSize, search).records

        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("实例管理")
        // 创建标题
        val titleRow = sheet.createRow(0)
        userTitleList.forEachIndexed { index, s ->
            titleRow.createCell(index).setCellValue(s)
        }
        // 创建内容
        var offset = 1
        records.forEach { record ->
            val row = sheet.createRow(offset)
            row.createCell(0).setCellValue(record.workspaceName)
            row.createCell(1).setCellValue(record.workspaceSystemType.display())
            row.createCell(2).setCellValue(record.status?.display())
            row.createCell(3).setCellValue(record.zoneConfig?.zone)
            row.createCell(4).setCellValue(record.winConfig?.size)
            row.createCell(5).setCellValue(record.hostName)
            row.createCell(6).setCellValue(record.owner)
            offset++
        }
        // 调整宽度
        userTitleList.forEachIndexed { index, _ ->
            sheet.trackAllColumnsForAutoSizing()
            sheet.autoSizeColumn(index)
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

    companion object {
        private val titleList =
            listOf("项目ID", "实例名称", "状态", "云桌面ID", "系统类型", "机型", "城市", "创建人", "拥有者", "共享人")
        private val webTitleList =
            listOf("桌面 ID/别名", "内网 IP", "状态", "机型", "地域", "MAC地址", "拥有者", "共享人")
        private val userTitleList =
            listOf("桌面 ID/别名", "机器类型", "状态", "地域", "机型", "ip", "拥有者")
    }
}
