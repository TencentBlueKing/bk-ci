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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 服务端大变量文件读写（制品库 [BuildVarOverflowUtils.REPO_NAME] 仓库）。
 *
 * 大变量的上传 / 下载主要在构建机完成，服务端只处理没有构建机参与的场景：
 * API / 老构建机写入的变量、引擎侧表达式求值，以及用户查看完整值。
 */
@Service
class BuildVarOverflowStorage @Autowired constructor(
    private val bkRepoClient: BkRepoClient,
    private val pipelineVarOverflowConfig: PipelineVarOverflowConfig
) {

    /** 制品库为 bkrepo 时才支持大变量；否则沿用 4K 上限的历史行为。 */
    fun isEnabled(): Boolean = bkRepoClient.useBkRepo()

    /**
     * 一次上传的归属。
     * @property executeCount 写入时构建的执行次数，决定文件所在子目录
     * @property pipelineName / buildNum 流水线、构建目录的展示名，为空时不设置
     */
    data class UploadScope(
        val projectId: String,
        val pipelineId: String,
        val buildId: String,
        val executeCount: Int,
        val pipelineName: String?,
        val buildNum: String?
    )

    /**
     * 上传大变量并返回引用串。调用方需保证 [isEnabled]、值超过 4K 且不超过单值上限。
     * 文件和目录展示名都用系统用户写入。
     */
    fun upload(scope: UploadScope, value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val ref = BuildVarOverflowUtils.LargeVarRef(
            length = value.length,
            executeCount = scope.executeCount.coerceAtLeast(1),
            digest = BuildVarOverflowUtils.sha256Hex(bytes)
        )
        val path = BuildVarOverflowUtils.blobPath(scope.pipelineId, scope.buildId, ref)
        uploadBytes(scope, path, bytes)
        markDisplayName(scope)
        return BuildVarOverflowUtils.toReference(ref)
    }

    /** 首次失败后再试 [UPLOAD_RETRY_TIMES] 次，随后把最后一次异常抛出。 */
    private fun uploadBytes(scope: UploadScope, path: String, bytes: ByteArray) {
        var lastError: Exception? = null
        for (attempt in 0..UPLOAD_RETRY_TIMES) {
            try {
                ensureVariableRepo(scope.projectId)
                bkRepoClient.uploadFile(
                    userId = SYSTEM,
                    projectId = scope.projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    path = path.removePrefix("/"),
                    inputStream = ByteArrayInputStream(bytes)
                )
                return
            } catch (ignored: Exception) {
                lastError = ignored
                if (attempt < UPLOAD_RETRY_TIMES) {
                    logger.warn("${scope.buildId}|LARGE_VAR_UPLOAD_RETRY|path=$path|${ignored.message}")
                }
            }
        }
        throw lastError ?: IllegalStateException("large variable upload failed")
    }

    /**
     * 上传前确认仓库存在。同一项目只确认一次，结果放进 Caffeine。
     * 实际创建走 [BkRepoClient.ensureVariableRepo]：成功报文是 code=0、data 为空；
     * 重名 251007 按已存在处理。确认失败不写入缓存。用制品库默认用户创建，避免构建触发人没有建仓权限。
     */
    fun ensureVariableRepo(projectId: String) {
        variableRepoReady.get(projectId) {
            bkRepoClient.ensureVariableRepo(REPO_CREATE_USER, projectId)
            true
        }
    }

    private fun markDisplayName(scope: UploadScope) {
        val dirs = listOfNotNull(
            scope.pipelineName?.takeIf { it.isNotBlank() }?.let { scope.pipelineId to it },
            scope.buildNum?.takeIf { it.isNotBlank() }?.let { "${scope.pipelineId}/${scope.buildId}" to it }
        )
        dirs.forEach { (dir, displayName) ->
            try {
                bkRepoClient.setMetadata(
                    userId = SYSTEM,
                    projectId = scope.projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    path = dir,
                    metadata = mapOf(METADATA_DISPLAY_NAME to displayName)
                )
            } catch (ignored: Exception) {
                logger.warn("${scope.buildId}|LARGE_VAR_DISPLAY_NAME_FAIL|dir=$dir|${ignored.message}")
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
            val output = BoundedByteArrayOutputStream(pipelineVarOverflowConfig.effectiveLargeValueMaxBytes())
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
        /** 与 artifactory 模块的 BKREPO_DEFAULT_USER 相同，这里不引用该模块。 */
        private const val REPO_CREATE_USER = "admin"
        private const val UPLOAD_RETRY_TIMES = 1
        private const val METADATA_DISPLAY_NAME = "displayName"
        private const val VARIABLE_REPO_READY_MAX = 100_000L

        private val variableRepoReady = Caffeine.newBuilder()
            .maximumSize(VARIABLE_REPO_READY_MAX)
            .build<String, Boolean>()
    }
}
