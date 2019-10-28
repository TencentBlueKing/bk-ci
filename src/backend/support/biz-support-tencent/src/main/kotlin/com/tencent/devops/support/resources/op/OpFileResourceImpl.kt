package com.tencent.devops.support.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.op.OpFileResource
import com.tencent.devops.support.services.FileService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class OpFileResourceImpl @Autowired constructor(private val fileService: FileService) : OpFileResource {

    override fun uploadFile(userId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<String?> {
        return fileService.uploadFile(inputStream, disposition)
    }
}