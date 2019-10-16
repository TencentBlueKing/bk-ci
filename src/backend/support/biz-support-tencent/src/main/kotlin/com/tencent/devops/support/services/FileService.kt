package com.tencent.devops.support.services

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.support.constant.SupportMessageCode
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files

@Service
class FileService @Autowired constructor(private val awsClientService: AwsClientService) {

    @Value("\${file.allowUploadFileTypes}")
    private lateinit var allowUploadFileTypes: String

    @Value("\${file.maxUploadFileSize}")
    private lateinit var maxUploadFileSize: String

    fun uploadFile(inputStream: InputStream, disposition: FormDataContentDisposition): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1)
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadFileTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType.toLowerCase())) {
            return MessageCodeUtil.generateResponseDataObject(SupportMessageCode.UPLOAD_FILE_TYPE_IS_NOT_SUPPORT, arrayOf(fileType, allowUploadFileTypes))
        }
        val file = Files.createTempFile("random_" + System.currentTimeMillis(), ".$fileType").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        // 校验上传文件大小是否超出限制
        val fileSize = file.length()
        val maxFileSize = maxUploadFileSize.toLong()
        if (fileSize>maxFileSize) {
            return MessageCodeUtil.generateResponseDataObject(SupportMessageCode.UPLOAD_FILE_IS_TOO_LARGE, arrayOf((maxFileSize / 1048576).toString() + "M"))
        }
        val result: Result<String?>
        try {
            result = awsClientService.uploadFile(file)
        } finally {
            file.delete()
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileService::class.java)
    }
}
