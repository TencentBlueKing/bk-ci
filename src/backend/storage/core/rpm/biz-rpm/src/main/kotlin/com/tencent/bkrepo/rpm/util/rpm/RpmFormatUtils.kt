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

package com.tencent.bkrepo.rpm.util.rpm

import com.tencent.bkrepo.rpm.util.redline.model.FormatWithType
import com.tencent.bkrepo.rpm.util.redline.model.RpmFormat
import org.redline_rpm.ReadableChannelWrapper
import org.redline_rpm.header.Header
import org.redline_rpm.header.RpmType
import org.redline_rpm.header.Signature
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

object RpmFormatUtils {
    private val logger = LoggerFactory.getLogger(RpmFormatUtils::class.java)

    fun resolveRpmFormat(channel: ReadableByteChannel): RpmFormat {
        val stopWatch = StopWatch("getRpmFormat")
        val format = FormatWithType()
        val readableChannelWrapper = ReadableChannelWrapper(channel)
        stopWatch.start("headerStartKey")
        val headerStartKey = readableChannelWrapper.start()
        stopWatch.stop()
        stopWatch.start("lead")
        val lead = readableChannelWrapper.start()
        stopWatch.stop()
        stopWatch.start("formatLead")
        format.lead.read(readableChannelWrapper)
        stopWatch.stop()

        stopWatch.start("signature")
        val signature = readableChannelWrapper.start()
        stopWatch.stop()
        stopWatch.start("count")
        var count = format.signature.read(readableChannelWrapper)
        stopWatch.stop()
        val sigEntry = format.signature.getEntry(Signature.SignatureTag.SIGNATURES)
        var expected = if (sigEntry == null) 0 else (ByteBuffer.wrap(sigEntry.values as ByteArray, 8, 4).int / -16)

        val headerStartPos = readableChannelWrapper.finish(headerStartKey) as Int
        format.header.startPos = headerStartPos
        stopWatch.start("headerKey")
        val headerKey = readableChannelWrapper.start()
        stopWatch.stop()
        stopWatch.start("count2")
        count = format.header.read(readableChannelWrapper)
        stopWatch.stop()
        val immutableEntry = format.header.getEntry(Header.HeaderTag.HEADERIMMUTABLE)
        expected = if (immutableEntry == null)
            0 else (ByteBuffer.wrap(immutableEntry.values as ByteArray, 8, 4).int / -16)
        val headerLength = readableChannelWrapper.finish(headerKey) as Int
        format.header.endPos = headerStartPos + headerLength
        if (logger.isDebugEnabled) {
            logger.debug("getRpmFormatStat: $stopWatch")
        }

        return RpmFormat(headerStartPos, headerStartPos + headerLength, format, RpmType.BINARY)
    }
}
