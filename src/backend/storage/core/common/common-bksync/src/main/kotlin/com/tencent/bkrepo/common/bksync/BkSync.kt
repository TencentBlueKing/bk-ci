package com.tencent.bkrepo.common.bksync

import com.google.common.hash.HashCode
import com.google.common.primitives.Ints
import com.tencent.bkrepo.common.bksync.checksum.Adler32RollingHash
import com.tencent.bkrepo.common.bksync.checksum.Checksum
import com.tencent.bkrepo.common.api.util.StreamUtils.readFully
import com.tencent.bkrepo.common.bksync.transfer.exception.InterruptedRollingException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.Adler32
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import kotlin.math.ceil

/**
 * 基于rsync算法实现的增量上传/下载工具
 * */
@Suppress("UnstableApiUsage")
class BkSync(val blockSize: Int = DEFAULT_BLOCK_SIZE, var windowBufferSize: Int = DEFAULT_WINDOW_BUFFER_SIZE) {
    private val logger = LoggerFactory.getLogger(BkSync::class.java)
    private val md5Digest: MessageDigest = MessageDigest.getInstance("MD5")
    private val sha256Digest: MessageDigest = MessageDigest.getInstance("SHA-256")
    private val adler32RollingHash = Adler32RollingHash(blockSize)

    // 是否需要计算merge后文件的sha256
    var calculateSha256 = false

    // 是否需要计算merge后文件的md5
    var calculateMd5 = false

    // 用于计算校验和使用
    private val arrayOutputStream = ByteArrayOutputStream()
    private val writableByteChannel = Channels.newChannel(arrayOutputStream)

    /**
     * 对文件进行分块和输出校验和信息
     * @param file 需要处理的文件
     * @param checksumOutput 校验和输出流
     * */
    fun checksum(file: File, checksumOutput: OutputStream) {
        checksum(file.inputStream(), checksumOutput)
    }

    /**
     * 计算强弱哈希,生成checksum stream
     * 4 bytes rolling checksum - block 0
     * 16 bytes hash value

     * 4 bytes rolling checksum - block 1
     * 16 bytes hash value

     * ...

     * 4 bytes rolling checksum - block N
     * 16 bytes hash value
     * @param inputStream 需要被处理的输入流
     * @param checksumOutput 校验和输出流
     * */
    fun checksum(inputStream: InputStream, checksumOutput: OutputStream) {
        val block = ByteArray(blockSize)
        val adler32 = Adler32()
        var bytes = readFully(inputStream, block)
        while (bytes > 0) {
            adler32.update(block, 0, bytes)
            val rollingHash = adler32.value
            val rollingHashData = Ints.toByteArray(rollingHash.toInt())
            md5Digest.update(block, 0, bytes)
            val md5Data = md5Digest.digest()
            checksumOutput.write(rollingHashData)
            checksumOutput.write(md5Data)
            bytes = readFully(inputStream, block)
            adler32.reset()
            md5Digest.reset()
        }
    }

    /**
     * 使用rsync算法，检测文件差异，生成delta数据
     * @param file 需要上传的文件
     * @param checksumStream 远端checksum
     * @param deltaOutput delta output stream
     * @param reuseThreshold 重复率阈值
     * */
    fun diff(file: File, checksumStream: InputStream, deltaOutput: OutputStream, reuseThreshold: Float): DiffResult {
        // 使用滑动窗口检测,找到与远端相同的部分
        val index = ChecksumIndex(checksumStream)
        val window = BufferedSlidingWindow(blockSize, windowBufferSize, file.inputStream(), file.length())
        val raf = RandomAccessFile(file, READ)
        raf.use {
            return detecting(window, index, deltaOutput, it, reuseThreshold)
        }
    }

    /**
     * 检测文件差异
     * 使用基于rsync的滑动窗口算法，进行检测
     * @param raf 待检测的文件
     * @param index 校验和索引
     * @param outputStream delta output stream
     * @param reuseThreshold 重复率阈值
     * */
    private fun detecting(
        window: BufferedSlidingWindow,
        index: ChecksumIndex,
        outputStream: OutputStream,
        raf: RandomAccessFile,
        reuseThreshold: Float
    ): DiffResult {
        var content: ByteArray
        var deltaStart: Long
        var deltaEnd: Long
        var nextDeltaStart = 0L
        var reuse = 0
        while (window.hasNext()) {
            /*
            * 基于滑动窗口算法，使用固定窗口大小进行检测
            * 1. 判断checksum是否存在
            * 2. 不存在则移动一个字节，滚动哈希，重复1
            * 3. 找到相同块时，记录当前位置。到上一相同块的位置之间为增量数据
            * 4. 移动整个窗口，即块大小。重复1
            * */
            window.moveToNext()
            content = window.content()
            adler32RollingHash.reset()
            adler32RollingHash.update(content)
            val rollingHash = adler32RollingHash.digest()
            var checksum = search(rollingHash.toInt(), index, window)
            if (checksum == null) {
                checksum = try {
                    rolling(window, adler32RollingHash, index, reuse, reuseThreshold)
                } catch (e: InterruptedRollingException) {
                    return DiffResult(reuse, index.total)
                }
            }
            if (checksum != null) {
                deltaStart = nextDeltaStart
                deltaEnd = window.headPos()
                checkAndWriteDelta(deltaStart, deltaEnd, raf, outputStream)
                nextDeltaStart = window.tailPos() + 1
                val seqData = Ints.toByteArray(checksum.seq)
                outputStream.write(seqData)
                reuse++
            }
        }
        // 确定文件末端是否是增量数据
        deltaStart = nextDeltaStart
        deltaEnd = raf.length()
        if (deltaEnd - deltaStart > 0) {
            checkAndWriteDelta(deltaStart, deltaEnd, raf, outputStream)
        }
        val result = DiffResult(reuse, index.total)
        logger.info(result.toString())
        return result
    }

    /**
     * 查找checksum是否在index中存在
     * */
    private fun search(
        rollingHash: Int,
        index: ChecksumIndex,
        slidingWindow: BufferedSlidingWindow
    ): Checksum? {
        if (!index.exist(rollingHash)) {
            return null
        }
        val content = slidingWindow.content()
        return index.get(rollingHash, md5(content))
    }

    /**
     * 检查增量数据，大于0则写入增量数据
     * */
    private fun checkAndWriteDelta(
        deltaStart: Long,
        deltaEnd: Long,
        raf: RandomAccessFile,
        outputStream: OutputStream
    ) {
        val len = deltaEnd - deltaStart
        if (len > Int.MAX_VALUE) {
            throw IllegalStateException("delta data len[$len] exceed ${Int.MAX_VALUE}.")
        }
        if (len > 0) {
            writeDelta(outputStream, len, raf, deltaStart)
        }
    }

    /**
     * 移动一个字节，并进行滚动哈希。
     * 只有在移动到流末尾或者找到相同块时返回
     * @param slidingWindow 滑动窗口
     * @param adler32RollingHash 滚动哈希
     * @param index 校验和索引
     * @param reuse 重复使用的块数量
     * @param reuseThreshold 重复使用率阈值
     * */
    private fun rolling(
        slidingWindow: BufferedSlidingWindow,
        adler32RollingHash: Adler32RollingHash,
        index: ChecksumIndex,
        reuse: Int,
        reuseThreshold: Float
    ): Checksum? {
        val detectingWindowCount = ceil(index.total - (index.total * reuseThreshold - reuse)).toInt()
        var detectingDataCount = slidingWindow.windowSize * detectingWindowCount
        while (slidingWindow.hasNext()) {
            val (remove, enter) = slidingWindow.moveToNextByte()
            adler32RollingHash.rotate(remove, enter)
            val rollingHash = adler32RollingHash.digest()
            val checksum = search(rollingHash.toInt(), index, slidingWindow)
            if (checksum != null) {
                return checksum
            }
            if (--detectingDataCount == 0) {
                logger.info("Even if remaining data can be reused, the reuse rate is still less than the threshold")
                throw InterruptedRollingException()
            }
        }
        return null
    }

    /**
     * 计算md5的值
     * */
    private fun md5(bytes: ByteArray): ByteArray {
        md5Digest.reset()
        md5Digest.update(bytes)
        return md5Digest.digest()
    }

    /**
     * 写入delta数据
     * 格式为：
     * 4 byte block reference
     * 4 byte block reference
     * 4 byte block reference
     * 4 byte block reference
     * 4 byte block reference
     * 4 byte sequence begin (-1)
     * 4 byte sequence length
     * N byte data
     *
     * 4 byte block reference
     * @param outputStream delta output stream
     * @param len 需要写入的增量数据长度
     * @param ras 数据源文件
     * @param deltaStart 数据源起始位置
     * */
    private fun writeDelta(
        outputStream: OutputStream,
        len: Long,
        ras: RandomAccessFile,
        deltaStart: Long
    ) {
        // 写入数据流标志-1
        outputStream.write(BEGIN_FLAG)
        // 写入数据流长度
        val lenData = Ints.toByteArray(len.toInt())
        outputStream.write(lenData)
        // 从deltaStart位置开始，读取指定长度的数据，写入到output
        ras.seek(deltaStart)
        val bufferSize = len.toInt().coerceAtMost(DEFAULT_BUFFER_SIZE)
        var deltaBuffer = ByteArray(bufferSize)
        var bytes = ras.read(deltaBuffer)
        var total = 0L
        while (bytes > 0 && total < len) {
            outputStream.write(deltaBuffer, 0, bytes)
            total += bytes
            if (total == len) {
                return
            }
            if (total + deltaBuffer.size > len) {
                // 剩余需要读取的数据小于buffer大小，调整buffer大小
                deltaBuffer = ByteArray((len - total).toInt())
            }
            bytes = ras.read(deltaBuffer)
        }
    }

    /**
     * 根据delta数据和旧文件合并成新的文件
     * */
    fun merge(baseFile: File, deltaInput: InputStream, newFileChannel: WritableByteChannel): MergeResult {
        val fileBlockChannel = FileBlockChannel(baseFile, baseFile.name)
        fileBlockChannel.use {
            return merge(fileBlockChannel, deltaInput, newFileChannel)
        }
    }

    fun merge(
        blockChannel: BlockChannel,
        deltaInput: InputStream,
        newFileChannel: WritableByteChannel
    ): MergeResult {
        val deltaStream = DeltaInputStream(deltaInput)
        var reuse = 0
        var deltaDataLength = 0L
        var bytes = deltaStream.moveToNext()
        var startSeq = -1
        var endSeq = -1
        while (bytes > 0) {
            if (deltaStream.isBlockReference()) {
                val blockReference = deltaStream.getBlockReference()
                if (startSeq == -1) {
                    // 记录开始位置
                    startSeq = blockReference
                    endSeq = blockReference
                } else if (blockReference == (endSeq + 1)) {
                    // 连续块，更新结束位置
                    endSeq = blockReference
                } else {
                    // 非连续块，拷贝之前连续块数据，重新开始记录起始块
                    copyOldBlock(newFileChannel, startSeq, endSeq, blockChannel)
                    startSeq = blockReference
                    endSeq = blockReference
                }
                reuse++
            } else if (deltaStream.isDataSequence()) {
                if (startSeq > -1) {
                    // 增量数据前，如果有未冲刷的文件引用块，需要先冲刷
                    copyOldBlock(newFileChannel, startSeq, endSeq, blockChannel)
                    startSeq = -1
                    endSeq = -1
                }
                // 移动到len
                bytes = deltaStream.moveToNext()
                if (bytes == 0) {
                    throw IllegalStateException("Delta stream broken.")
                }
                val len = deltaStream.getDataSequenceLength()
                copyDataSequence(newFileChannel, deltaStream, len)
                deltaDataLength += len
            }
            bytes = deltaStream.moveToNext()
        }
        if (startSeq > -1) {
            // 最后一块数据是块引用
            copyOldBlock(newFileChannel, startSeq, endSeq, blockChannel)
        }
        var md5: String? = null
        var sha256: String? = null
        if (needCalculateDigest()) {
            md5 = HashCode.fromBytes(md5Digest.digest()).toString()
            sha256 = HashCode.fromBytes(sha256Digest.digest()).toString()
        }
        val mergeResult = MergeResult(
            reuse,
            deltaDataLength,
            blockChannel.totalSize(),
            blockChannel.name(),
            blockSize,
            sha256,
            md5
        )
        logger.info(mergeResult.toString())
        return mergeResult
    }

    /**
     * 从源文件拷贝数据
     * @param newFileChannel 新文件输出流
     * @param startSeq 源文件块开始序列号
     * @param endSeq 源文件块结束序列号
     * @param blockChannel 源数据
     * */
    private fun copyOldBlock(
        newFileChannel: WritableByteChannel,
        startSeq: Int,
        endSeq: Int,
        blockChannel: BlockChannel
    ) {
        var currentStartSeq = startSeq
        while (currentStartSeq <= endSeq) {
            val blocks = (endSeq - currentStartSeq).coerceAtMost(MAX_IO_READ_MERGE_BLOCKS)
            val currentEndSeq = currentStartSeq + blocks
            blockChannel.transferTo(currentStartSeq, currentEndSeq, blockSize, newFileChannel)
            if (needCalculateDigest()) {
                blockChannel.transferTo(currentStartSeq, currentEndSeq, blockSize, writableByteChannel)
                val data = arrayOutputStream.toByteArray()
                calculateDigestIfNeed(data, 0, data.size)
                arrayOutputStream.reset()
            }
            currentStartSeq = currentEndSeq + 1
        }
    }

    /**
     * 从流中拷贝数据
     * @param newFileChannel 新文件输出channel
     * @param deltaStream 增量数据流
     * @param len 需要写入的长度
     * */
    private fun copyDataSequence(
        newFileChannel: WritableByteChannel,
        deltaStream: DeltaInputStream,
        len: Int
    ) {
        val bufferSize = len.coerceAtMost(DEFAULT_BUFFER_SIZE)
        var buffer = ByteArray(bufferSize)
        var totalRead = 0
        var remain = len
        while (remain > 0) {
            val bytes = deltaStream.read(buffer)
            if (bytes == -1) {
                break
            }
            newFileChannel.write(ByteBuffer.wrap(buffer, 0, bytes))
            calculateDigestIfNeed(buffer, 0, bytes)
            totalRead += bytes
            remain = len - totalRead
            if (remain in 1 until bufferSize) {
                buffer = ByteArray(remain)
            }
        }
    }

    /**
     * 如果设置了calculateSha256和calculateMd5,则进行计算对应校验和
     * */
    private fun calculateDigestIfNeed(bytes: ByteArray, pos: Int, len: Int) {
        if (calculateSha256) {
            sha256Digest.update(bytes, pos, len)
        }
        if (calculateMd5) {
            md5Digest.update(bytes, pos, len)
        }
    }

    /**
     * 是否需要计算校验和
     * */
    private fun needCalculateDigest(): Boolean {
        return calculateMd5 || calculateSha256
    }

    companion object {
        const val DEFAULT_BLOCK_SIZE = 2048
        const val DEFAULT_WINDOW_BUFFER_SIZE = 16 * 1024 * 1024
        val BEGIN_FLAG: ByteArray = Ints.toByteArray(-1)
        const val READ = "r"
        const val MAX_IO_READ_MERGE_BLOCKS = 1024
    }
}
