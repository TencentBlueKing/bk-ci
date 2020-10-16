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

package com.tencent.bkrepo.rpm.util.rpm

import com.tencent.bkrepo.rpm.util.redline.model.FormatWithType
import com.tencent.bkrepo.rpm.util.redline.model.RpmFormat
import org.redline_rpm.ReadableChannelWrapper
import org.redline_rpm.header.Header
import org.redline_rpm.header.RpmType
import org.redline_rpm.header.Signature
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

object RpmFormatUtil {

    @Throws(IOException::class)
    fun getRpmFormat(channel: ReadableByteChannel): RpmFormat {
        val format = FormatWithType()
        val readableChannelWrapper = ReadableChannelWrapper(channel)
        val headerStartKey = readableChannelWrapper.start()

        val lead = readableChannelWrapper.start()
        format.lead.read(readableChannelWrapper)

        val signature = readableChannelWrapper.start()
        var count = format.signature.read(readableChannelWrapper)
        val sigEntry = format.signature.getEntry(Signature.SignatureTag.SIGNATURES)
        var expected = if (sigEntry == null) 0 else (ByteBuffer.wrap(sigEntry.values as ByteArray, 8, 4).int / -16)

        val headerStartPos = readableChannelWrapper.finish(headerStartKey) as Int
        format.header.startPos = headerStartPos
        val headerKey = readableChannelWrapper.start()
        count = format.header.read(readableChannelWrapper)
        val immutableEntry = format.header.getEntry(Header.HeaderTag.HEADERIMMUTABLE)
        expected = if (immutableEntry == null) 0 else (ByteBuffer.wrap(immutableEntry.values as ByteArray, 8, 4).int / -16)
        val headerLength = readableChannelWrapper.finish(headerKey) as Int
        format.header.endPos = headerStartPos + headerLength

        return RpmFormat(headerStartPos, headerStartPos + headerLength, format, RpmType.BINARY)
    }
}
