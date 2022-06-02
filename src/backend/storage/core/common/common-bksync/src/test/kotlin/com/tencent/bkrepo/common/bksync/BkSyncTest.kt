package com.tencent.bkrepo.common.bksync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BkSyncTest {

    private val bkSync = BkSync(4, 2)
    private val oldData = byteArrayOf(
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12
    )
    private val oldFile = createTempFile()
    private val mergeFile = createTempFile()
    private val checksumStream: InputStream
    private val newFile = createTempFile()
    private val deltaOutput = ByteArrayOutputStream()

    init {
        oldFile.writeBytes(oldData)
        val input = oldFile.inputStream()
        val output = ByteArrayOutputStream()
        bkSync.checksum(input, output)
        checksumStream = ByteArrayInputStream(output.toByteArray())
    }

    @AfterEach
    fun afterEach() {
        newFile.delete()
        oldFile.delete()
        mergeFile.delete()
        deltaOutput.reset()
    }

    @Test
    fun uploadTest() {
        val newData = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12,
            15
        )
        newFile.writeBytes(newData)
        val bkSync = BkSync(4)
        val dstOldFile = oldFile
        val srcNewFile = newFile
        val mergeFile = mergeFile

        val signOutputStream = ByteArrayOutputStream()
        // 生成签名文件
        bkSync.checksum(dstOldFile, signOutputStream)
        val signInputStream = ByteArrayInputStream(signOutputStream.toByteArray())
        val deltaOutputStream = ByteArrayOutputStream()
        // 比较差异
        bkSync.diff(srcNewFile, signInputStream, deltaOutputStream)
        val deltaInputStream = ByteArrayInputStream(deltaOutputStream.toByteArray())
        // 结合delta文件，merge成新文件
        val newFileOutputStream = mergeFile.outputStream()
        newFileOutputStream.use {
            bkSync.merge(dstOldFile, deltaInputStream, newFileOutputStream)
        }
        // 检查生成的文件与源文件是否相同
        Assertions.assertEquals(srcNewFile.length(), mergeFile.length())
        Assertions.assertEquals(srcNewFile.md5(), mergeFile.md5())
    }

    @Test
    fun checksumTest() {
        val bkSync = BkSync(2)
        val data = byteArrayOf(1, 2, 3, 4, 5, 6)
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()
        bkSync.checksum(input, output)
        Assertions.assertEquals(60, output.size())
        val checksumStream = ByteArrayInputStream(output.toByteArray())
        val index = ChecksumIndex(checksumStream)
        Assertions.assertEquals(3, index.size())
    }

    @Test
    fun diffInTailTest() {
        val fileData = byteArrayOf(
            1, 2, 3, 4,
            5, 6, 7, 8,
            9, 10, 11, 12,
            13, 14
        )
        newFile.writeBytes(fileData)
        bkSync.diff(newFile, checksumStream, deltaOutput)

        // delta stream should 3*ref(4b) +begin(-1,4b) +len(4b) +delta data(2b) 
        Assertions.assertEquals(22, deltaOutput.size())
        val deltaData = deltaOutput.toByteArray()
        Assertions.assertEquals(14, deltaData.last())
    }

    @DisplayName("中间插入检测测试")
    @Test
    fun diffInMid() {
        val newData = byteArrayOf(
            1, 2, 3, 4,
            5, 13, 14, 6, 7, 8,
            9, 10, 11, 12
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)

        // delta stream should 2*ref(4b) +begin(-1,4b) +len(4b) +delta data(6b)
        Assertions.assertEquals(22, deltaOutput.size())
        val deltaData = deltaOutput.toByteArray()
        // ref0 -1 len delta(5, 13, 14, 6, 7, 8) ref2
        Assertions.assertEquals(5, deltaData[12])
        Assertions.assertEquals(13, deltaData[13])
        Assertions.assertEquals(14, deltaData[14])
        Assertions.assertEquals(6, deltaData[15])
        Assertions.assertEquals(7, deltaData[16])
        Assertions.assertEquals(8, deltaData[17])
        // ref2
        Assertions.assertEquals(2, deltaData.last())
    }

    @DisplayName("前面插入测试")
    @Test
    fun diffInHead() {
        val newData = byteArrayOf(
            10, 11, 12,
            1, 2, 3, 4,
            5, 6, 7, 8,
            9, 10, 11, 12
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)

        // delta stream should 3*ref(4b) +begin(-1,4b) +len(4b) +delta data(3b)
        Assertions.assertEquals(23, deltaOutput.size())

        val deltaData = deltaOutput.toByteArray()
        // -1 len delta(10, 11, 12) ref0 ref1 ref2
        Assertions.assertEquals(-1, deltaData[0])
        Assertions.assertEquals(3, deltaData[7])
        Assertions.assertEquals(10, deltaData[8])
        Assertions.assertEquals(11, deltaData[9])
        Assertions.assertEquals(12, deltaData[10])
        Assertions.assertEquals(2, deltaData.last())
    }

    @DisplayName("删除检测测试")
    @Test
    fun diffInDeleteTest() {
        val newData = byteArrayOf(
            2, 3, 4,
            5, 6, 7, 8,
            9, 10, 11, 12
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)

        // delta stream should 2*ref(4b) +begin(-1,4b) +len(4b) +delta data(3b)
        Assertions.assertEquals(19, deltaOutput.size())

        val deltaData = deltaOutput.toByteArray()
        // -1 len delta(2,3,4) ref1 ref2
        Assertions.assertEquals(-1, deltaData[0])
        Assertions.assertEquals(3, deltaData[7])
        Assertions.assertEquals(2, deltaData[8])
        Assertions.assertEquals(3, deltaData[9])
        Assertions.assertEquals(4, deltaData[10])
        Assertions.assertEquals(2, deltaData.last())
    }

    @DisplayName("混合插入删除检测测试")
    @Test
    fun diffInMix() {
        val newData = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12,
            15
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)

        // delta stream should ref(4b) +2*begin(-1,4b) +2*len(4b) +delta data(8b)+delta data(1b)
        Assertions.assertEquals(29, deltaOutput.size())

        val deltaData = deltaOutput.toByteArray()
        // -1 len delta(1, 3, 4, 5, 6, 7, 13, 8) ref2 -1 len delta(15)
        Assertions.assertEquals(-1, deltaData[0])
        Assertions.assertEquals(8, deltaData[7])
        Assertions.assertEquals(1, deltaData[8])
        Assertions.assertEquals(8, deltaData[15])
        Assertions.assertEquals(2, deltaData[19])
        Assertions.assertEquals(15, deltaData.last())
    }

    @DisplayName("合并测试")
    @Test
    fun mergeTest() {
        val newData = byteArrayOf(
            1, 2, 3, 4,
            5, 13, 14, 6, 7, 8,
            9, 10, 11, 12
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)
        // delta stream should 2*ref(4b) +begin(-1,4b) +len(4b) +delta data(6b)
        Assertions.assertEquals(22, deltaOutput.size())
        val deltaInput = ByteArrayInputStream(deltaOutput.toByteArray())
        val newFileOutputStream = mergeFile.outputStream()
        newFileOutputStream.use {
            bkSync.merge(oldFile, deltaInput, newFileOutputStream)
        }
        Assertions.assertEquals(14, mergeFile.length())
        val mergeData = ByteArray(newData.size)
        mergeFile.inputStream().read(mergeData)
        Assertions.assertEquals(true, mergeData.contentEquals(newData))
    }

    fun File.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        this.inputStream().buffered().use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var sizeRead = it.read(buffer)
            while (sizeRead != -1) {
                md.update(buffer, 0, sizeRead)
                sizeRead = it.read(buffer)
            }
        }
        return BigInteger(1, md.digest()).toString(16).padStart(32, '0')
    }
}
