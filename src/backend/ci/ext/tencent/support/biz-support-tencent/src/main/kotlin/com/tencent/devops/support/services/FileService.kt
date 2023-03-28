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

package com.tencent.devops.support.services

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.support.constant.SupportMessageCode
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files

@Service
class FileService @Autowired constructor(private val client: Client) {

    @Value("\${file.allowUploadFileTypes}")
    private lateinit var allowUploadFileTypes: String

    @Value("\${file.maxUploadFileSize}")
    private lateinit var maxUploadFileSize: String

    fun uploadFile(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        val fileName = disposition.fileName
        logger.info("$userId upload file:$fileName")
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1)
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadFileTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType.toLowerCase())) {
            return MessageUtil.generateResponseDataObject(
                messageCode = SupportMessageCode.UPLOAD_FILE_TYPE_IS_NOT_SUPPORT,
                params = arrayOf(fileType, allowUploadFileTypes),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        // 校验上传文件大小是否超出限制
        val fileSize = file.length()
        val maxFileSize = maxUploadFileSize.toLong()
        if (fileSize > maxFileSize) {
            return MessageUtil.generateResponseDataObject(
                messageCode = SupportMessageCode.UPLOAD_FILE_IS_TOO_LARGE,
                params = arrayOf((maxFileSize / 1048576).toString() + "M"),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoResource::class)
        // 组装文件上传目标路径
        val destPath = "file/$fileType/${file.name}"
        val serviceUrl =
            "$serviceUrlPrefix/service/bkrepo/statics/file/upload?userId=$userId&destPath=$destPath"
        try {
            OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.warn("$userId upload file:$fileName fail,responseContent:$responseContent")
                    return MessageUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    )
                }
                return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
            }
        } finally {
            file.delete()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileService::class.java)
    }
}
