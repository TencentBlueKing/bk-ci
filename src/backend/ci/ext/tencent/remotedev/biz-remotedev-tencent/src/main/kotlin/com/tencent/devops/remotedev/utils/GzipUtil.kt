package com.tencent.devops.remotedev.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtil {
    fun gzipBytes(payload: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            GZIPOutputStream(baos).use { gzip -> gzip.write(payload) }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
        return baos.toByteArray()
    }

    fun unzipBytes(gzPayload: ByteArray): ByteArray {
        val bais = ByteArrayInputStream(gzPayload)
        try {
            GZIPInputStream(bais).use { gzip ->
                return gzip.readBytes()
            }
        } catch (e: IOException) {
            throw UncheckedIOException("Error while unpacking gzip content", e)
        }
    }
}
