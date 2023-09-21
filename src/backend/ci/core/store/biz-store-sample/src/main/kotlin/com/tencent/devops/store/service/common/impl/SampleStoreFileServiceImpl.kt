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

import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.service.common.StoreFileService
import java.io.File
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class SampleStoreFileServiceImpl : StoreFileService() {

    companion object {
        private val logger = LoggerFactory.getLogger(SampleStoreFileServiceImpl::class.java)
    }
    override fun uploadFileToPath(
        userId: String,
        pathList: List<String>,
        client: Client,
        fileDirPath: String,
        result: MutableMap<String, String>
    ): Map<String, String> {
        pathList.forEach { path ->
            val file = File("$fileDirPath${fileSeparator}$path")
            if (file.exists()) {
                val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
                val fileUrl = CommonUtils.serviceUploadFile(
                    userId = userId,
                    serviceUrlPrefix = serviceUrlPrefix,
                    file = file,
                    fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
                    logo = true,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ).data
                fileUrl?.let { result[path] = fileUrl }
            } else {
                logger.warn("Resource file does not exist:${file.path}")
            }
            file.delete()
        }
        return result
    }

    override fun serviceArchiveAtomFile(
        userId: String,
        projectCode: String,
        atomCode: String,
        serviceUrlPrefix: String,
        releaseType: String,
        version: String,
        file: File,
        os: String
    ): Result<Boolean?> {
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/archiveAtom" +
                "?userId=$userId&projectCode=$projectCode&atomCode=$atomCode" +
                "&version=$version&releaseType=$releaseType&os=$os"
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            response.body!!.string()
            if (!response.isSuccessful) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = I18nUtil.getLanguage(userId)
                )
            }
            return Result(true)
        }
    }
}
