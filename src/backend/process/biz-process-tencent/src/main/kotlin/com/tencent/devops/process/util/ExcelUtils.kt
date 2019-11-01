package com.tencent.devops.process.util

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
            throw RuntimeException("Excel格式错误，或文件不存在")
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