package com.tencent.bkrepo.common.bksync.benchmarks

import com.tencent.bkrepo.common.bksync.BufferedSlidingWindow
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
open class BufferedSlidingWindowBenchmark {
    lateinit var file: File
    lateinit var raf: RandomAccessFile
    lateinit var bsw: BufferedSlidingWindow

    companion object {
        private const val MB = 1024 * 1024
        private const val GB = 1024 * MB
    }

    @Setup
    fun setup() {
        file = createTempFile()
        Files.newOutputStream(file.toPath()).use {
            ZeroInputStream(GB).copyTo(it)
        }
        raf = RandomAccessFile(file, "r")
        bsw = BufferedSlidingWindow(2000, 16 * MB, file.inputStream(), file.length())
    }

    @TearDown
    fun tearDown() {
        raf.close()
        file.delete()
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    fun testMoveToNextByte() {
        bsw.moveToNextByte()
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    fun testMoveToNext() {
        bsw.moveToNext()
    }

    class ZeroInputStream(private val size: Int) : InputStream() {
        var read = 0
        override fun read(): Int {
            if (++read > size) {
                return -1
            }
            return 0
        }
    }
}
