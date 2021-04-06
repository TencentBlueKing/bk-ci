package com.tencent.bkrepo.common.api.util

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import java.io.InputStream

object DecompressUtils {
    private const val BUFFER_SIZE = 2048

    /**
     * 获取压缩流中的[fileName]文件内容
     */
    fun getContent(archiveInputStream: ArchiveInputStream, fileName: String): String {
        var zipEntry: ArchiveEntry
        archiveInputStream.use { it ->
            while (archiveInputStream.nextEntry.also { zipEntry = it } != null) {
                if ((!zipEntry.isDirectory) && zipEntry.name.split("/").last() == fileName) {
                    return streamToString(it)
                }
            }
        }
        throw NotFoundException(CommonMessageCode.RESOURCE_NOT_FOUND, "Can not find $fileName")
    }

    private fun streamToString(inputStream: InputStream): String {
        val stringBuilder = StringBuffer("")
        var length: Int
        val bytes = ByteArray(BUFFER_SIZE)
        while ((inputStream.read(bytes).also { length = it }) != -1) {
            stringBuilder.append(String(bytes, 0, length))
        }
        return stringBuilder.toString()
    }
}