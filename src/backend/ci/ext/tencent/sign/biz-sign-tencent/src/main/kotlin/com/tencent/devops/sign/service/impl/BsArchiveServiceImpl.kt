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

package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.archive.client.DirectBkRepoClient
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.utils.IpaIconUtil
import com.tencent.devops.sign.utils.sha256
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.net.SocketTimeoutException

@Service
class BsArchiveServiceImpl @Autowired constructor(
    private val directBkRepoClient: DirectBkRepoClient,
    private val commonConfig: CommonConfig,
    private val profile: Profile
) : ArchiveService {

    override fun archive(
        signedIpaFile: File,
        ipaSignInfo: IpaSignInfo,
        properties: MutableMap<String, String>?
    ): Boolean {
        logger.info("archive, signedIpaFile: ${signedIpaFile.absolutePath}, ipaSignInfo: $ipaSignInfo, properties: $properties")
        val path = if (ipaSignInfo.archiveType.toLowerCase() == "pipeline") {
            "${ipaSignInfo.pipelineId}/${ipaSignInfo.buildId}/${signedIpaFile.name}"
        } else {
            "${ipaSignInfo.archivePath}/${signedIpaFile.name}"
        }

        // icon图标
        try {
            if (null != properties) {
                val resolveIpaIcon = IpaIconUtil.resolveIpaIcon(signedIpaFile)
                if (null != resolveIpaIcon) {
                    val sha256 = resolveIpaIcon.inputStream().sha256()
                    val url = directBkRepoClient.uploadByteArray(
                        userId = "app-icon",
                        projectId = getIconProject(),
                        repoName = getIconRepo(),
                        path = "/app-icon/ipa/$sha256.png",
                        byteArray = resolveIpaIcon
                    )
                    properties["appIcon"] = url
                }
            }
        } catch (e: SocketTimeoutException) {
            logger.error("bk repo upload icon with timeout, need retry", e)
            throw e
        } catch (ignored: Throwable) {
            logger.warn("load icon of ipa failed with error.", ignored)
        }

        try {
            directBkRepoClient.uploadLocalFile(
                userId = ipaSignInfo.userId,
                projectId = ipaSignInfo.projectId,
                repoName = ipaSignInfo.archiveType.toLowerCase(),
                path = path,
                file = signedIpaFile,
                metadata = properties ?: mapOf(),
                override = true
            )
        } catch (e: SocketTimeoutException) {
            logger.error("bk repo upload file with timeout, need retry", e)
            throw e
        } catch (e: Exception) {
            logger.error("archive upload file with error. path:$path", e)
            return false
        }
        return true
    }

    private fun getIconProject(): String {
        return if (profile.isDev()) {
            "repo-dev-test"
        } else {
            "bkdevops"
        }
    }

    private fun getIconRepo(): String {
        return if (profile.isDev()) {
            "public"
        } else {
            "app-icon"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BsArchiveServiceImpl::class.java)

        private const val BKREPO_OVERRIDE = "X-BKREPO-OVERWRITE"
        private const val BKREPO_UID = "X-BKREPO-UID"
    }
}
