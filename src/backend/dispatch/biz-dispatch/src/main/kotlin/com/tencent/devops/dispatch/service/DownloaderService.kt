package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.util.FileUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class DownloaderService {

    @Value("\${dispatch.workerFile:#{null}}")
    private val workerFile: String? = null

    fun downloadWorker(eTag: String?): Response {
        if (workerFile.isNullOrBlank()) {
            throw RuntimeException("worker.jar文件路径没有配置")
        }
        return download(workerFile!!, eTag)
    }

    private fun download(file: String, eTag: String?): Response {
        val worker = File(file)
        if (!worker.exists()) {
            throw NotFoundException("${worker.absolutePath} 不存在")
        }

        if (!worker.isFile) {
            throw RuntimeException("${worker.absolutePath} 不是一个文件")
        }

        if (eTag != null && eTag.isNotBlank()) {
            // 检查文件的MD5值是否和客户端一致
            val workerMD5 = FileUtil.getMD5(worker)
            if (workerMD5 != null && workerMD5 == eTag) {
                return Response.status(Response.Status.NOT_MODIFIED).build()
            }
        }
        val fileStream = StreamingOutput { output ->
            output.write(worker.readBytes())
            output.flush()
        }
        return Response
                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = ${worker.name}")
                .build()
    }
}