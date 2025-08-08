/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.exception.ExecuteException
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object CsvUtil {

    private val logger = LoggerFactory.getLogger(DateTimeUtil::class.java)

    /**
     * 写CSV并转换为字节流
     * @param headers 表头
     * @param cellList 表数据
     * @return
     */
    @Suppress("SpreadOperator")
    fun writeCsv(
        headers: Array<String>,
        cellList: List<Array<String?>>
    ): ByteArray {
        val bytes: ByteArray
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 写入BOM头防止乱码
        byteArrayOutputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val outputStreamWriter = OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)
        val bufferedWriter = BufferedWriter(outputStreamWriter)
        var csvPrinter: CSVPrinter? = null
        try {
            // 创建csvPrinter并设置表格头
            csvPrinter = CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader(*headers))
            // 写数据
            csvPrinter.printRecords(cellList)
            csvPrinter.flush()
            bytes = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()).toByteArray()
        } catch (e: IOException) {
            logger.error("writeCsv error:", e)
            throw ExecuteException("writeCsv error")
        } finally {
            try {
                csvPrinter?.close()
                bufferedWriter.close()
                outputStreamWriter.close()
                byteArrayOutputStream.close()
            } catch (e: IOException) {
                logger.error("stream close error:", e)
            }
        }
        return bytes
    }

    /**
     * 设置csv下载响应
     * @param fileName
     * @param bytes
     * @param response
     */
    fun setCsvResponse(
        fileName: String,
        bytes: ByteArray,
        response: HttpServletResponse
    ) {
        try {
            val convertFileName = URLEncoder.encode("$fileName.csv", StandardCharsets.UTF_8.name())
            response.contentType = "application/csv"
            response.characterEncoding = StandardCharsets.UTF_8.name()
            response.setHeader("Pragma", "public")
            response.setHeader("Cache-Control", "max-age=30")
            response.setHeader("Content-Disposition", "attachment; filename=$convertFileName")
            val outputStream: OutputStream = response.outputStream
            outputStream.write(bytes)
            outputStream.flush()
        } catch (e: IOException) {
            logger.error("stream close error:", e)
            throw ExecuteException("setCsvResponse error")
        }
    }
}
