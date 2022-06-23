package com.tencent.bkrepo.common.bksync

import com.tencent.bkrepo.common.api.collection.MutablePair
import java.io.InputStream

/**
 * 使用缓冲区的滑动窗口
 * 支持移动一个字节和移动整个窗口
 * */
class BufferedSlidingWindow(
    val windowSize: Int,
    private val bufferSize: Int,
    private val srcInput: InputStream,
    private val size: Long
) {
    /**
     * 窗口数据
     * */
    var windowData = ByteArray(windowSize)

    /**
     * 当前窗口在源数据中的偏移量,指向窗口第一个字节的下标
     * */
    private var offset = 0L

    /**
     * buffer中的head指针
     * */
    private var head = 0

    /**
     * buffer中的tail指针
     * */
    private var tail = 0

    /**
     * 缓冲区，窗口在缓冲区中移动
     * */
    private var buffer = ByteArray(bufferSize)

    /**
     * 缓冲区最大可读取长度
     * */
    private var maxRead = 0

    /**
     * move返回的容器缓存对象，仅仅用作参数容器，未避免查询时创建大量Key对象
     * */
    private var moveResult = MutablePair<Byte, Byte>(-1, -1)

    init {
        assert(bufferSize > windowSize) { "window bufferSize[$bufferSize] must greater than windowSize[$windowSize]." }
    }

    /**
     * 移动到下一个字节
     * @return first: next head byte,
     * second: next tail byte,
     * third: true move success else false
     * */
    fun moveToNextByte(): MutablePair<Byte, Byte> {
        // 如果tail指针移动到buffer可读的最后一位，则更新buffer
        if (tail == maxRead - 1) {
            updateBufferByNextByte()
            head = 0
            tail = windowSize - 1
        }
        moveResult.setValue(buffer[head++], buffer[++tail])
        return moveResult
    }

    /**
     * 移动到下一个窗口
     * @return true move success else false
     * */
    fun moveToNext() {
        if (maxRead == 0) {
            // 首次加载
            maxRead = srcInput.read(buffer)
            head = 0
            tail = windowSize - 1
            return
        }
        // buffer中可读数据不足一个窗口时
        if (maxRead - tail - 1 < windowSize) {
            updateBufferByNext()
            head = 0
            // 可读数据小于窗口大小，则设置tail指针为最大可读位置
            tail = if (maxRead < windowSize) maxRead - 1 else windowSize - 1
        } else {
            head += windowSize
            tail += windowSize
        }
    }

    /**
     * 是否还有数据
     * */
    fun hasNext(): Boolean {
        return tailPos() < size - 1
    }

    /**
     * 获取窗口的头部位置
     * */
    fun headPos(): Long {
        return offset + head
    }

    /**
     * 获取窗口的尾部位置
     * */
    fun tailPos(): Long {
        return offset + tail
    }

    /**
     * 移动一个字节的情况下更新buffer。
     * 1. 移动当前窗口到buffer头部。
     * 2. 读取数据到buffer中。
     * 3. 设置最大可读取长度。
     * 4. 更新offset到head（head对于源数据偏移量没变，所以直接将offset移动到head位置即可）。
     * */
    private fun updateBufferByNextByte() {
        // 将余下窗口数据移动到buffer头部
        buffer.copyInto(buffer, 0, head, tail + 1)
        val pos = windowSize
        val read = srcInput.read(buffer, pos, bufferSize - pos)
        // 至少需要读取一个字节
        if (read < 1) {
            throw IllegalStateException("No more data.")
        }
        maxRead = pos + read
        offset += head
    }

    /**
     * 移动一个窗口的情况下更新buffer。
     * 1. 将当前buffer剩余数据移动到buffer头部。
     * 2. 读取数据到buffer。
     * 3. 设置最大可读取长度
     * 4. 更新offset为tail+1（因为移动窗口会更新head为下一个字节，所以这里需要+1）
     * */
    private fun updateBufferByNext() {
        val lastLen = maxRead - tail - 1
        if (lastLen > 0) {
            val start = tail + 1
            buffer.copyInto(buffer, 0, start, start + lastLen)
        }
        val read = srcInput.read(buffer, lastLen, bufferSize - lastLen)
        maxRead = if (read == -1) {
            lastLen
        } else {
            read + lastLen
        }
        offset += tail + 1
    }

    /**
     * 获取当前窗口数据
     * */
    fun content(): ByteArray {
        if (tail - head < windowSize) {
            // 不足一个窗口大小，使用新的array
            return buffer.copyOfRange(head, tail + 1)
        }
        windowData = buffer.copyInto(windowData, 0, head, tail + 1)
        return windowData
    }
}
