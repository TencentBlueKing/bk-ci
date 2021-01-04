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

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.TclsType
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.element.TclsAddVersionElement
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.atom.defaultSuccessAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.Base64

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TclsAddVersionTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<TclsAddVersionElement> {

    override fun getParamElement(task: PipelineBuildTask): TclsAddVersionElement {
        return JsonUtil.mapTo(task.taskParams, TclsAddVersionElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: TclsAddVersionElement, runVariables: Map<String, String>): AtomResponse {
        logger.info("Enter TclsAddVersionDelegate run...")

        // 如果增加了mtcls2的类型，则这里需要修改
        val isMtclsApp = param.mtclsApp == TclsType.MTCLS
        val buildId = task.buildId
        val elementId = task.taskId

        if (isMtclsApp) {
            if (param.serviceId.isNullOrBlank()) {
                logger.warn("TCLS serviceId is not init of build($buildId)")
                buildLogPrinter.addRedLine(buildId, "TCLS serviceId is not init", elementId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "TCLS serviceId is not init"
                )
            }
        } else {
            if (param.tclsAppId.isNullOrBlank()) {
                logger.warn("TCLS appId is not init of build($buildId)")
                buildLogPrinter.addRedLine(buildId, "TCLS appId is not init", elementId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "TCLS appId is not init"
                )
            }
        }

        if (param.ticketId.isBlank()) {
            logger.warn("ticketId is not init of build($buildId)")
            buildLogPrinter.addRedLine(buildId, "ticketId is not init", elementId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "ticketId is not init"
            )
        }

        val tclsAppServiceId = if (isMtclsApp) {
            parseVariable(param.serviceId, runVariables)
        } else {
            parseVariable(param.tclsAppId, runVariables)
        }
        val ticketId = parseVariable(param.ticketId, runVariables)
        val envId = parseVariable(param.envId, runVariables)
        val versionFrom = parseVariable(param.versionFrom, runVariables)
        val versionTo = parseVariable(param.versionTo, runVariables)
        val desc = parseVariable(param.desc, runVariables)
        val pkgName = parseVariable(param.pkgName, runVariables)
        val httpUrl = parseVariable(param.httpUrl, runVariables)
        val fileHash = parseVariable(param.fileHash, runVariables)
        val size = parseVariable(param.size, runVariables)
        val updateWay = parseVariable(param.updateWay, runVariables)
        val hashUrl = parseVariable(param.hashUrl, runVariables)
        val hashMd5 = parseVariable(param.hashMd5, runVariables)
        val customStr = parseVariable(param.customStr, runVariables)
        val updatePkgType = parseVariable(param.updatePkgType, runVariables)

        val userId = task.starter

        val projectId = task.projectId
        val ticketsMap = getCredential(projectId, ticketId, CredentialType.USERNAME_PASSWORD)
        val appAcount = ticketsMap["v1"].toString()
        val password = ticketsMap["v2"].toString()

        /* http 请求 */
        val url = if (isMtclsApp) "http://open.oa.com/component/compapi/mtcls/add_versions/" else "http://open.oa.com/component/compapi/tcls/add_versions/"
        val requestData = mapOf(
                "app_code" to APP_CODE,
                "app_secret" to APP_SECRET,
                "operator" to userId,
                "tcls_app_id" to tclsAppServiceId,
                "env_id" to envId,
                "app_account" to appAcount,
                "password" to password,
                "version_infos" to listOf(mapOf(
                        "package_info" to mapOf(
                                "package_desc" to mapOf(
                                        "name" to pkgName,
                                        "httpurl" to httpUrl,
                                        "file_hash" to fileHash,
                                        "version_from" to versionFrom,
                                        "version_to" to versionTo,
                                        "update_way" to updateWay,
                                        "download_way" to "1",
                                        "hash_url" to hashUrl,
                                        "hash_md5" to hashMd5,
                                        "size" to size,
                                        "custom_str" to customStr,
                                        "desc" to desc,
                                        "pkg_type" to updatePkgType
                                ))))
        )
        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//                .connectTimeout(5L, TimeUnit.SECONDS)
//                .readTimeout(60L, TimeUnit.SECONDS)
//                .writeTimeout(60L, TimeUnit.SECONDS)
//                .build()

        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody)).build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//                val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["result"] == false) {
                    val msg = responseData["message"]

                    logger.error("add version failed: $msg")
                    buildLogPrinter.addRedLine(buildId, "添加 TCLS 版本失败,错误信息：$msg", elementId, task.containerHashId, task.executeCount ?: 1)
                    return defaultFailAtomResponse
                }
            } catch (e: Exception) {
                logger.error("add version error", e)
                buildLogPrinter.addRedLine(buildId, "添加 TCLS 版本失败,错误信息：${e.message}", elementId, task.containerHashId, task.executeCount ?: 1)
                return defaultFailAtomResponse
            }
        }
        return defaultSuccessAtomResponse
    }

    private fun getCredential(projectId: String, credentialId: String, type: CredentialType): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(projectId, credentialId,
                encoder.encodeToString(pair.publicKey))
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
            throw throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "Fail to get the credential($credentialId) of project($projectId)"
            )
        }

        val credential = credentialResult.data!!
        if (type != credential.credentialType) {
            logger.error("CredentialId is invalid, expect:${type.name}, but real:${credential.credentialType.name}")
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "Fail to get the credential($credentialId) of project($projectId)"
            )
        }

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey))
        ticketMap["v1"] = v1

        if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            val v2 = String(DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v2"] = v2
        }

        if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            val v3 = String(DHUtil.decrypt(
                    decoder.decode(credential.v3),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v3"] = v3
        }

        if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            val v4 = String(DHUtil.decrypt(
                    decoder.decode(credential.v4),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v4"] = v4
        }

        return ticketMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TclsAddVersionTaskAtom::class.java)

        private val JSON = MediaType.parse("application/json;charset=utf-8")

        private const val APP_CODE = "bkci"
        private const val APP_SECRET = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
    }
}
