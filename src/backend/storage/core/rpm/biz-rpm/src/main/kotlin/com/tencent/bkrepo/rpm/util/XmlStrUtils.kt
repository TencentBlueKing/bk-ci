/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.rpm.util

import com.tencent.bkrepo.common.api.constant.StringPool.DASH
import com.tencent.bkrepo.rpm.pojo.Index
import com.tencent.bkrepo.rpm.pojo.IndexType
import com.tencent.bkrepo.rpm.pojo.RepoDataPojo
import com.tencent.bkrepo.rpm.pojo.XmlIndex
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmXmlMetadata
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.regex.Pattern

object XmlStrUtils {
    private val logger = LoggerFactory.getLogger(XmlStrUtils::class.java)

    // RpmMetadata序列化成xml中 metadata 结束字符串
    private const val METADATA_SUFFIX = "</metadata>"

    /**
     * 在原有xml索引文件开头写入新的内容
     * 返回更新后xml
     */
    fun insertPackageIndex(randomAccessFile: RandomAccessFile, markContent: ByteArray): Int {
        val insertIndex = randomAccessFile.length() - METADATA_SUFFIX.length
        updatePackageXml(randomAccessFile, insertIndex, 0, markContent)
        return 1
    }

    /**
     * 针对重复节点则替换相应数据
     */
    fun updatePackageIndex(
        randomAccessFile: RandomAccessFile,
        indexType: IndexType,
        locationStr: String,
        markContent: ByteArray
    ): Int {
        logger.info(
            "updatePackageIndex: indexType: $indexType, " +
                    "locationStr: ${locationStr.replace("\n", "")}"
        )
        val stopWatch = StopWatch("updatePackageIndex")

        stopWatch.start("findPackageIndex")
        val xmlIndex = findPackageIndex(randomAccessFile, getPackagePrefix(indexType), locationStr, getPackageSuffix())
        stopWatch.stop()

        val changeCount = if (xmlIndex == null) {
            logger.warn("find package index failed, skip delete index")
            stopWatch.start("insertPackageXml")
            insertPackageXml(randomAccessFile, markContent)
            stopWatch.stop()
            1
        } else {
            stopWatch.start("updatePackageIndex")
            val cleanLength = xmlIndex.suffixEndIndex - xmlIndex.prefixIndex
            updatePackageXml(randomAccessFile, xmlIndex.prefixIndex, cleanLength, markContent)
            stopWatch.stop()
            0
        }
        if (logger.isDebugEnabled) {
            logger.debug("updatePackageIndexStat: $stopWatch")
        }
        return changeCount
    }

    /**
     * 删除包对应的索引
     * [indexType] 索引类型
     * [locationStr] rpm构件相对repodata的目录
     */
    fun deletePackageIndex(randomAccessFile: RandomAccessFile, indexType: IndexType, locationStr: String): Int {
        logger.info(
            "deletePackageIndex: indexType: $indexType, " +
                    "locationStr: ${locationStr.replace("\n", "")}"
        )

        val stopWatch = StopWatch("deletePackageIndex")
        stopWatch.start("findIndex")
        val xmlIndex = findPackageIndex(randomAccessFile, getPackagePrefix(indexType), locationStr, getPackageSuffix())
        stopWatch.stop()

        if (xmlIndex == null) {
            logger.warn("findPackageIndex failed, skip delete index")
            return 0
        } else {
            stopWatch.start("deleteContent")
            deletePackageXml(randomAccessFile, xmlIndex)
            stopWatch.stop()
        }
        if (logger.isDebugEnabled) {
            logger.debug("deletePackageIndexStat: $stopWatch")
        }
        return -1
    }

    /**
     * 按照仓库设置的repodata深度 分割请求参数
     */
    fun resolveRepodataUri(uri: String, depth: Int): RepoDataPojo {
        val uriList = uri.removePrefix("/").removeSuffix("/").split("/")
        val repodataPath = StringBuilder("/")
        for (i in 0 until depth) {
            repodataPath.append(uriList[i]).append("/")
        }
        val artifactRelativePath = uri.removePrefix(repodataPath.toString())
        return RepoDataPojo(repodataPath.toString(), artifactRelativePath)
    }

    /**
     * 在文件名前加上sha1值。
     */
    fun getGroupNodeFullPath(uri: String, fileSha1: String): String {
        val uriList = uri.removePrefix("/").split("/")
        val filename = "$fileSha1$DASH${uriList.last()}"
        val stringBuilder = StringBuilder("/")
        val size = uriList.size
        for (i in 0 until size - 1) {
            stringBuilder.append(uriList[i]).append("/")
        }
        stringBuilder.append(filename)
        return stringBuilder.toString()
    }

    fun indexOf(randomAccessFile: RandomAccessFile, str: String): Long {
        val bufferSize = str.toByteArray().size + 1
        val buffer = ByteArray(bufferSize)
        var mark: Int
        // 保存上一次读取的内容
        var tempStr = ""
        var index = 0L
        randomAccessFile.seek(0L)
        while (randomAccessFile.read(buffer).also { mark = it } > 0) {
            val content = String(buffer, 0, mark)
            val insideIndex = (tempStr + content).indexOf(str)
            if (insideIndex >= 0) {
                index = index + insideIndex - bufferSize
                return index
            } else {
                tempStr = content
                index += buffer.size
            }
        }
        return -1L
    }

    /**
     * 插入rpm包索引
     */
    private fun insertPackageXml(randomAccessFile: RandomAccessFile, newContent: ByteArray) {
        val insertIndex = randomAccessFile.length() - METADATA_SUFFIX.length
        updatePackageXml(randomAccessFile, insertIndex, 0, newContent)
    }

    /**
     * 删除rpm包索引
     */
    fun deletePackageXml(randomAccessFile: RandomAccessFile, xmlIndex: XmlIndex) {
        updatePackageXml(
            randomAccessFile,
            xmlIndex.prefixIndex,
            xmlIndex.suffixEndIndex - xmlIndex.prefixIndex,
            "".toByteArray()
        )
    }

    /**
     * [File] 需要查找的文件
     * [XmlIndex] 封装结果
     * [prefixStr] rpm 包索引package节点的开始字符串
     * [locationStr] 可以唯一定位一个 rpm 包索引package节点位置的 字符串
     * [suffixStr] rpm 包索引package节点的结束字符串
     */
    fun findPackageIndex(
        randomAccessFile: RandomAccessFile,
        prefixStr: String,
        locationStr: String,
        suffixStr: String
    ): XmlIndex? {
        if (logger.isDebugEnabled) {
            logger.debug("findPackageIndex: [$prefixStr|$locationStr|$suffixStr]")
        }
        var prefixIndex: Long = -1L
        var locationIndex: Long = -1L
        var suffixIndex: Long = -1L

        val buffer = ByteArray(locationStr.toByteArray().size)
        var len: Int
        var index: Long = 0
        // 保存上一次读取的内容
        var tempStr = ""
        randomAccessFile.seek(0L)
        loop@ while (randomAccessFile.read(buffer).also { len = it } > 0) {
            val content = String(buffer, 0, len)
            if (locationIndex < 0) {
                val prefix = (tempStr + content).searchContent(index, prefixIndex, prefixStr, buffer.size)
                val location = (tempStr + content).searchContent(index, locationIndex, locationStr, buffer.size)
                if (location.isFound) {
                    locationIndex = location.index
                    val suffix = (tempStr + content).searchContent(index, suffixIndex, suffixStr, buffer.size)
                    if (suffix.index > locationIndex) {
                        suffixIndex = suffix.index
                        break@loop
                    }
                }
                if (!location.isFound && prefix.isFound) {
                    prefixIndex = prefix.index
                }
                if (location.isFound && prefix.isFound && prefix.index < location.index) {
                    prefixIndex = prefix.index
                }
            }
            if (locationIndex > 0) {
                val suffix = (tempStr + content).searchContent(index, suffixIndex, suffixStr, buffer.size)
                if (suffix.index > locationIndex) {
                    suffixIndex = suffix.index
                    break@loop
                }
            }
            index += buffer.size
            tempStr = content
        }

        return if (prefixIndex <= 0L || locationIndex <= 0L || suffixIndex <= 0L) {
            logger.warn(
                "findPackageIndex failed, locationStr: $locationStr, " +
                        "prefixIndex: $prefixIndex, " +
                        "locationIndex: $locationIndex, suffixIndex: $suffixIndex"
            )
            null
        } else {
            val suffixEndIndex = suffixIndex + suffixStr.length
            if (logger.isDebugEnabled) {
                logger.debug("findPackageIndex result: [$prefixIndex|$locationIndex|$suffixIndex|$suffixEndIndex]")
            }
            XmlIndex(prefixIndex, locationIndex, suffixIndex, suffixEndIndex)
        }
    }

    /**
     * 查找内容
     * [index] 查找开始位置 0
     * [returnIndex] 需要查找的文件 xml节点在文件中的开始位置
     * [targetStr] 需要查找的字符串
     * [bufferSize] 缓存大小
     */
    private fun String.searchContent(index: Long, returnIndex: Long, targetStr: String, bufferSize: Int): Index {
        val location = this.indexOf(targetStr)
        return if (location >= 0) {
            val prefixStr = this.split(targetStr).first()
            val size = prefixStr.toByteArray().size
            Index(index + size - bufferSize, true)
        } else {
            Index(bufferSize + (if (returnIndex.toInt() == -1) 0 else returnIndex), false)
        }
    }

    /**
     * 更新索引文件中 package 数量
     */
    fun updatePackageCount(
        randomAccessFile: RandomAccessFile,
        indexType: IndexType,
        changCount: Int,
        calculatePackage: Boolean
    ) {
        logger.info(
            "updatePackageCount, indexType: $indexType, " +
                    "changCount: $changCount, " +
                    "calculatePackage: $calculatePackage"
        )
        val currentCount = resolvePackageCount(randomAccessFile, indexType)
        logger.info("currentCount: $currentCount")

        val packageCount = if (calculatePackage) {
            calculatePackageCount(randomAccessFile, indexType)
        } else {
            currentCount + changCount
        }
        logger.info("packageCount: $packageCount")
        if (packageCount == currentCount) {
            logger.info("package count not change")
            return
        }

        val packageCountIndex = when (indexType) {
            IndexType.PRIMARY ->
                """<?xml version="1.0" encoding="UTF-8" ?>
<metadata xmlns="http://linux.duke.edu/metadata/common" xmlns:rpm="http://linux.duke.edu/metadata/rpm" packages=""""
                    .length
            IndexType.FILELISTS ->
                """<?xml version="1.0" encoding="UTF-8" ?>
<metadata xmlns="http://linux.duke.edu/metadata/filelists" packages="""".length
            IndexType.OTHERS ->
                """<?xml version="1.0" encoding="UTF-8" ?>
<metadata xmlns="http://linux.duke.edu/metadata/other" packages="""".length
        }
        updatePackageXml(
            randomAccessFile,
            packageCountIndex.toLong(),
            currentCount.toString().length.toLong(),
            packageCount.toString().toByteArray()
        )
    }

    /**
     * 更新索引
     * [randomAccessFile] 文件内容
     * [updateIndex] 更新位置
     * [cleanLength] 清理长度
     * [newContent] 新内容
     */
    fun updatePackageXml(
        randomAccessFile: RandomAccessFile,
        updateIndex: Long,
        cleanLength: Long,
        newContent: ByteArray
    ) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "updatePackageXml: updateIndex: $updateIndex, " +
                        "cleanLength: $cleanLength, " +
                        "newContentSize: ${newContent.size}"
            )
        }

        randomAccessFile.seek(updateIndex + cleanLength)
        // 如果已经是文件末尾，直接在更新位置追加内容
        if (randomAccessFile.filePointer == randomAccessFile.length()) {
            if (newContent.isNotEmpty()) {
                randomAccessFile.seek(updateIndex)
                randomAccessFile.write(newContent)
                randomAccessFile.setLength(randomAccessFile.filePointer)
            }
            return
        }

        var bufferFile: File? = null
        try {
            var memoryBuffer: ByteArrayOutputStream? = null
            // 插入点离文件末尾小于2M时使用内存缓存
            val outputStream = if (randomAccessFile.length() - randomAccessFile.filePointer > 2 * 1024 * 1024) {
                bufferFile = createTempFile("updatePackageXml_", ".buffer")
                if (logger.isDebugEnabled) {
                    logger.debug("create buffer file: ${bufferFile.absolutePath}")
                }
                bufferFile.outputStream()
            } else {
                memoryBuffer = ByteArrayOutputStream()
                memoryBuffer
            }
            // 缓存文件后半部分
            outputStream.use { stream ->
                val buffer = newBuffer()
                var len: Int
                while (randomAccessFile.read(buffer).also { len = it } > 0) {
                    stream.write(buffer, 0, len)
                }
                stream.flush()
            }

            randomAccessFile.seek(updateIndex)
            if (newContent.isNotEmpty()) {
                randomAccessFile.write(newContent)
            }

            if (memoryBuffer != null) {
                randomAccessFile.write(memoryBuffer.toByteArray())
            } else {
                bufferFile!!.inputStream().use { inputStream ->
                    val buffer = newBuffer()
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } > 0) {
                        randomAccessFile.write(buffer, 0, len)
                    }
                }
            }
            randomAccessFile.setLength(randomAccessFile.filePointer)
        } finally {
            if (bufferFile != null && bufferFile.exists()) {
                bufferFile.delete()
                logger.debug("buffer file(${bufferFile.absolutePath}) deleted")
            }
        }
    }

    private fun getPackagePrefix(indexType: IndexType): String {
        return when (indexType) {
            IndexType.OTHERS, IndexType.FILELISTS -> {
                "  <package pkgid"
            }
            IndexType.PRIMARY -> {
                "  <package type=\"rpm\">"
            }
        }
    }

    private fun getPackageSuffix(): String {
        return "</package>\n"
    }

    /**
     * 解析索引文件中的 package 数量
     */
    private fun calculatePackageCount(randomAccessFile: RandomAccessFile, indexType: IndexType): Int {
        val markStr = when (indexType) {
            IndexType.PRIMARY -> """<package type="rpm">"""
            IndexType.FILELISTS, IndexType.OTHERS -> "<package pkgid="
        }
        randomAccessFile.seek(0L)
        var line: String?
        var count = 0
        loop@ while (randomAccessFile.readLine().also { line = it } != null) {
            if (line!!.contains(markStr)) {
                ++count
            }
        }
        return count
    }

    /**
     * 解析索引文件中的 package 值(metadata -> packages)
     */
    fun resolvePackageCount(randomAccessFile: RandomAccessFile, indexType: IndexType): Int {
        val regex = when (indexType) {
            IndexType.PRIMARY -> """^<metadata xmlns="http://linux.duke.edu/metadata/common" xmlns:rpm=
                |"http://linux.duke.edu/metadata/rpm" packages="(\d+)">$""".trimMargin()
            IndexType.FILELISTS -> """^<metadata xmlns="http://linux.duke.edu/metadata/filelists" packages="(\d+)">$"""
            IndexType.OTHERS -> """^<metadata xmlns="http://linux.duke.edu/metadata/other" packages="(\d+)">$"""
        }

        randomAccessFile.seek(0L)
        var line: String?
        var lineNum = 0
        while (randomAccessFile.readLine().also { line = it } != null) {
            val matcher = Pattern.compile(regex).matcher(line!!)
            if (matcher.find()) {
                return matcher.group(1).toInt()
            }
            // 应该在索引文件的第二行
            if (lineNum++ > 50) {
                throw RuntimeException("resolve package count from file failed")
            }
        }
        throw RuntimeException("resolve package count from file failed")
    }

    /**
     * RpmXmlMetadata 序列化为字符串
     */
    fun toMarkFileXml(rpmXmlMetadata: RpmXmlMetadata, indexType: IndexType): String {
        val prefix = when (indexType) {
            IndexType.OTHERS -> "<metadata xmlns=\"http://linux.duke.edu/metadata/other\" packages=\"1\">\n"
            IndexType.PRIMARY ->
                "<metadata xmlns=\"http://linux.duke.edu/metadata/common\" " +
                        "xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\" packages=\"1\">\n"
            IndexType.FILELISTS -> "<metadata xmlns=\"http://linux.duke.edu/metadata/filelists\" packages=\"1\">\n"
        }
        return rpmXmlMetadata.toXml().removePrefix(prefix).removeSuffix(METADATA_SUFFIX)
    }

    private fun newBuffer(): ByteArray {
        return ByteArray(1024 * 1024)
    }
}
