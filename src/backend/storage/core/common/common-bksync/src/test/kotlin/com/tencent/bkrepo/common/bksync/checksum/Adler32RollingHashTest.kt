package com.tencent.bkrepo.common.bksync.checksum

import java.util.zip.Adler32
import kotlin.random.Random
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class Adler32RollingHashTest {
    @Test
    fun rollingHashTest() {
        val adler32 = Adler32()
        val data = Random.nextBytes(1024 * 1024)
        val windowSize = 1000
        var window = data.copyOf(windowSize)
        val rollingHash = Adler32RollingHash(windowSize)
        rollingHash.update(window)
        adler32.update(window)
        // init
        Assertions.assertEquals(adler32.value, rollingHash.digest())
        // rolling hash
        var next = windowSize
        var start = 1
        while (next < data.size) {
            val out = window.first()
            val enter = data[next]
            rollingHash.rotate(out, enter)
            window = data.copyOfRange(start, start + windowSize)
            Assertions.assertEquals(window.last(), enter)
            adler32.reset()
            adler32.update(window)
            Assertions.assertEquals(adler32.value, rollingHash.digest())
            next++
            start++
        }
    }
}
