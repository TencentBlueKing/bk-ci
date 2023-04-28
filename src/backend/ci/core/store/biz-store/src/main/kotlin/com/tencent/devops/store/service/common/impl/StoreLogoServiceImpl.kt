/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreLogoDao
import com.tencent.devops.store.pojo.common.Logo
import com.tencent.devops.store.pojo.common.StoreLogoInfo
import com.tencent.devops.store.pojo.common.StoreLogoReq
import com.tencent.devops.store.service.common.StoreLogoService
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

/**
 * store商店logo逻辑类
 *
 * since: 2019-02-15
 */
@Suppress("ALL")
@Service
@RefreshScope
abstract class StoreLogoServiceImpl @Autowired constructor() : StoreLogoService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeLogoDao: StoreLogoDao

    @Autowired
    lateinit var client: Client

    @Value("\${logo.allowUploadLogoTypes}")
    private lateinit var allowUploadLogoTypes: String

    @Value("\${logo.allowUploadLogoWidth}")
    private lateinit var allowUploadLogoWidth: String

    @Value("\${logo.allowUploadLogoHeight}")
    private lateinit var allowUploadLogoHeight: String

    @Value("\${logo.maxUploadLogoSize}")
    private lateinit var maxUploadLogoSize: String

    private val logger = LoggerFactory.getLogger(StoreLogoServiceImpl::class.java)

    /**
     * 上传logo
     */
    override fun uploadStoreLogo(
        userId: String,
        contentLength: Long,
        sizeLimitFlag: Boolean?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<StoreLogoInfo?> {
        val fileName = disposition.fileName
        logger.info("uploadStoreLogo upload file fileName is:$fileName,contentLength is:$contentLength")
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1).lowercase()
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadLogoTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_LOGO_TYPE_IS_NOT_SUPPORT,
                params = arrayOf(fileType, allowUploadLogoTypes),
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 校验上传文件大小是否超出限制
        val maxFileSize = maxUploadLogoSize.toLong()
        if (contentLength > maxFileSize) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.UPLOAD_LOGO_IS_TOO_LARGE,
                params = arrayOf((maxFileSize / 1048576).toString() + "M"),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        val output = file.outputStream()
        // svg类型图片不做尺寸检查
        if ("svg" != fileType) {
            val img = ImageIO.read(inputStream)
            // 判断上传的logo是否为512x512规格
            val width = img.width
            val height = img.height
            if (sizeLimitFlag != false) {
                if (width != height || width < allowUploadLogoWidth.toInt()) {
                    return I18nUtil.generateResponseDataObject(
                        StoreMessageCode.USER_ATOM_LOGO_SIZE_IS_INVALID,
                        arrayOf(allowUploadLogoWidth, allowUploadLogoHeight),
                        language = I18nUtil.getLanguage(userId)
                    )
                }
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
            } catch (ignored: Throwable) {
                logger.error("BKSystemErrorMonitor|uploadStoreLogo|error=${ignored.message}", ignored)
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = I18nUtil.getLanguage(userId)
                )
            } finally {
                output.close()
            }
        }
        val logoUrl = uploadStoreLogo(userId, file).data
        logger.info("uploadStoreLogo logoUrl is:$logoUrl")
        return Result(StoreLogoInfo(logoUrl))
    }

    abstract fun uploadStoreLogo(userId: String, file: File): Result<String?>

    /**
     * 获取logo列表
     */
    override fun list(
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
    override fun get(
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
    override fun add(
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
    override fun update(
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
    override fun delete(
        userId: String,
        id: String
    ): Result<Boolean> {
        logger.info("delete logo: userId=$userId, id=$id")
        storeLogoDao.delete(dslContext, id)
        return Result(true)
    }
}
