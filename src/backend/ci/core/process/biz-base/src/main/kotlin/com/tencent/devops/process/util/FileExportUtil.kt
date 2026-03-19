package com.tencent.devops.process.util

import java.net.URLEncoder
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput

object FileExportUtil {
    fun exportStringToFile(content: String, fileName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(content)
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val encodeName = URLEncoder.encode(fileName, "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $encodeName")
            .header("Cache-Control", "no-cache")
            .build()
    }
}
