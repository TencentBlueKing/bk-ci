package com.tencent.devops.support.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.service.ServiceImageManageResource
import com.tencent.devops.support.model.image.UploadImageRequest
import com.tencent.devops.support.services.AwsClientService
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.net.URL
import java.nio.file.Files

@RestResource
class ServiceImageManageResourceImpl @Autowired constructor(private val awsClientService: AwsClientService) : ServiceImageManageResource {

    private val logger = LoggerFactory.getLogger(ServiceImageManageResourceImpl::class.java)

    /**
     * 按照规定大小压缩图片
     */
    override fun compressImage(imageUrl: String, compressWidth: Int, compressHeight: Int): Result<String> {
        val file = Files.createTempFile("random_" + System.currentTimeMillis(), ".png").toFile()
        val url = URL(imageUrl)
        val bytes: ByteArray?
        try {
            Thumbnails.of(url)
                    .size(compressWidth, compressHeight)
                    .outputFormat("png")
                    .toFile(file)
            bytes = Files.readAllBytes(file.toPath())
        } finally {
            file.delete()
        }
        val data = "data:image/png;base64," + Base64.encodeBase64String(bytes)
        logger.info("the compressImage base64 data is:$data")
        return Result(data)
    }

    override fun uploadImage(userId: String, uploadImageRequest: UploadImageRequest): Result<String?> {
        val bytes = Base64.decodeBase64(uploadImageRequest.imageContentStr)
        val file = Files.createTempFile("random_" + System.currentTimeMillis(), ".${uploadImageRequest.imageType}").toFile()
        try {
            file.writeBytes(bytes)
            return awsClientService.uploadFile(file)
        } finally {
            file.delete()
        }
    }
}