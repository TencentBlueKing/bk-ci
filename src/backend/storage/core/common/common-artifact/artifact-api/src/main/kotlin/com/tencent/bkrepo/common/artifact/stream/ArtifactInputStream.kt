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

package com.tencent.bkrepo.common.artifact.stream

import java.io.InputStream

/**
 * 构件输入流
 * @param delegate 实际操作的输入流
 * @param range 输入流在文件中的范围
 */
class ArtifactInputStream(
    delegate: InputStream,
    val range: Range
) : DelegateInputStream(delegate) {

    private val listenerList = mutableListOf<StreamReadListener>()

    override fun read(): Int {
        return super.read().apply {
            if (this >= 0) {
                listenerList.forEach { it.data(this) }
            } else {
                notifyFinish()
            }
        }
    }

    override fun read(byteArray: ByteArray): Int {
        return super.read(byteArray).apply {
            if (this >= 0) {
                listenerList.forEach { it.data(byteArray, this) }
            } else {
                notifyFinish()
            }
        }
    }

    override fun read(byteArray: ByteArray, off: Int, len: Int): Int {
        return super.read(byteArray, off, len).apply {
            if (this >= 0) {
                listenerList.forEach { it.data(byteArray, this) }
            } else {
                notifyFinish()
            }
        }
    }

    override fun close() {
        super.close()
        listenerList.forEach { it.close() }
    }

    /**
     * 添加流读取监听器[listener]
     */
    fun addListener(listener: StreamReadListener) {
        if (range.isPartialContent()) {
            listener.close()
            throw IllegalArgumentException("ArtifactInputStream is partial content, maybe cause data inconsistent")
        }
        listenerList.add(listener)
    }

    /**
     * 通知各个listener流关闭
     */
    private fun notifyFinish() {
        listenerList.forEach { it.finish() }
    }
}
