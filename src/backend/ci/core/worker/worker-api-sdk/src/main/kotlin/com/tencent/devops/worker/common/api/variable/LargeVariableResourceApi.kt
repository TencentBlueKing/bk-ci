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

package com.tencent.devops.worker.common.api.variable

import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.BuildVarOverflowUtils.LargeVarRef
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.archive.ArtifactoryBuildResourceApi
import com.tencent.devops.worker.common.api.archive.BkRepoResourceApi
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

/**
 * 大变量文件读写，与 [com.tencent.devops.worker.common.utils.ArchiveUtils.archivePipelineFiles] 的制品库对接方式一致：
 *
 * - 配置了 bkrepo 文件网关（[BkRepoResourceApi.tokenAccess]）：申请本构建大变量目录的临时令牌，
 *   经文件网关 `/generic/temporary/{upload|download}/...` 读写；
 * - 否则经构建网关 `/bkrepo/api/build/generic/...` 直连。
 *
 * 批量下载没有临时令牌形态，统一经构建网关 `POST /bkrepo/api/build/generic/batch/{projectId}/{repo}`；
 * 失败或缺失的文件由调用方逐个下载兜底（单个下载仍按上面的规则选择通道）。
 *
 * 上传不携带 pipelineId / buildId 等元数据，避免大变量文件出现在构建制品列表中；
 * 但与构件归档一样给流水线、构建目录设置展示名（流水线名 / 构建号）。
 */
class LargeVariableResourceApi : AbstractBuildResourceApi(), LargeVariableSDKApi {

    override fun isSupported(): Boolean = supported

    override fun upload(scope: LargeVariableScope, ref: LargeVarRef, content: ByteArray) {
        val fullPath = BuildVarOverflowUtils.blobPath(scope.pipelineId, scope.buildId, ref)
        val body = content.toRequestBody(OctetMediaType)
        val headers = mapOf(BKREPO_UID to scope.userId, BKREPO_OVERWRITE to "true", BKREPO_COMMIT_EDGE to "true")
        val token = repoToken(scope)
        val request = if (token == null) {
            buildPut(path = directPath(scope, fullPath), requestBody = body, headers = headers)
        } else {
            buildPut(
                path = temporaryPath("upload", scope, fullPath, token),
                requestBody = body,
                headers = headers,
                useFileDevnetGateway = scope.vmBuildEnv
            )
        }
        val response = request(request, "upload large variable failed")
        val code = objectMapper.readTree(response)?.get("code")?.asText()
        if (code != null && code != "0") {
            throw RemoteServiceException("upload large variable failed: $response")
        }
        if (displayNameMarked.add(scope.buildId)) {
            markDisplayName(scope)
        }
    }

    override fun download(scope: LargeVariableScope, ref: LargeVarRef): ByteArray? {
        val fullPath = BuildVarOverflowUtils.blobPath(scope.pipelineId, scope.buildId, ref)
        val headers = mapOf(BKREPO_UID to scope.userId)
        val token = repoToken(scope)
        val request = if (token == null) {
            buildGet(path = directPath(scope, fullPath), headers = headers)
        } else {
            buildGet(
                path = temporaryPath("download", scope, fullPath, token),
                headers = headers,
                useFileDevnetGateway = scope.vmBuildEnv
            )
        }
        return fetch(request, "download large variable failed") { readBounded(it) }
    }

    override fun batchDownload(scope: LargeVariableScope, refs: Collection<LargeVarRef>): Map<String, ByteArray> {
        if (refs.isEmpty()) return emptyMap()
        val distinctRefs = refs.distinctBy { it.digest }
        val wanted = distinctRefs.mapTo(HashSet()) { it.digest }
        val paths = distinctRefs.map { BuildVarOverflowUtils.blobPath(scope.pipelineId, scope.buildId, it) }
        val request = buildPost(
            path = "/bkrepo/api/build/generic/batch/${scope.projectId}/${BuildVarOverflowUtils.REPO_NAME}",
            requestBody = getJsonRequest(mapOf("paths" to paths)),
            headers = mapOf(BKREPO_UID to scope.userId)
        )
        return fetch(request, "batch download large variables failed") { unzip(it, wanted) } ?: emptyMap()
    }

    /** 404 返回 null，其它非 2xx 抛异常。 */
    private fun <T> fetch(request: Request, errorMessage: String, reader: (InputStream) -> T): T? {
        requestForResponse(request).use { response ->
            if (response.code == HTTP_NOT_FOUND) return null
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    errorMessage = errorMessage,
                    httpStatus = response.code,
                    responseContent = response.body?.string()
                )
            }
            return response.body!!.byteStream().use(reader)
        }
    }

    private fun unzip(input: InputStream, wanted: Set<String>): Map<String, ByteArray> {
        val result = HashMap<String, ByteArray>(wanted.size)
        ZipInputStream(input).use { zip ->
            while (result.size < wanted.size) {
                val entry = zip.nextEntry ?: break
                // 条目名可能是完整路径或相对路径，文件名即摘要
                val digest = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && digest in wanted && digest !in result) {
                    result[digest] = readBounded(zip)
                }
            }
        }
        return result
    }

    /** 与 [BkRepoResourceApi.setPipelineMetadata] 一致：流水线目录展示流水线名，构建目录展示构建号；失败不影响上传。 */
    private fun markDisplayName(scope: LargeVariableScope) {
        val dirs = listOfNotNull(
            scope.pipelineName?.takeIf { it.isNotBlank() }?.let { "/${scope.pipelineId}" to it },
            scope.buildNum?.takeIf { it.isNotBlank() }?.let { "/${scope.pipelineId}/${scope.buildId}" to it }
        )
        dirs.forEach { (dir, displayName) ->
            try {
                bkRepoResourceApi.saveMetadata(
                    userId = scope.userId,
                    projectId = scope.projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    fullPath = dir,
                    metadata = mapOf(METADATA_DISPLAY_NAME to displayName)
                )
            } catch (ignore: Exception) {
                logger.warn("LARGE_VAR_DISPLAY_NAME_FAIL|$dir|${ignore.message}")
            }
        }
    }

    private fun directPath(scope: LargeVariableScope, fullPath: String) =
        "/bkrepo/api/build/generic/${scope.projectId}/${BuildVarOverflowUtils.REPO_NAME}$fullPath"

    private fun temporaryPath(action: String, scope: LargeVariableScope, fullPath: String, token: String) =
        "/generic/temporary/$action/${scope.projectId}/${BuildVarOverflowUtils.REPO_NAME}$fullPath?token=$token"

    /**
     * 文件网关模式下返回本构建大变量目录的读写令牌（进程内复用，临近过期前重新申请）；直连模式返回 null。
     */
    private fun repoToken(scope: LargeVariableScope): String? {
        if (!tokenAccess) return null
        val now = System.currentTimeMillis()
        tokens[scope.buildId]?.takeIf { it.validUntil > now }?.let { return it.token }
        synchronized(tokenLock) {
            tokens[scope.buildId]?.takeIf { it.validUntil > now }?.let { return it.token }
            val token = HttpRetryUtils.retry(retryTime = TOKEN_RETRY_TIMES, retryPeriodMills = TOKEN_RETRY_PERIOD) {
                bkRepoResourceApi.createBkRepoTemporaryToken(
                    userId = scope.userId,
                    projectId = scope.projectId,
                    repoName = BuildVarOverflowUtils.REPO_NAME,
                    path = BuildVarOverflowUtils.blobDir(scope.pipelineId, scope.buildId),
                    type = TokenType.ALL,
                    expireSeconds = TOKEN_EXPIRE_SECONDS
                )
            }
            tokens.keys.retainAll(setOf(scope.buildId))
            tokens[scope.buildId] = RepoToken(token, now + (TOKEN_EXPIRE_SECONDS - TOKEN_RENEW_AHEAD_SECONDS) * 1000)
            return token
        }
    }

    private fun readBounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (out.size() + n > PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES) {
                throw RemoteServiceException("large variable exceeds $PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES bytes")
            }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private data class RepoToken(val token: String, val validUntil: Long)

    companion object {
        private const val BKREPO_UID = "X-BKREPO-UID"
        private const val BKREPO_OVERWRITE = "X-BKREPO-OVERWRITE"
        private const val BKREPO_COMMIT_EDGE = "X-BKREPO-COMMIT-EDGE"
        private const val HTTP_NOT_FOUND = 404
        private const val BUFFER_SIZE = 8192
        private const val TOKEN_EXPIRE_SECONDS = 4 * 3600L
        private const val TOKEN_RENEW_AHEAD_SECONDS = 600L
        private const val TOKEN_RETRY_TIMES = 3
        private const val TOKEN_RETRY_PERIOD = 1000L
        private const val METADATA_DISPLAY_NAME = "displayName"

        private val logger = LoggerFactory.getLogger(LargeVariableResourceApi::class.java)

        private val bkRepoResourceApi = BkRepoResourceApi()

        private val displayNameMarked: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val supported: Boolean by lazy {
            ArtifactoryBuildResourceApi().getRealm() == REALM_BK_REPO
        }

        private val tokenAccess: Boolean by lazy { bkRepoResourceApi.tokenAccess() }

        private val tokens = ConcurrentHashMap<String, RepoToken>()
        private val tokenLock = Any()
    }
}
