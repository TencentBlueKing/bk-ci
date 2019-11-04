/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
