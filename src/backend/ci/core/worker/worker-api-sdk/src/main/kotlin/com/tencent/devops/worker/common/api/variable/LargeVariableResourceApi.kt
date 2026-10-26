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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.artifactory.constant.REALM_BK_REPO
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.BuildVarOverflowUtils.LargeVarRef
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LARGE_VALUE_MAX_BYTES
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.archive.ArtifactoryBuildResourceApi
import com.tencent.devops.worker.common.api.archive.BkRepoResourceApi
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpRetryException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * 大变量文件读写。上传通道与 [BkRepoReportResourceApi.uploadReport] 一致：
 *
 * - 配置了 bkrepo 文件网关：申请本构建目录的临时令牌，经 `/generic/temporary/{upload|download}/...` 读写，
 *   路径整段 URL 编码（与 [BkRepoResourceApi.uploadFileByToken] 相同）；
 * - 否则经构建网关 `/bkrepo/api/build/generic/...` 直连。
 *
 * 与报告上传的差异：令牌类型是 [TokenType.ALL] 而不是 UPLOAD，因为同一个令牌还要下载。
 * 文件不带 pipelineId / buildId 元数据，也不带后缀。上传成功后给流水线、构建目录设置展示名，失败不影响上传。
 *
 * 只提供单文件读写：一次预取多个大变量时由调用方并行调用 [download]，不走制品库的 zip 批量下载接口
 * （该接口没有临时令牌形态，必须整段路径由构建机自行拼装后经构建网关下发，收敛不了访问范围）。
 */
class LargeVariableResourceApi : AbstractBuildResourceApi(), LargeVariableSDKApi {

    override fun isSupported(): Boolean = supported

    override fun upload(scope: LargeVariableScope, ref: LargeVarRef, content: ByteArray) {
        ensureRepo(scope)
        val fullPath = BuildVarOverflowUtils.blobPath(scope.pipelineId, scope.buildId, ref)
        val body = content.toRequestBody(OctetMediaType)
        // 与报告上传一致：覆盖写、提交到边缘节点，不附带流水线元数据
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
        // 节点创建超时在制品库里是非 2xx，request() 会直接抛 RemoteServiceException。
        // 与 uploadFileByToken 一样，从错误响应体里认出 251030 再交给上传重试。
        val response = try {
            request(request, "upload large variable failed")
        } catch (e: RemoteServiceException) {
            val detail = createNodeTimeoutDetail(e.responseContent)
            if (detail != null) {
                throw HttpRetryException(detail, CODE_CREATE_NODE_TIMEOUT)
            }
            throw e
        }
        val codeNode = runCatching { objectMapper.readTree(response)?.get("code") }.getOrNull()
        if (codeNode != null && codeNode.asInt() == CODE_CREATE_NODE_TIMEOUT) {
            throw HttpRetryException(response, CODE_CREATE_NODE_TIMEOUT)
        }
        if (codeNode != null && codeNode.asText() != "0") {
            throw RemoteServiceException("upload large variable failed: $response")
        }
        // add() 只有第一个线程返回 true，先占位再请求；并发上传时不会重复调用 markDisplayName。
        // 标记失败则撤销占位，交给后续上传重试。
        if (displayNameMarked.add(scope.buildId)) {
            displayNameMarked.retainAll(setOf(scope.buildId))
            if (!markDisplayName(scope)) {
                displayNameMarked.remove(scope.buildId)
            }
        }
    }

    /**
     * 直连上传前确认 variable 仓库存在。同一项目只向 process 确认一次。
     * 创建仍由服务端 BkRepoClient.ensureVariableRepo 完成，构建机不再单独调制品库建仓接口。
     */
    private fun ensureRepo(scope: LargeVariableScope) {
        if (scope.projectId in repoReady) {
            return
        }
        synchronized(repoLock) {
            if (scope.projectId in repoReady) {
                return
            }
            val request = buildPost(path = "/ms/process/api/build/variable/repo/ensure")
            val response = request(request, "ensure variable repo failed")
            val result = objectMapper.readValue<Result<Boolean>>(response)
            if (result.isNotOk()) {
                throw RemoteServiceException("ensure variable repo failed: ${result.message ?: response}")
            }
            repoReady.add(scope.projectId)
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

    /**
     * 与 [BkRepoResourceApi.setPipelineMetadata] 一致：流水线目录展示流水线名，构建目录展示构建号。
     * @return 全部设置成功（或没有需要设置的目录）时为 true，失败时下次上传再试
     */
    private fun markDisplayName(scope: LargeVariableScope): Boolean {
        val dirs = listOfNotNull(
            scope.pipelineName?.takeIf { it.isNotBlank() }?.let { "/${scope.pipelineId}" to it },
            scope.buildNum?.takeIf { it.isNotBlank() }?.let { "/${scope.pipelineId}/${scope.buildId}" to it }
        )
        var success = true
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
                success = false
                logger.warn("LARGE_VAR_DISPLAY_NAME_FAIL|$dir|${ignore.message}")
            }
        }
        return success
    }

    /** 响应体里的创建节点超时文案；不是该错误时返回 null。 */
    private fun createNodeTimeoutDetail(responseContent: String?): String? {
        if (responseContent.isNullOrBlank()) return null
        val node = runCatching { objectMapper.readTree(responseContent) }.getOrNull() ?: return null
        if (node.get("code")?.asInt() != CODE_CREATE_NODE_TIMEOUT) return null
        return node.get("message")?.asText()?.takeIf { it.isNotBlank() } ?: responseContent
    }

    private fun directPath(scope: LargeVariableScope, fullPath: String) =
        "/bkrepo/api/build/generic/${scope.projectId}/${BuildVarOverflowUtils.REPO_NAME}$fullPath"

    /** 与 [BkRepoResourceApi.uploadFileByToken] 相同：整段 fullPath 做 URL 编码，斜杠变成 %2F。 */
    private fun temporaryPath(action: String, scope: LargeVariableScope, fullPath: String, token: String): String {
        val encoded = URLEncoder.encode(fullPath, "UTF-8").replace("+", "%20")
        return "/generic/temporary/$action/${scope.projectId}/${BuildVarOverflowUtils.REPO_NAME}/$encoded?token=$token"
    }

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
        private const val CODE_CREATE_NODE_TIMEOUT = 251030

        private val logger = LoggerFactory.getLogger(LargeVariableResourceApi::class.java)

        private val bkRepoResourceApi = BkRepoResourceApi()

        private val displayNameMarked: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private val supported: Boolean by lazy {
            ArtifactoryBuildResourceApi().getRealm() == REALM_BK_REPO
        }

        private val tokenAccess: Boolean by lazy { bkRepoResourceApi.tokenAccess() }

        private val tokens = ConcurrentHashMap<String, RepoToken>()
        private val tokenLock = Any()
        private val repoReady = ConcurrentHashMap.newKeySet<String>()
        private val repoLock = Any()
    }
}
