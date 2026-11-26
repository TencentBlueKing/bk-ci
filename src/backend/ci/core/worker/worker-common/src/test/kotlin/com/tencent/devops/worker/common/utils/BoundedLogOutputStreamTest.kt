package com.tencent.devops.worker.common.utils

import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundedLogOutputStreamTest {

    @Test
    fun splitsLongLineWithoutNewlineByMaxBytes() {
        val lines = mutableListOf<String>()
        val stream = object : BoundedLogOutputStream(
            charsetName = StandardCharsets.UTF_8.name(),
            maxLineBytes = 10
        ) {
            override fun processLine(line: String?, level: Int) {
                if (line != null) {
                    lines.add(line)
                }
            }
        }

        stream.write("0123456789abcdefghijXYZ\n".toByteArray(StandardCharsets.UTF_8))
        stream.close()

        assertEquals(listOf("0123456789", "abcdefghij", "XYZ"), lines)
    }

    @Test
    fun preservesUtf8CharacterBoundariesWhenSplitting() {
        val lines = mutableListOf<String>()
        val stream = object : BoundedLogOutputStream(
            charsetName = StandardCharsets.UTF_8.name(),
            maxLineBytes = 7
        ) {
            override fun processLine(line: String?, level: Int) {
                if (line != null) {
                    lines.add(line)
                }
            }
        }

        // 每个中文字符占 3 字节，7 字节边界落在第 3 个中文中间，应完整保留该字符后再切分
        val text = "游戏引擎日志流测试"
        stream.write("$text\n".toByteArray(StandardCharsets.UTF_8))
        stream.close()

        assertTrue(lines.size > 1)
        assertEquals(text, lines.joinToString(""))
        assertTrue(lines.none { it.contains('\uFFFD') })
    }

    @Test
    fun handlesCrLfAndSingleByteWritesNormally() {
        val lines = mutableListOf<String>()
        val stream = object : BoundedLogOutputStream(
            charsetName = StandardCharsets.UTF_8.name(),
            maxLineBytes = 64
        ) {
            override fun processLine(line: String?, level: Int) {
                if (line != null) {
                    lines.add(line)
                }
            }
        }

        val bytes = "first\r\nsecond\nthird".toByteArray(StandardCharsets.UTF_8)
        bytes.forEach { stream.write(it.toInt() and 0xFF) }
        stream.close()

        assertEquals(listOf("first", "second", "third"), lines)
    }
}
