package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.sign.service.IpaSignService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class IpaSignServiceImpl : IpaSignService {

    override fun resignIpaPackage(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)

        if (!fileSuffix.contains("ipa") && !fileSuffix.contains("IPA")) {
            throw InvalidParamException(
                message = "该文件不是正确的IPA包",
                params = arrayOf(fileName)
            )
        }

        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }

        return Result(fileName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IpaSignServiceImpl::class.java)
    }
}