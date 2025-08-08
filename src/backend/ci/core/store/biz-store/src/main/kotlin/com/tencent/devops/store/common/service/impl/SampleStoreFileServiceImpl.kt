/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.store.common.service.impl

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.constant.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.constant.REPO_NAME_PLUGIN
import com.tencent.devops.artifactory.pojo.LocalDirectoryInfo
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.web.utils.CommonServiceUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.service.StoreFileService
import com.tencent.devops.store.common.utils.StoreFileAnalysisUtil.isDirectoryNotEmpty
import com.tencent.devops.store.pojo.common.TextReferenceFileDownloadRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.URLEncoder

@Service
class SampleStoreFileServiceImpl : StoreFileService() {

    companion object {
        private val logger = LoggerFactory.getLogger(SampleStoreFileServiceImpl::class.java)
    }

    override fun downloadFile(
        userId: String,
        filePath: String,
        file: File,
        repositoryHashId: String?,
        branch: String?,
        format: String?
    ) {
        try {
            val url = client.getServiceUrl(ServiceArchiveAtomResource::class) +
                "/service/artifactories/atom/file/download?filePath=${URLEncoder.encode(filePath, "UTF-8")}"
            CommonServiceUtils.downloadFileFromService(url, file)
        } catch (ignore: Throwable) {
            logger.warn("FAIL|Download file from $filePath")
        }
    }

    override fun getFileNames(
        projectCode: String,
        fileDir: String,
        i18nDir: String?,
        repositoryHashId: String?,
        branch: String?
    ): List<String>? {
        var filePath = "$projectCode/$fileDir"
        i18nDir?.let { filePath = "$filePath/$i18nDir" }
        logger.info("getFileNames by filePath:$filePath")
        return client.get(ServiceArtifactoryResource::class).listFileNamesByPath(
            projectId = BKREPO_STORE_PROJECT_ID,
            repoName = REPO_NAME_PLUGIN,
            filePath = URLEncoder.encode(filePath, Charsets.UTF_8.name())
        ).data
    }

    override fun textReferenceFileDownload(
        userId: String,
        fileDirPath: String,
        request: TextReferenceFileDownloadRequest
    ) {
        val fileNames = request.fileNames
        fileNames.forEach {
            downloadFile(
                userId,
                "${request.projectCode}$fileSeparator${request.fileDir}${fileSeparator}file$fileSeparator$it",
                File(fileDirPath, it)
            )
        }
        if (!isDirectoryNotEmpty(fileDirPath)) {
            logger.warn(" FAIL|Download file from ${request.storeCode} fail, branch:${request.branch ?: MASTER}")
        }
    }

    @Suppress("NestedBlockDepth")
    override fun uploadFileToPath(
        userId: String,
        result: MutableMap<String, String>,
        localDirectoryInfo: LocalDirectoryInfo
    ): Map<String, String> {
        val fileDirPath = localDirectoryInfo.fileDirPath
        localDirectoryInfo.pathList.forEach { pathInfo ->
            val file = File("$fileDirPath${fileSeparator}${pathInfo.relativePath}")
            try {
                if (file.exists()) {
                    val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
                    val fileUrl = CommonServiceUtils.uploadFileToArtifactories(
                        userId = userId,
                        serviceUrlPrefix = serviceUrlPrefix,
                        file = file,
                        staticFlag = pathInfo.staticFlag,
                        fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ).data
                    fileUrl?.let { result[pathInfo.relativePath] = fileUrl }
                } else {
                    logger.warn("Resource file does not exist:${file.path}")
                }
            } finally {
                file.delete()
            }
        }
        return result
    }
}
