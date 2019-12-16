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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.worker.task.sign

import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_IPA_SIGN_STATUS
import com.tencent.devops.common.pipeline.element.IosSJTYSignElement
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.ios.sign.IOSSignApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.IosUtils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@TaskClassType(classTypes = [IosSJTYSignElement.classType])
class SJTYSignTask : ITask() {

    companion object {
        private val IP_LIST = if (AgentEnv.isProd()) listOf("9.30.7.110", "9.30.7.46")
        else listOf("9.30.7.110", "9.30.7.46")
    }

    private val iosSignApi = IOSSignApi()

    override fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {

        val taskParams = buildTask.params ?: mapOf()
        val ipaFiles = taskParams["ipaFile"] ?: throw TaskExecuteException(
            errorMsg = "ipaFile is empty",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val customPath = taskParams["destPath"]
        val isCustomize = taskParams["customize"]
        val certId = taskParams["certId"] ?: throw TaskExecuteException(
            errorMsg = "certId is empty",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val repoType = if (isCustomize != null && isCustomize.toBoolean()) {
            1
        } else {
            0
        }

        // 逗号或分号分割
        ipaFiles.split(",").forEach {
            it.split(";").forEach file@{ ipaFile ->
                val files = FileUtil.matchFiles(workspace, ipaFile.trim())
                if (files.isEmpty()) {
                    LoggerService.addYellowLine("no ipa file found in: $ipaFile")
                    return
                }
//                val gatewayDomain = System.getProperty("soda.gateway", "gw.devops.oa.com")
                val gatewayDomain = AgentEnv.getGateway()
                val newSignMethod = gatewayDomain.contains("devgw")
                files.forEach { file ->
                    uploadIpa(file, buildVariables, newSignMethod, gatewayDomain, repoType, customPath, certId)
                }
            }
        }
    }

    private fun uploadIpa(
        file: File,
        buildVariables: BuildVariables,
        newSignMethod: Boolean,
        gatewayDomain: String,
        repoType: Int,
        customPath: String?,
        certId: String
    ) {
        LoggerService.addNormalLine("start to sign ipa file: ${file.canonicalPath}")

        val props = URLEncoder.encode(getProps(file, buildVariables), "utf-8")

        var isException = true
        val tryTime = 2
        var remain = tryTime
        // 重试2次
        while (isException && remain > 0) {
            try {
                if (newSignMethod) {
                    iosSignApi.uploadIpa(file, props, repoType, customPath, certId, 2) // p12Id=1为深圳科技
                    return // 成功退出
                } else {
                    IP_LIST.shuffled().forEach { ip ->
                        val url =
                            "http://$ip/upload?projectId=${buildVariables.projectId}&pipelineId=${buildVariables.pipelineId}&" +
                                "buildId=${buildVariables.buildId}&size=${file.length()}&md5=${FileUtil.getMD5(file)}&" +
                                "env=${AgentEnv.getEnv().name.toLowerCase()}&properties=$props&repoType=$repoType&customPath=$customPath&certId=$certId&p12Id=2"
                        val fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file)
                        val requestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", file.name, fileBody)
                            .build()
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        val timeout = (1 + file.length() / 1024 / 1024 / 1024) * 14 // 每G给14分钟，再增加14分钟做签名。
                        val okHttpClient = OkHttpClient.Builder()
                            .connectTimeout(100, TimeUnit.SECONDS)
                            .writeTimeout(timeout, TimeUnit.MINUTES)
                            .readTimeout(timeout, TimeUnit.MINUTES)
                            .build()

                        okHttpClient.newCall(request).execute().use { response ->
                            val data = response.body()!!.string().trim()
                            if (data != "success") {
                                LoggerService.addRedLine("response data: $data")
                            } else {
                                LoggerService.addNormalLine("response data: $data")
                            }

                            if (!response.isSuccessful || data != "success") {
                                LoggerService.addRedLine("enterprise sign ($file) fail in ip(${SecurityUtil.encrypt(ip)})")
                                throw TaskExecuteException(
                                    errorMsg = "enterprise sign ($file) fail in ip(${SecurityUtil.encrypt(ip)})",
                                    errorType = ErrorType.USER,
                                    errorCode = AtomErrorCode.USER_INPUT_INVAILD
                                )
                            } else {
                                LoggerService.addNormalLine(
                                    "enterprise sign successfully ($file)  in ip(${SecurityUtil.encrypt(
                                        ip
                                    )})"
                                )
                                return
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 异常情况打印尝试日志
                remain--
                val currentTime = tryTime - remain
                LoggerService.addRedLine("enterprise sign failed time at $currentTime time : ($file)")
            }
        }
        // 最终失败
        if (isException) {
            throw TaskExecuteException(
                errorMsg = "enterprise sign failed after all retry : ($file)",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }

    private fun getProps(file: File, buildVariables: BuildVariables): String {
        val props = IosUtils.getIpaInfoMap(file)
        val bundleId = props["bundleIdentifier"] ?: ""
        val appTitle = props["appTitle"] ?: ""
        val bundleVersion = props["bundleVersion"] ?: ""

        return "bundleIdentifier=$bundleId;appTitle=$appTitle;appVersion=$bundleVersion;" +
            "projectId=${buildVariables.projectId};pipelineId=${buildVariables.pipelineId};buildId=${buildVariables.buildId};" +
            "buildNo=${buildVariables.variables[PIPELINE_BUILD_NUM]};userId=${buildVariables.variables[PIPELINE_START_USER_ID]};source=pipeline;$ARCHIVE_PROPS_IPA_SIGN_STATUS=true"
    }
}
