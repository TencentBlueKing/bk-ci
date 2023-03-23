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

package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.INCORRECT_EXCEL_FORMAT
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.util.IOUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.FileInputStream

object ExcelUtils {

    private val logger = LoggerFactory.getLogger(ExcelUtils::class.java)

    fun getAccountFromExcel(filePath: String): Map<String, String> {
        var workBook: Workbook? = null
        try {
            workBook = FileInputStream(filePath).use { fis ->
                when {
                    filePath.toLowerCase().endsWith("xlsx") -> XSSFWorkbook(fis)
                    filePath.toLowerCase().endsWith("xls") -> HSSFWorkbook(fis)
                    else -> HSSFWorkbook(fis)
                }
            }

            val result = mutableMapOf<String, String>()
            // 获取第一个sheet
            val sheet = workBook!!.getSheetAt(0)
            // 第0行是表头，忽略，从第二行开始读取
            for (rowNum in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowNum)
                if (null == row.getCell(0)) {
                    continue
                }
                row.getCell(0).setCellType(CellType.STRING)
                val account = row.getCell(0).stringCellValue
                val password = if (null != row.getCell(1)) {
                    row.getCell(1).setCellType(CellType.STRING)
                    row.getCell(1).stringCellValue
                } else ""
                result[account] = password
            }

            return result
        } catch (e: Exception) {
            logger.error("Excel format error!", e)
            throw RuntimeException(
                MessageUtil.getMessageByLocale(
                    messageCode = INCORRECT_EXCEL_FORMAT,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        } finally {
            if (null != workBook) {
                IOUtils.closeQuietly(workBook)
            }
        }
    }
}

// fun main(args: Array<String>) {
//    val path = "/Users/johuang/Downloads/WeTestAccountTemplate.xlsx"
//    val result = ExcelUtils.getAccountFromExcel(path)
//
//    println(result)
// }
