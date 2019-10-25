package com.tencent.devops.store.service.common

import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.StoreLogoDao
import com.tencent.devops.store.pojo.common.Logo
import com.tencent.devops.store.pojo.common.StoreLogoReq
import com.tencent.devops.support.api.ServiceImageManageResource
import com.tencent.devops.support.model.image.UploadImageRequest
import org.apache.commons.codec.binary.Base64
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * store商城logo逻辑类
 * author: carlyin
 * since: 2019-02-15
 */
@Service
class StoreLogoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeLogoDao: StoreLogoDao,
    private val client: Client
) {
    @Value("\${logo.allowUploadLogoTypes}")
    private lateinit var allowUploadLogoTypes: String

    @Value("\${logo.allowUploadLogoWidth}")
    private lateinit var allowUploadLogoWidth: String

    @Value("\${logo.allowUploadLogoHeight}")
    private lateinit var allowUploadLogoHeight: String

    @Value("\${logo.maxUploadLogoSize}")
    private lateinit var maxUploadLogoSize: String

    private val logger = LoggerFactory.getLogger(StoreLogoService::class.java)

    /**
     * 上传logo
     */
    fun uploadStoreLogo(userId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1).toLowerCase()
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadLogoTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType)) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_ATOM_LOGO_TYPE_IS_NOT_SUPPORT, arrayOf(fileType, allowUploadLogoTypes))
        }
        // 校验上传文件大小是否超出限制
        val fileSize = disposition.size
        val maxFileSize = maxUploadLogoSize.toLong()
        if (fileSize>maxFileSize) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.UPLOAD_LOGO_IS_TOO_LARGE, arrayOf((maxFileSize / 1048576).toString() + "M"))
        }
        val output = ByteArrayOutputStream()
        // svg类型图片不做尺寸检查
        if ("svg" != fileType) {
            val img = ImageIO.read(inputStream)
            // 判断上传的logo是否为512x512规格
            val width = img.width
            val height = img.height
            if (height < allowUploadLogoWidth.toInt() || width < allowUploadLogoWidth.toInt()) {
                return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_ATOM_LOGO_SIZE_IS_INVALID, arrayOf(allowUploadLogoWidth, allowUploadLogoHeight))
            }
            ImageIO.write(img, fileType, output)
        } else {
            val buffer = ByteArray(1024)
            var len: Int
            try {
                while (true) {
                    len = inputStream.read(buffer)
                    if (len > -1) {
                        output.write(buffer, 0, len)
                    } else {
                        break
                    }
                }
                output.flush()
            } catch (e: Exception) {
                logger.info("the output write error is:$e")
            } finally {
                output.close()
            }
        }
        val bytes = output.toByteArray()
        val logoUrl = client.get(ServiceImageManageResource::class).uploadImage(userId, UploadImageRequest(fileType, Base64.encodeBase64String(bytes))).data
        logger.info("the logoUrl is:$logoUrl")
        return Result(logoUrl)
    }

    /**
     * 获取logo列表
     */
    fun list(
        userId: String,
        type: String
    ): Result<List<Logo>?> {
        logger.info("list logo: userId=$userId, type=$type")
        val logos = storeLogoDao.getAllLogo(dslContext, type)?.map { storeLogoDao.convert(it) }

        return Result(logos)
    }

    /**
     * 获取logo
     */
    fun get(
        userId: String,
        id: String
    ): Result<Logo?> {
        logger.info("get logo: userId=$userId, id=$id")
        val logo = storeLogoDao.getLogo(dslContext, id)
        return Result(if (logo == null) {
            null
        } else {
            storeLogoDao.convert(logo)
        })
    }

    /**
     * 新增logo
     */
    fun add(
        userId: String,
        type: String,
        storeLogoReq: StoreLogoReq
    ): Result<Boolean> {
        logger.info("add logo: userId=$userId, type=$type, storeLogoReq=$storeLogoReq")
        val id = UUIDUtil.generate()
        storeLogoDao.add(dslContext, id, userId, storeLogoReq, type)

        return Result(true)
    }

    /**
     * 更新logo
     */
    fun update(
        userId: String,
        id: String,
        storeLogoReq: StoreLogoReq
    ): Result<Boolean> {
        logger.info("update logo: userId=$userId, id=$id, storeLogoReq=$storeLogoReq")
        storeLogoDao.update(dslContext, id, userId, storeLogoReq)

        return Result(true)
    }

    /**
     * 删除logo
     */
    fun delete(
        userId: String,
        id: String
    ): Result<Boolean> {
        logger.info("delete logo: userId=$userId, id=$id")
        storeLogoDao.delete(dslContext, id)

        return Result(true)
    }
}
