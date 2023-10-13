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

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.constant.REPO_NAME_PLUGIN
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.store.pojo.common.TextReferenceFileParseRequest
import com.tencent.devops.store.utils.AtomReleaseTxtAnalysisUtil
import java.io.File
import java.net.URLEncoder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SampleStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    companion object {
        private val logger = LoggerFactory.getLogger(SampleStoreI18nMessageServiceImpl::class.java)
    }

    override fun getFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        val filePath =
            URLEncoder.encode("$projectCode/$fileDir/$fileName", Charsets.UTF_8.name())
        return client.get(ServiceArtifactoryResource::class).getFileContent(
            userId = BKREPO_DEFAULT_USER,
            projectId = BKREPO_STORE_PROJECT_ID,
            repoName = REPO_NAME_PLUGIN,
            filePath = filePath
        ).data
    }

    override fun downloadFile(
        filePath: String,
        file: File,
        repositoryHashId: String?,
        branch: String?,
        format: String?
    ) {
        val url = client.getServiceUrl(ServiceArchiveAtomResource::class) +
                "/service/artifactories/atom/file/download?filePath=${URLEncoder.encode(filePath, "UTF-8")}"
        logger.info("downloadFile filePath:$filePath")
        OkhttpUtils.downloadFile(url, file)
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
            userId = BKREPO_DEFAULT_USER,
            projectId = BKREPO_STORE_PROJECT_ID,
            repoName = REPO_NAME_PLUGIN,
            filePath = URLEncoder.encode(filePath, Charsets.UTF_8.name())
        ).data
    }

    override fun textReferenceFileAnalysis(
        userId: String,
        projectCode: String,
        request: TextReferenceFileParseRequest
    ): String {
        val separator = File.separator
        val fileNameList = getFileNames(
            projectCode = projectCode,
            fileDir = "${request.fileDir}${separator}file"
        )
        if (fileNameList.isNullOrEmpty()) {
            logger.info("descriptionAnalysis get fileNameList fail")
            return request.content
        }
        val fileDirPath = AtomReleaseTxtAnalysisUtil.buildAtomArchivePath(
            userId = userId,
            atomDir = request.fileDir
        )
        fileNameList.forEach {
            downloadFile(
                "$projectCode$separator${request.fileDir}${separator}file$separator$it",
                File("$fileDirPath${separator}file", it)
            )
        }

        return storeFileService.descriptionAnalysis(
            fileDirPath = "$fileDirPath${separator}file",
            userId = userId,
            description = request.content,
            client = client
        )
    }
}
