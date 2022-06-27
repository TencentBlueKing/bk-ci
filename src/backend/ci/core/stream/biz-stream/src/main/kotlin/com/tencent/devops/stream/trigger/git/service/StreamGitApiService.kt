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

package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCommitInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.StreamGitFileInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitMrChangeInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitMrInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitProjectInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitProjectUserInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitTreeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitUserInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamRevisionInfo
import com.tencent.devops.stream.util.RetryUtils
import org.slf4j.LoggerFactory

/**
 * Stream 需要用到的各平台的标准接口
 * 注：在代码主流程中直接使用api的方法需要放到这里，只在各个action中的可以在其具体的GitApiService中去实现
 * 注2：只应该在trigger相关逻辑中使用此接口，其他地方使用scm相关
 */
interface StreamGitApiService {

    /**
     * 通过传递的凭据信息直接获取实体token，方便直接调用接口，类似commit check之类的
     * @param cred 调用api相关凭证
     */
    fun getToken(cred: StreamGitCred): String

    /**
     * 获取stream需要的git项目信息
     * 当前api加入缓存，需要调用优先从缓存获取
     * @see com.tencent.devops.stream.trigger.parsers.StreamTriggerCache.getAndSaveRequestGitProjectInfo
     * @param gitProjectId git项目唯一标识
     * @param retry 当前请求重试的相关信息
     */
    fun getGitProjectInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): StreamGitProjectInfo?

    /**
     * 获取stream需要的git commit相关信息
     * @param sha 获取commit的信息，例如：hash值、分支名或tag
     */
    fun getGitCommitInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): StreamGitCommitInfo?

    /**
     * 根据token获取用户信息
     * TODO: 后续多源可以看是否放到具体Git平台的实现中
     */
    fun getUserInfoByToken(
        cred: StreamGitCred
    ): StreamGitUserInfo?

    /**
     * 获取项目下用户的信息(带权限)
     * @param userId git用户的唯一标识
     * TODO: 后续多源可以看是否放到具体Git平台的实现中
     */
    fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): StreamGitProjectUserInfo

    /**
     * 获取合并请求信息
     * @param mrId 合并请求的唯一凭证
     * TODO: 后续多源可以看是否放到具体Git平台的实现中
     */
    fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): StreamGitMrInfo?

    /**
     * 获取合并请求信息包括变更文件
     */
    fun getMrChangeInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): StreamGitMrChangeInfo?

    /**
     * 获取Git仓库文件列表
     * @param path 获取文件路径下的文件列表
     * @param ref commit hash值、分支 或 tag
     * @param recursive 是否支持递归目录结构
     * TODO: 后续多源可以看是否放到具体Git平台的实现中
     */
    fun getFileTree(
        cred: StreamGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<StreamGitTreeFileInfo>

    /**
     * 获取yaml文件的具体内容
     * @param fileName 文件名称
     */
    fun getFileContent(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String,
        retry: ApiRequestRetryInfo
    ): String

    /**
     * 获取yaml文件内容以及文件信息
     * TODO: 后续多源可以看是否放到具体Git平台的实现中
     */
    fun getFileInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): StreamGitFileInfo?

    /**
     * 获取项目列表
     */
    fun getProjectList(
        cred: StreamGitCred,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<StreamGitProjectInfo>?

    fun getLatestRevision(
        pipelineId: String,
        projectName: String,
        gitUrl: String,
        branch: String,
        userName: String,
        enableUserId: String,
        retry: ApiRequestRetryInfo
    ): StreamRevisionInfo?

    fun <T> doRetryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        return if (retry.retry) {
            retryFun(
                retry = retry,
                log = log,
                apiErrorCode = apiErrorCode
            ) {
                action()
            }
        } else {
            action()
        }
    }

    private fun <T> retryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        try {
            return RetryUtils.clientRetry(
                retry.retryTimes,
                retry.retryPeriodMills
            ) {
                action()
            }
        } catch (e: ClientException) {
            logger.warn("retry 5 times $log", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.formatErrorMessage
            )
        } catch (e: RemoteServiceException) {
            logger.warn("GIT_API_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("GIT_SCM_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.message}"
            )
        } catch (ignore: Throwable) {
            logger.error("retryFun error $log", ignore)
            throw ErrorCodeException(
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = if (ignore.message.isNullOrBlank()) {
                    "$log: ${apiErrorCode.formatErrorMessage}"
                } else {
                    "$log: ${ignore.message}"
                }
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamGitApiService::class.java)
    }
}
