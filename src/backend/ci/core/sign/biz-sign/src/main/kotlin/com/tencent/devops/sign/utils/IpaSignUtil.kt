package com.tencent.devops.sign.utils

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.pojo.IpaSignInfo
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object IpaSignUtil {
    private val bufferSize =  8 * 1024
    /*
    * 复制流到目标文件，并计算md5
    * */
    fun copyInputStreamToFile(
            inputStream: InputStream,
            target: File
    ): String? {
        val md5 = null
        inputStream.copyTo(FileOutputStream(target))
        return null
    }
}