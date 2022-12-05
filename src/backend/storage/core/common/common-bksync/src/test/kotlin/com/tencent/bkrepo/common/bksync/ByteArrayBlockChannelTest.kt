package com.tencent.bkrepo.common.bksync

import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ByteArrayBlockChannelTest {
    @Test
    fun getBlocks() {
        val data = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12,
            15
        )
        val blockInputStream = ByteArrayBlockChannel(data, "")

        val out1 = ByteArrayOutputStream()
        val channel1 = Channels.newChannel(out1)
        blockInputStream.transferTo(0, 2, 4, channel1)
        val data1 = byteArrayOf(
            1, 3, 4, 5,
            6, 7, 13, 8,
            9, 10, 11, 12
        )
        Assertions.assertArrayEquals(data1, out1.toByteArray())

        val out2 = ByteArrayOutputStream()
        val channel2 = Channels.newChannel(out2)
        blockInputStream.transferTo(2, 3, 4, channel2)
        val data2 = byteArrayOf(
            9, 10, 11, 12,
            15
        )
        Assertions.assertArrayEquals(data2, out2.toByteArray())

        val out3 = ByteArrayOutputStream()
        val channel3 = Channels.newChannel(out3)
        blockInputStream.transferTo(3, 3, 4, channel3)
        val data3 = byteArrayOf(15)
        Assertions.assertArrayEquals(data3, out3.toByteArray())
    }
}
