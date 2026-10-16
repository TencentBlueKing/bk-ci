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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 服务端大变量文件读写（制品库流水线仓库 [BuildVarOverflowUtils.REPO_NAME]）。
 *
 * 大变量的上传 / 下载主要在构建机完成，服务端只处理没有构建机参与的场景：
 * API / 老构建机写入的变量、引擎侧表达式求值，以及用户查看完整值。
 */
@Service
class BuildVarOverflowStorage @Autowired constructor(
    private val bkRepoClient: BkRepoClient
) {

    /** 制品库为 bkrepo 时才支持大变量；否则沿用 4K 上限的历史行为。 */
    fun isEnabled(): Boolean = bkRepoClient.useBkRepo()

    /**
     * 写入目标。
     * @property executeCount 写入时构建的执行次数，决定文件所在子目录
     * @property pipelineName / buildNum 流水线、构建目录的展示名（与构件归档一致），为空时不设置
     */
    data class UploadTarget(val executeCount: Int, val pipelineName: String?, val buildNum: String?)

    /**
     * 上传大变量并返回引用串。调用方需保证 [isEnabled]、值超过 4K 且不超过单值上限。
     */
    @Suppress("LongParameterList")
    fun upload(
        userId: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        target: UploadTarget,
        value: String
    ): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val ref = BuildVarOverflowUtils.LargeVarRef(
            length = value.length,
            executeCount = target.executeCount.coerceAtLeast(1),
            digest = BuildVarOverflowUtils.sha256Hex(bytes)
        )
        val path = BuildVarOverflowUtils.blobPath(pipelineId, buildId, ref)
        var attempt = 0
        while (true) {
            try {
                bkRepoClient.uploadFile(
                    userId = userId.orSystem(),
                    projectId = projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    path = path.removePrefix("/"),
                    inputStream = ByteArrayInputStream(bytes)
                )
                break
            } catch (ignored: Exception) {
                if (++attempt > UPLOAD_RETRY_TIMES) throw ignored
                logger.warn("$buildId|LARGE_VAR_UPLOAD_RETRY|path=$path|${ignored.message}")
            }
        }
        markDisplayName(userId.orSystem(), projectId, pipelineId, buildId, target)
        return BuildVarOverflowUtils.toReference(ref)
    }

    private fun markDisplayName(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        target: UploadTarget
    ) {
        val dirs = listOfNotNull(
            target.pipelineName?.takeIf { it.isNotBlank() }?.let { pipelineId to it },
            target.buildNum?.takeIf { it.isNotBlank() }?.let { "$pipelineId/$buildId" to it }
        )
        dirs.forEach { (dir, displayName) ->
            try {
                bkRepoClient.setMetadata(
                    userId = userId,
                    projectId = projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    path = dir,
                    metadata = mapOf(METADATA_DISPLAY_NAME to displayName)
                )
            } catch (ignored: Exception) {
                logger.warn("$buildId|LARGE_VAR_DISPLAY_NAME_FAIL|dir=$dir|${ignored.message}")
            }
        }
    }

    /**
     * 按引用串下载真实值；引用无法解析、文件不存在、内容校验失败或下载异常时返回 null。
     */
    fun download(
        userId: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        reference: String
    ): String? {
        val ref = BuildVarOverflowUtils.parseReference(reference) ?: return null
        if (!isEnabled()) return null
        val path = BuildVarOverflowUtils.blobPath(pipelineId, buildId, ref)
        return try {
            val output = BoundedByteArrayOutputStream(PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES)
            bkRepoClient.downloadFile(
                userId = userId.orSystem(),
                projectId = projectId,
                repoName = BuildVarOverflowUtils.REPO_NAME,
                fullPath = path,
                outputStream = output
            )
            BuildVarOverflowUtils.decodeVerified(ref, output.toByteArray()) ?: run {
                logger.warn("$buildId|LARGE_VAR_DIGEST_MISMATCH|path=$path")
                null
            }
        } catch (ignored: Exception) {
            logger.warn("$buildId|LARGE_VAR_DOWNLOAD_FAIL|path=$path|${ignored.message}")
            null
        }
    }

    private fun String?.orSystem() = this?.takeIf { it.isNotBlank() } ?: SYSTEM

    private class BoundedByteArrayOutputStream(private val maxBytes: Int) : ByteArrayOutputStream() {
        override fun write(b: Int) {
            ensureCapacity(1)
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            ensureCapacity(len)
            super.write(b, off, len)
        }

        private fun ensureCapacity(len: Int) {
            if (count + len > maxBytes) throw IOException("large variable exceeds $maxBytes bytes")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildVarOverflowStorage::class.java)
        private const val UPLOAD_RETRY_TIMES = 1
        private const val METADATA_DISPLAY_NAME = "displayName"
    }
}
