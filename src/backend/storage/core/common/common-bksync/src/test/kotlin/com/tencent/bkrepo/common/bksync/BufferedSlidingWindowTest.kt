package com.tencent.bkrepo.common.bksync

import java.io.RandomAccessFile
import java.nio.file.Files
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BufferedSlidingWindowTest {

    private val tempFile = createTempFile()
    private val data = "ABCDEFGHIGKLMNOPQRSTUVWXYZ".toByteArray()
    private val ras = RandomAccessFile(tempFile, "r")
    private val bufferSize = 16 * 1024

    @BeforeEach
    fun beforeEach() {
        tempFile.writeBytes(data)
    }

    @AfterEach
    fun afterEach() {
        ras.close()
        Files.deleteIfExists(tempFile.toPath())
    }

    @DisplayName("测试文件大小大于窗口的初始化情况")
    @Test
    fun initFileSizeBigThanWindowTest() {
        val window = BufferedSlidingWindow(4, bufferSize, data.inputStream(), data.size.toLong())
        window.moveToNext()
        // window data ABCD
        Assertions.assertEquals("ABCD", String(window.content()))
    }

    @DisplayName("测试文件大小小于窗口的初始化情况")
    @Test
    fun initFileSizeLessThanWindowTest() {
        val window = BufferedSlidingWindow(26, bufferSize, data.inputStream(), data.size.toLong())
        window.moveToNext()
        // window data is ALL
        Assertions.assertEquals(String(data), String(window.content()))
    }

    @DisplayName("测试移动一个字节")
    @Test
    fun moveByteTest() {
        val window = BufferedSlidingWindow(4, bufferSize, data.inputStream(), data.size.toLong())
        window.moveToNext()
        val (nextHead, nextTail) = window.moveToNextByte()
        // window BCDE
        Assertions.assertEquals("BCDE", String(window.content()))
        // head A
        Assertions.assertEquals('A', nextHead.toChar())
        // tail E
        Assertions.assertEquals('E', nextTail.toChar())

        // 总长度26，窗口大小4，每次移动一个字节，最多移动22次
        repeat(20) { window.moveToNextByte() }
        // 移动到最后
        val (head, tail) = window.moveToNextByte()
        // window WXYZ
        Assertions.assertEquals("WXYZ", String(window.content()))
        // head W
        Assertions.assertEquals('V', head.toChar())
        // tail Z
        Assertions.assertEquals('Z', tail.toChar())

        // 无法移动
        Assertions.assertEquals(false, window.hasNext())
    }

    @DisplayName("测试移动窗口")
    @Test
    fun moveWindowTest() {
        val window = BufferedSlidingWindow(4, bufferSize, data.inputStream(), data.size.toLong())
        // ABCD
        window.moveToNext()
        Assertions.assertEquals("ABCD", String(window.content()))
        // 总长度26，总共（6*4，1*2）7个窗口大小
        repeat(5) { window.moveToNext() }
        Assertions.assertTrue(window.hasNext())
        window.moveToNext()
        // YZ
        Assertions.assertEquals("YZ", String(window.content()))

        // 无法移动
        Assertions.assertEquals(false, window.hasNext())
    }

    @DisplayName("混合移动测试")
    @Test
    fun mixMoveTest() {
        val window = BufferedSlidingWindow(4, bufferSize, data.inputStream(), data.size.toLong())
        window.moveToNext()
        // 总长度26，窗口大小为4，总窗口数为7，最后一个窗口大小为2，移动2个字节，剩余5个窗口
        repeat(2) { window.moveToNextByte() }
        repeat(4) { window.moveToNext() }
        window.moveToNext()
        // 最后一个窗口 WXYZ
        Assertions.assertEquals("WXYZ", String(window.content()))
        // 无法移动
        Assertions.assertEquals(false, window.hasNext())
    }
}
