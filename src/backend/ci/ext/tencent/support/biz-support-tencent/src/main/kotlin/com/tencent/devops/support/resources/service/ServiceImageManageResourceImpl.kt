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

package com.tencent.devops.support.resources.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.service.ServiceImageManageResource
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.codec.binary.Base64
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files

@RestResource
class ServiceImageManageResourceImpl @Autowired constructor(private val client: Client) : ServiceImageManageResource {

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
        return Result(data)
    }

    override fun uploadImage(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1).toLowerCase()
        val uploadFileName = "${UUIDUtil.generate()}.$fileType"
        val tmpdir = System.getProperty("java.io.tmpdir")
        val file = File(tmpdir, uploadFileName)
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoResource::class)
        val destPath = "image/$uploadFileName"
        val serviceUrl =
            "$serviceUrlPrefix/service/bkrepo/statics/file/upload?userId=$userId&destPath=$destPath"
        try {
            OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
                }
                return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
            }
        } finally {
            file.delete()
        }
    }
}
