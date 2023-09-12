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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceBkRepoStaticResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.service.common.StoreFileService
import com.tencent.devops.store.utils.StoreUtils
import java.io.File
import org.slf4j.LoggerFactory

@Suppress("ALL")
class TxStoreFileServiceImpl : StoreFileService() {

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreFileServiceImpl::class.java)
    }
    override fun uploadFileToPath(
        userId: String,
        pathList: List<String>,
        client: Client,
        atomPath: String,
        result: MutableMap<String, String>
    ): Map<String, String> {
        pathList.forEach { path ->
            val file = File("$atomPath${fileSeparator}file${fileSeparator}$path")
            if (file.exists()) {
                val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoStaticResource::class)
                val fileUrl = serviceUploadFile(
                    userId = userId,
                    serviceUrlPrefix = serviceUrlPrefix,
                    file = file
                ).data
                logger.info("uploadFileToPath return fileUrl:$fileUrl")
                fileUrl?.let { result[path] = StoreUtils.removeUrlHost(fileUrl) }
            } else {
                logger.warn("Resource file does not exist:${file.path}")
            }
            file.delete()
        }
        logger.info("uploadFileToPath result:$result")
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
        TODO("内部版发布不需要归档插件包")
    }

    private fun serviceUploadFile(
        userId: String,
        serviceUrlPrefix: String,
        file: File
    ): Result<String> {
        val index = file.path.indexOf(BK_CI_ATOM_DIR)
        val serviceUrl = "$serviceUrlPrefix/service/bkrepo/statics/file/upload" +
                "?userId=$userId&destPath=${file.path.substring(index)}"
        logger.info("the serviceUrl is:$serviceUrl")
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            val responseContent = response.body!!.string()
            logger.error("uploadFile responseContent is: $responseContent")
            if (!response.isSuccessful) {
                val message = I18nUtil.getCodeLanMessage(messageCode = CommonMessageCode.SYSTEM_ERROR)
                Result(CommonMessageCode.SYSTEM_ERROR.toInt(), message, null)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String>>() {})
        }
    }
}
