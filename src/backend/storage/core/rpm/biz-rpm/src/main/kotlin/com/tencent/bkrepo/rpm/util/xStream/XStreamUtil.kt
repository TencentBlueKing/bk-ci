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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.rpm.util.xStream

import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmEntry
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmFile
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmMetadata
import com.tencent.bkrepo.rpm.util.xStream.pojo.RpmXmlMetadata
import com.tencent.bkrepo.rpm.util.xStream.repomd.Repomd
import com.thoughtworks.xstream.XStream
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.Writer

object XStreamUtil {
    /**
     * @param Any 转 xml 字符串
     */
    fun Any.objectToXml(): String {
        val xStream = XStream()
        val outputStream = ByteArrayOutputStream()
        val writer: Writer = OutputStreamWriter(outputStream, "UTF-8")
        // 如果Any is RpmXmlMetadata 添加xml声明
        if (this is RpmXmlMetadata) { writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n") }
        xStream.autodetectAnnotations(true)
        xStream.toXML(this, writer)
        return String(outputStream.toByteArray())
    }

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
}
