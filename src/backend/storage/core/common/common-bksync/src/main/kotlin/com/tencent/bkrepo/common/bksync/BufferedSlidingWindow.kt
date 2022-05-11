package com.tencent.bkrepo.common.bksync

import com.tencent.bkrepo.common.api.collection.MutablePair
import java.io.RandomAccessFile

/**
 * 使用缓冲区的滑动窗口
 * 支持移动一个字节和移动整个窗口
 * */
class BufferedSlidingWindow(private val windowSize: Int, val bufferSize: Int, private val raf: RandomAccessFile) {

    private val fileLength = raf.length()

    /**
     * 文件最后index
     * */
    private val lastFileIndex = fileLength - 1

    /**
     * 头缓冲区
     * */
    var headBuffer: Buffer = Buffer(data = ByteArray(bufferSize))

    /**
     * 尾部缓冲区
     * */
    var tailBuffer: Buffer = Buffer(data = ByteArray(bufferSize))

    /**
     * 窗口数据
     * */
    var windowData = ByteArray(windowSize)

    /**
     * move返回的容器缓存对象，仅仅用作参数容器，未避免查询时创建大量Key对象
     * */
    private var moveResult = MutablePair<Byte, Byte>(-1, -1)

    init {
        assert(bufferSize > 1) { "window bufferSize[$bufferSize] must more than 1" }
        assert(windowSize > bufferSize) { "window bufferSize[$bufferSize] must less than windowSize[$windowSize]." }
    }

    /**
     * 移动到下一个字节
     * @return first: next head byte,
     * second: next tail byte,
     * third: true move success else false
     * */
    fun moveToNextByte(): MutablePair<Byte, Byte> {
        moveResult.setValue(nextHead(), nextTail())
        return moveResult
    }

    /**
     * 移动到下一个窗口
     * @return true move success else false
     * */
    fun moveToNext() {
        val curTailOffset = tailPos()
        headBuffer.start = curTailOffset
        headBuffer.offset = 0
        /*
        * 1. 后面的数据大于窗口大小，加载buffer
        * 2. 后面的数据小于或者等于窗口大小，则无需buffer
        * */
        if (curTailOffset + windowSize < lastFileIndex) {
            // 初始化buffer
            raf.seek(headBuffer.start)
            raf.read(headBuffer.data)

            tailBuffer.start = curTailOffset + windowSize
            tailBuffer.offset = 0
            raf.seek(tailBuffer.start)
            raf.read(tailBuffer.data)
        } else {
            tailBuffer.start = fileLength
            tailBuffer.offset = 0
        }
    }

    /**
     * 是否还有数据
     * */
    fun hasNext(): Boolean {
        return tailPos() < fileLength
    }

    /**
     * 获取窗口的头部字节
     * */
    fun headPos(): Long {
        return headBuffer.start + headBuffer.offset
    }

    /**
     * 获取窗口的尾部字节
     * */
    fun tailPos(): Long {
        return tailBuffer.start + tailBuffer.offset
    }

    /**
     * 下个头部字节
     * */
    private fun nextHead(): Byte {
        if (headBuffer.isLastIndex()) {
            updateBuffer(headBuffer)
        }
        return headBuffer.next()
    }

    /**
     * 下个尾部字节
     * */
    private fun nextTail(): Byte {
        if (tailBuffer.isLastIndex()) {
            updateBuffer(tailBuffer)
        }
        return tailBuffer.next()
    }

    /**
     * 更新为下个buffer的内容
     * @param buffer 待更新的buffer
     * */
    private fun updateBuffer(buffer: Buffer) {
        var bufferData = buffer.data
        val start = buffer.start
        val offset = buffer.offset
        bufferData[0] = bufferData[offset]
        val curTailOffset = tailPos()
        if (curTailOffset + bufferSize > lastFileIndex) {
            val tempByte = bufferData[0]
            val bufferLen = lastFileIndex - curTailOffset + 1
            buffer.data = ByteArray(bufferLen.toInt())
            bufferData = buffer.data
            bufferData[0] = tempByte
        }
        val curOffset = start + offset
        val bytes = bufferData.size - 1
        if (bytes > 0) {
            raf.seek(curOffset + 1)
            raf.read(bufferData, 1, bytes)
        }
        buffer.start = curOffset
        buffer.offset = 0
    }

    /**
     * 获取当前窗口数据
     * */
    fun content(): ByteArray {
        val start = headPos()
        val end = tailPos()
        val len = end - start
        if (len == 0L) {
            return ByteArray(0)
        }
        if (len < windowData.size) {
            windowData = ByteArray(len.toInt())
        }
        raf.seek(start)
        raf.read(windowData)
        return windowData
    }

    /**
     * 窗口缓冲区
     * */
    data class Buffer(var start: Long = 0, var offset: Int = 0, var data: ByteArray) {
        /**
         * 下一个字节
         * */
        fun next(): Byte {
            return data[offset++]
        }

        /**
         * 是否为最后一个字节
         * */
        fun isLastIndex(): Boolean {
            return offset == data.lastIndex
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Buffer

            if (start != other.start) return false
            if (offset != other.offset) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = start.hashCode()
            result = 31 * result + offset
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
