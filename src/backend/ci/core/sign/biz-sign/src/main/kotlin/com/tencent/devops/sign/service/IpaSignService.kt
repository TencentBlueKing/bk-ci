package com.tencent.devops.sign.service

import com.tencent.devops.common.api.pojo.Result
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.InputStream

interface IpaSignService {
    fun resignIpaPackage(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?>
}