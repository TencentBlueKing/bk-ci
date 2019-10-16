package com.tencent.devops.support.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.user.UserFileResource
import com.tencent.devops.support.services.FileService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserFileResourceImpl @Autowired constructor(private val fileService: FileService) : UserFileResource {

    override fun uploadFile(userId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<String?> {
        return fileService.uploadFile(inputStream, disposition)
    }
}