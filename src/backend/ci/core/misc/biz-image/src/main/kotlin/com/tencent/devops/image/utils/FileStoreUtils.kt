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

package com.tencent.devops.image.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.constants.ImageMessageCode.MIRROR_FILE_SAVE_FAILED
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object FileStoreUtils {
    private val logger = LoggerFactory.getLogger(FileStoreUtils::class.java)
    private const val FILE_STORE_DIR = "/data/image-upload-fileStore"

    fun getFullFileName(fileName: String): String {
        return "$FILE_STORE_DIR/$fileName"
    }

    fun storeFile(inputStream: InputStream): String {
        val fileStoreDir = File(FILE_STORE_DIR)
        if (!fileStoreDir.exists()) {
            fileStoreDir.mkdirs()
        }

        val fileId = UUID.randomUUID().toString()
        val fullFileName = "$FILE_STORE_DIR/$fileId"

        var ips: InputStream? = null
        var ops: OutputStream? = null
        try {
            ips = BufferedInputStream(inputStream)
            ops = BufferedOutputStream(FileOutputStream(fullFileName))

            val buffer = ByteArray(4096)
            var len: Int
            while (true) {
                len = ips.read(buffer)
                if (len > -1) {
                    ops.write(buffer, 0, len)
                } else {
                    break
                }
            }
            ops.flush()
        } catch (ex: Exception) {
            logger.error("store file failed", ex)
            throw TaskExecuteException(
                errorCode = MIRROR_FILE_SAVE_FAILED.toInt(),
                errorType = ErrorType.USER,
                errorMsg = I18nUtil.getCodeLanMessage(MIRROR_FILE_SAVE_FAILED)
            )
        } finally {
            closeQuietily(ips)
            closeQuietily(ops)
        }
        return fileId
    }

    private fun closeQuietily(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ex: Exception) {
            // ignore
        }
    }

    fun deleteFile(fileName: String) {
        File(fileName).delete()
    }
}
