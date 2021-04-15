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

package com.tencent.bkrepo.common.storage.filesystem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class FileSystemClientTest {

    private val inputFileName = "test.file"
    private val outputFileName = "output.file"
    private val tempDirectoryName = "tmp"
    private val inputFile = File(javaClass.getResource("/$inputFileName").file)

    private val fileSystemClient = FileSystemClient(javaClass.getResource("/").path)

    @BeforeEach
    fun beforeEach() {
        javaClass.getResource("/$outputFileName")?.file?.let {
            val outputFile = File(it)
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }

        javaClass.getResource("/$tempDirectoryName")?.file?.let {
            val tempDirectory = File(it)
            if (tempDirectory.exists()) {
                tempDirectory.delete()
            }
        }
    }

    @Test
    fun touch() {
        val newFile = fileSystemClient.touch("/aaa", outputFileName)
        assertTrue(newFile.exists())
        assertTrue(newFile.isFile)
        fileSystemClient.delete("/aaa", outputFileName)
        fileSystemClient.deleteDirectory("/", "aaa")
    }

    @Test
    fun storeFromString() {
        fileSystemClient.store("/", outputFileName, "0123456789".byteInputStream(), 10)
        val file = fileSystemClient.load("/", outputFileName)!!
        assertEquals("0123456789", file.readText())
    }

    @Test
    fun storeFromFile() {
        val inputStream = inputFile.inputStream()
        fileSystemClient.store("/", outputFileName, inputStream, inputFile.length())
        val file = fileSystemClient.load("/", outputFileName)!!
        assertEquals(file.readText(), inputFile.readText())
    }

    @Test
    fun storeRepeat() {
        fileSystemClient.store("/", outputFileName, "0123456789".byteInputStream(), 10)
        fileSystemClient.store("/", outputFileName, "0123456789abc".byteInputStream(), 13)
        val file = fileSystemClient.load("/", outputFileName)!!
        assertEquals("0123456789", file.readText())
    }

    @Test
    fun storeOverwrite() {
        fileSystemClient.store("/", outputFileName, "0123456789".byteInputStream(), 10, true)
        fileSystemClient.store("/", outputFileName, "0123456789abc".byteInputStream(), 13, true)
        val file = fileSystemClient.load("/", outputFileName)!!
        assertEquals("0123456789abc", file.readText())
    }

    @Test
    fun delete() {
        fileSystemClient.store("/", outputFileName, "0123456789".byteInputStream(), 10)
        assertTrue(fileSystemClient.exist("/", outputFileName))
        fileSystemClient.delete("/", outputFileName)
        assertFalse(fileSystemClient.exist("/", outputFileName))
        assertFalse(fileSystemClient.exist("/", "123"))
        assertFalse(fileSystemClient.exist("/", ""))
    }

    @Test
    fun load() {
        assertNull(fileSystemClient.load("/", outputFileName))
        fileSystemClient.store("/", outputFileName, "0123456789".byteInputStream(), 10)
        assertTrue(fileSystemClient.exist("/", outputFileName))
    }

    @Test
    fun append() {
        assertThrows<IllegalArgumentException> {
            fileSystemClient.append("/", outputFileName, "0123456789".byteInputStream(), 10)
        }
        fileSystemClient.touch("/", outputFileName)
        fileSystemClient.append("/", outputFileName, "0123456789".byteInputStream(), 10)
        fileSystemClient.append("/", outputFileName, "abc".byteInputStream(), 3)
        fileSystemClient.append("/", outputFileName, "def".byteInputStream(), 3)
        val file = fileSystemClient.load("/", outputFileName)!!
        assertEquals("0123456789abcdef", file.readText())
    }

    @Test
    fun createDirectory() {
        fileSystemClient.createDirectory("/", tempDirectoryName)
        fileSystemClient.createDirectory("/", tempDirectoryName)
    }

    @Test
    fun deleteDirectory() {
        fileSystemClient.touch("/", outputFileName)
        assertThrows<IllegalArgumentException> {
            fileSystemClient.deleteDirectory("/", outputFileName)
        }
        assertThrows<IllegalArgumentException> {
            fileSystemClient.deleteDirectory("/", tempDirectoryName)
        }
        fileSystemClient.createDirectory("/", tempDirectoryName)
        assertTrue(fileSystemClient.checkDirectory(tempDirectoryName))
        fileSystemClient.deleteDirectory("/", tempDirectoryName)
        assertFalse(fileSystemClient.checkDirectory(tempDirectoryName))
    }

    @Test
    fun listFiles() {
        assertEquals(0, fileSystemClient.listFiles("/", "").size)
        fileSystemClient.createDirectory("/", tempDirectoryName)
        assertEquals(0, fileSystemClient.listFiles(tempDirectoryName, "").size)
        fileSystemClient.touch(tempDirectoryName, "1.txt")
        fileSystemClient.touch(tempDirectoryName, "2.txt")
        fileSystemClient.touch(tempDirectoryName, "2.txt0")
        fileSystemClient.createDirectory(tempDirectoryName, "sub")
        fileSystemClient.touch("$tempDirectoryName/sub", "1.txt")
        assertEquals(2, fileSystemClient.listFiles(tempDirectoryName, "txt").size)
        assertEquals(1, fileSystemClient.listFiles("$tempDirectoryName/sub", "txt").size)
        assertEquals(0, fileSystemClient.listFiles(tempDirectoryName, "txt1").size)
        assertEquals(0, fileSystemClient.listFiles("/", "txt").size)
        fileSystemClient.delete("$tempDirectoryName/sub", "1.txt")
        fileSystemClient.deleteDirectory(tempDirectoryName, "/sub")
        fileSystemClient.delete(tempDirectoryName, "1.txt")
        fileSystemClient.delete(tempDirectoryName, "2.txt")
        fileSystemClient.delete(tempDirectoryName, "2.txt0")
    }

    @Test
    fun mergeFiles() {
        val file = fileSystemClient.touch("/", outputFileName)
        fileSystemClient.mergeFiles(emptyList(), file)
        assertEquals("", file.readText())

        fileSystemClient.mergeFiles(listOf(inputFile, inputFile), file)
        assertEquals("Hello, world!Hello, world!", file.readText())
    }
}
