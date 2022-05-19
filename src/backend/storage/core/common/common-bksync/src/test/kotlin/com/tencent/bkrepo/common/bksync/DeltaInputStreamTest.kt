package com.tencent.bkrepo.common.bksync

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.junit.jupiter.api.Test

class DeltaInputStreamTest {

    private val bkSync = BkSync(4, 2)
    private val oldData = byteArrayOf(
        1, 2, 3, 4,
        5, 6, 7, 8,
        9, 10, 11, 12
    )
    private lateinit var checksumStream: InputStream
    private val newFile = createTempFile()

    @BeforeEach
    fun beforeEach() {
        val input = ByteArrayInputStream(oldData)
        val output = ByteArrayOutputStream()
        bkSync.checksum(input, output)
        checksumStream = ByteArrayInputStream(output.toByteArray())
    }

    @Test
    fun readTest() {
        val deltaOutput = ByteArrayOutputStream()
        val newData = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12,
            15
        )
        newFile.writeBytes(newData)
        bkSync.diff(newFile, checksumStream, deltaOutput)
        val deltaInput = DeltaInputStream(ByteArrayInputStream(deltaOutput.toByteArray()))
        // -1 len delta(1, 3, 4, 5, 6, 7, 13, 8) ref2 -1 len delta(15)
        // pos -1
        deltaInput.moveToNext()
        Assertions.assertEquals(true, deltaInput.isDataSequence())
        deltaInput.moveToNext()
        // pos len
        Assertions.assertEquals(8, deltaInput.getDataSequenceLength())
        val deltaData = ByteArray(8)
        deltaInput.read(deltaData)
        Assertions.assertEquals(1, deltaData.first())
        Assertions.assertEquals(8, deltaData.last())
        deltaInput.moveToNext()
        // pos ref2
        Assertions.assertEquals(true, deltaInput.isBlockReference())
        Assertions.assertEquals(2, deltaInput.getBlockReference())
        deltaInput.moveToNext()
        // pos -1
        Assertions.assertEquals(true, deltaInput.isDataSequence())
        deltaInput.moveToNext()
        // pos len
        Assertions.assertEquals(1, deltaInput.getDataSequenceLength())
        val deltaData1 = ByteArray(1)
        deltaInput.read(deltaData1)
        Assertions.assertEquals(15, deltaData1.first())
    }

    @Test
    fun lastIsRefTest() {
        val newData = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12
        )
        val newFile = createTempFile()
        newFile.writeBytes(newData)
        val deltaOutput = ByteArrayOutputStream()
        bkSync.diff(newFile, checksumStream, deltaOutput)
        val deltaInput = DeltaInputStream(ByteArrayInputStream(deltaOutput.toByteArray()))
        // -1 len delta(1, 3, 4, 5, 6, 7, 13, 8) ref2
        var move = 0
        var ret = deltaInput.moveToNext()
        while (ret > 0) {
            move++
            if (deltaInput.isDataSequence()) {
                deltaInput.moveToNext()
                move++
                val len = deltaInput.getDataSequenceLength()
                val byteArray = ByteArray(len)
                deltaInput.read(byteArray)
            }
            ret = deltaInput.moveToNext()
        }
        Assertions.assertEquals(3, move)
    }
}
