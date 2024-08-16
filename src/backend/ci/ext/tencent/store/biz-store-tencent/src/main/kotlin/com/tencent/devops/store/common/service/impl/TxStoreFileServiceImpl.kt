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
package com.tencent.devops.store.common.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceBkRepoStaticResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.pojo.LocalDirectoryInfo
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceTGitResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.common.service.StoreFileService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.TextReferenceFileDownloadRequest
import java.io.File
import java.net.URLEncoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
@Suppress("ALL")
class TxStoreFileServiceImpl : StoreFileService() {

    @Value("\${store.bkrepo.projectId:bk-store}")
    private val bkrepoStoreProjectId: String = "bk-store"

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreFileServiceImpl::class.java)
    }
    override fun uploadFileToPath(
        userId: String,
        result: MutableMap<String, String>,
        localDirectoryInfo: LocalDirectoryInfo
    ): Map<String, String> {
        val pathList = localDirectoryInfo.pathList
        pathList.forEach { pathInfo ->
            val file = File("${localDirectoryInfo.fileDirPath}$fileSeparator${pathInfo.relativePath}")
            if (file.exists()) {
                val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoStaticResource::class)
                val fileUrl = serviceUploadFile(
                    userId = userId,
                    serviceUrlPrefix = serviceUrlPrefix,
                    file = file
                ).data
                fileUrl?.let { result[pathInfo.relativePath] = StoreUtils.removeUrlHost(fileUrl) }
            } else {
                logger.warn("Resource file does not exist:${file.path}")
            }
        }
        logger.info("uploadFileToPath result:$result")
        return result
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

    override fun textReferenceFileDownload(
        userId: String,
        fileDirPath: String,
        request: TextReferenceFileDownloadRequest
    ) {
        request.fileNames.forEach {
            try {
                downloadFile(
                    userId = userId,
                    filePath = "file$fileSeparator$it",
                    file = File(fileDirPath, it),
                    repositoryHashId = request.repositoryHashId,
                    branch = request.branch
                )
            } catch (ignored: Throwable) {
                logger.warn("BKSystemErrorMonitor|parse store file[$it] fail|error=${ignored.message}")
            }
        }
    }

    override fun downloadFile(
        userId: String,
        filePath: String,
        file: File,
        repositoryHashId: String?,
        branch: String?,
        format: String?
    ) {
        val serviceUrl = client.getServiceUrl(ServiceTGitResource::class)
        val url = "$serviceUrl/service/tgit/downloadGitFile?repoId=$repositoryHashId" +
                "&filePath=$filePath&authType=${RepoAuthType.OAUTH}&ref=${branch ?: MASTER}"
        OkhttpUtils.downloadFile(url, file)
    }

    private fun serviceUploadFile(
        userId: String,
        serviceUrlPrefix: String,
        file: File
    ): Result<String> {
        val index = file.path.indexOf(BK_CI_ATOM_DIR)
        val serviceUrl = "$serviceUrlPrefix/service/bkrepo/statics/file/upload" +
                "?userId=$userId&destPath=${file.path.substring(index)}"
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                val message = I18nUtil.getCodeLanMessage(messageCode = CommonMessageCode.SYSTEM_ERROR)
                Result(CommonMessageCode.SYSTEM_ERROR.toInt(), message, null)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String>>() {})
        }
    }
}
