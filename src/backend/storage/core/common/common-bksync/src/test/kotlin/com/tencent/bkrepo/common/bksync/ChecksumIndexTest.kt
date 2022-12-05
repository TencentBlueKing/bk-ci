package com.tencent.bkrepo.common.bksync

import com.google.common.hash.Hashing
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Adler32
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ChecksumIndexTest {

    private val checksumStream: InputStream
    private val checksumSet = mutableSetOf<Pair<Long, ByteArray>>()
    private val count = 50

    init {
        val adler32 = Adler32()
        val outputStream = ByteArrayOutputStream()
        while (checksumSet.size < count) {
            val nanoTime = System.nanoTime()
            val data = Longs.toByteArray(nanoTime)
            adler32.update(data)
            val value = adler32.value
            val rollingHash = Ints.toByteArray(value.toInt())
            adler32.reset()
            val md5HashBytes = Hashing.md5().hashBytes(data)
            val md5 = md5HashBytes.asBytes()
            checksumSet.add(Pair(value, md5))
            outputStream.write(rollingHash)
            outputStream.write(md5)
        }
        checksumStream = ByteArrayInputStream(outputStream.toByteArray())
    }

    @Test
    fun initTest() {
        val checksumIndex = ChecksumIndex(checksumStream)
        if (count != checksumIndex.size()) {
            println(1)
        }
        Assertions.assertEquals(count, checksumIndex.size())
        checksumSet.forEach {
            Assertions.assertEquals(true, checksumIndex.exist(it.first.toInt()))
            val checksum = checksumIndex.get(it.first.toInt(), it.second)
            Assertions.assertNotNull(checksum)
            Assertions.assertTrue(it.second.contentEquals(checksum!!.md5))
        }
        Assertions.assertEquals(false, checksumIndex.exist(System.nanoTime().toInt()))
    }
}
