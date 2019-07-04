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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.artifactory.api.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum.WEB_SHOW
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.service.common.StoreLogoService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * store商店logo逻辑类
 *
 * since: 2019-02-15
 */
@Service
class StoreLogoServiceImpl @Autowired constructor(
    private val client: Client
) : StoreLogoService {

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
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1).toLowerCase()
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadLogoTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType)) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_ATOM_LOGO_TYPE_IS_NOT_SUPPORT,
                arrayOf(fileType, allowUploadLogoTypes)
            )
        }
        // 校验上传文件大小是否超出限制
        val fileSize = disposition.size
        val maxFileSize = maxUploadLogoSize.toLong()
        if (fileSize > maxFileSize) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.UPLOAD_LOGO_IS_TOO_LARGE,
                arrayOf((maxFileSize / 1048576).toString() + "M")
            )
        }
        val file = Files.createTempFile("random_" + System.currentTimeMillis(), ".$fileType").toFile()
        val output = file.outputStream()
        // svg类型图片不做尺寸检查
        if ("svg" != fileType) {
            val img = ImageIO.read(inputStream)
            // 判断上传的logo是否为512x512规格
            val width = img.width
            val height = img.height
            if (width != height || width < allowUploadLogoWidth.toInt()) {
                return MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_ATOM_LOGO_SIZE_IS_INVALID,
                    arrayOf(allowUploadLogoWidth, allowUploadLogoHeight)
                )
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
                logger.error("the output write error is:$e", e)
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            } finally {
                output.close()
            }
        }
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        return CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, file, WEB_SHOW.name)
    }
}
