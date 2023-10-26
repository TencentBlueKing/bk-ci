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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.pojo.common.TextReferenceFileParseRequest
import com.tencent.devops.store.utils.AtomReleaseTxtAnalysisUtil
import java.io.File
import java.net.URLEncoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils

@Service
class TxStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    @Value("\${store.bkrepo.projectId:bk-store}")
    private val bkrepoStoreProjectId: String = "bk-store"

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreI18nMessageServiceImpl::class.java)
        private val fileSeparator: String = System.getProperty("file.separator")
    }

    override fun getFileStr(
        projectCode: String,
        fileDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        logger.info("getFileStr repositoryHashId:$repositoryHashId")
        return if (!repositoryHashId.isNullOrBlank()) {
            // 从工蜂拉取文件
            try {
                client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryHashId,
                    filePath = fileName,
                    reversion = null,
                    branch = branch,
                    repositoryType = null
                ).data
            } catch (ignored: Throwable) {
                logger.warn("getPropertiesFileStr fileName:$fileName,branch:$branch error", ignored)
                null
            }
        } else {
            try {
                // 直接从仓库拉取文件
                val filePath =
                    URLEncoder.encode("$projectCode/$fileDir/$fileName", Charsets.UTF_8.name())
                return client.get(ServiceArtifactoryResource::class).getFileContent(
                    userId = BKREPO_DEFAULT_USER,
                    projectId = bkrepoStoreProjectId,
                    repoName = BkRepoEnum.PLUGIN.repoName,
                    filePath = filePath
                ).data
            } catch (ignored: Throwable) {
                logger.warn("getPropertiesFileStr ffilePath:${"$projectCode/$fileDir/$fileName"} error", ignored)
                null
            }
        }
    }

    override fun downloadFile(
        filePath: String,
        file: File,
        repositoryHashId: String?,
        branch: String?,
        format: String?
    ) {
        val serviceUrl = client.getServiceUrl(ServiceGitResource::class)
        val url = "$serviceUrl/service/git/downloadGitRepoFile?" +
                "repoId=$repositoryHashId&repositoryType=${RepositoryType.ID.name}&sha=${branch ?: MASTER}" +
                "&tokenType=${TokenTypeEnum.OAUTH.name}" +
                "&filePath=${URLEncoder.encode(filePath, Charsets.UTF_8.name())}" +
                "&format=${format ?: "zip"}"
        OkhttpUtils.downloadFile(url, file)
    }

    override fun getFileNames(
        projectCode: String,
        fileDir: String,
        i18nDir: String?,
        repositoryHashId: String?,
        branch: String?
    ): List<String>? {
        return if (!repositoryHashId.isNullOrBlank()) {
            val gitRepositoryDirItems = client.get(ServiceGitRepositoryResource::class).getGitRepositoryTreeInfo(
                userId = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
                repoId = repositoryHashId,
                refName = branch,
                path = i18nDir,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            ).data
            gitRepositoryDirItems?.filter { it.type != "tree" }?.map { it.name }
        } else {
            val filePath = URLEncoder.encode("$projectCode/$fileDir/$i18nDir", Charsets.UTF_8.name())
            client.get(ServiceArtifactoryResource::class).listFileNamesByPath(
                userId = BKREPO_DEFAULT_USER,
                projectId = bkrepoStoreProjectId,
                repoName = BkRepoEnum.PLUGIN.repoName,
                filePath = filePath
            ).data
        }
    }

    override fun textReferenceFileAnalysis(
        userId: String,
        projectCode: String,
        request: TextReferenceFileParseRequest
    ): String {
        if (request.repositoryHashId.isNullOrBlank()) return request.content
        var result = request.content
        val fileDirPath = AtomReleaseTxtAnalysisUtil.buildAtomArchivePath(
            userId = userId,
            atomDir = request.fileDir
        )
        val uuid = UUIDUtil.generate()
        val file = File(fileDirPath, "$uuid.zip")
        try {
            downloadFile(
                filePath = "file",
                file = file,
                repositoryHashId = request.repositoryHashId,
                branch = request.branch
            )
            if (file.exists()) {
                ZipUtil.unZipFile(file, fileDirPath, true)
                result = storeFileService.textReferenceFileAnalysis(
                    userId = userId,
                    content = request.content,
                    client = client,
                    fileDirPath = "$fileDirPath$fileSeparator$uuid${fileSeparator}file"
                )
            } else {
                logger.warn("textReferenceFileAnalysis file is not  exists path:${file.path}")
            }
        } catch (ignored: Throwable) {
            logger.warn("BKSystemErrorMonitor|parse atom file fail|error=${ignored.message}")
        } finally {
            file.delete()
            FileSystemUtils.deleteRecursively(File(fileDirPath).parentFile)
        }
        return result
    }
}
