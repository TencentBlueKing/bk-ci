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

package com.tencent.bkrepo.rpm.util.xStream

import com.tencent.bkrepo.rpm.pojo.IndexType
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmMetadata
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmEntry
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmFile
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackageChangeLog
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackageFileList
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmPackage
import com.tencent.bkrepo.rpm.util.xStream.repomd.Repomd
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.XStreamException
import org.slf4j.LoggerFactory

object XStreamUtil {
    private val logger = LoggerFactory.getLogger(XStreamUtil::class.java)

    fun xmlToObject(xml: String): Any {
        val xStream = XStream()
        XStream.setupDefaultSecurity(xStream)
        xStream.autodetectAnnotations(true)
        xStream.alias("metadata", RpmMetadata::class.java)
        xStream.alias("repomd", Repomd::class.java)
        xStream.allowTypes(
            arrayOf(
                RpmMetadata::class.java,
                RpmEntry::class.java,
                RpmFile::class.java
            )
        )
        return xStream.fromXML(xml)
    }

    fun checkMarkFile(markFileContent: ByteArray, indexType: IndexType): Boolean {
        return try {
            val xStream = XStream()
            XStream.setupDefaultSecurity(xStream)
            xStream.autodetectAnnotations(true)
            val clazz = when (indexType) {
                IndexType.FILELISTS -> RpmPackageFileList::class.java
                IndexType.OTHER -> RpmPackageChangeLog::class.java
                IndexType.PRIMARY -> RpmPackage::class.java
            }
            xStream.alias("package", clazz)
            xStream.allowTypes(
                arrayOf(
                    clazz,
                    RpmEntry::class.java,
                    RpmFile::class.java
                )
            )
            xStream.fromXML(markFileContent.inputStream())
            true
        } catch (e: XStreamException) {
            logger.warn("checkMarkFile error: ${e.message}")
            false
        }
    }
}
