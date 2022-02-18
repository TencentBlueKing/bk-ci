package com.tencent.devops.common.util

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object IOUtil {

    fun gzipCompress(input: String): ByteArray {
        val byteOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteOutputStream).bufferedWriter(StandardCharsets.UTF_8).use { it.write(input) }
        return byteOutputStream.toByteArray()
    }

    fun gzipUnCompress(input: ByteArray): String =
        GZIPInputStream(input.inputStream()).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}
