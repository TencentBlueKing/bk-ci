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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.project.constant.DEVOPS_CALLBACK_FLAG
import com.tencent.devops.project.constant.DEVOPS_SEND_TIMESTAMP
import com.tencent.devops.project.dao.ProjectCallbackDao
import com.tencent.devops.project.enum.ProjectEventType
import com.tencent.devops.project.pojo.ProjectCallbackData
import com.tencent.devops.project.pojo.SecretRequestParam
import com.tencent.devops.project.pojo.secret.DefaultSecretParam
import com.tencent.devops.project.pojo.secret.ISecretParam
import com.tencent.devops.project.pojo.secret.PaasCCSecretParam
import com.tencent.devops.project.pojo.secret.bkrepo.BkrepoModelSecretParam
import com.tencent.devops.project.pojo.secret.bkrepo.BkrepoProjectSecretParam
import com.tencent.devops.project.service.secret.DefaultSecretTokenService
import com.tencent.devops.project.service.secret.SecretTokenServiceFactory
import com.tencent.devops.project.service.secret.bkrepo.BkrepoModelSecretTokenService
import com.tencent.devops.project.service.secret.bkrepo.BkrepoProjectSecretTokenService
import com.tencent.devops.project.util.OkHttpUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Service
class ProjectCallbackControl @Autowired constructor(
    val projectCallbackDao: ProjectCallbackDao,
    val dslContext: DSLContext
) {

    @Value("\${project.callback.secretParam.aes-key}")
    private val aesKey = "C/R%3{?OS}IeGT21"

    @PostConstruct
    fun init() {
        logger.info("init project callback control")
        initSecretTokenService()
    }

    open fun initSecretTokenService() {
        SecretTokenServiceFactory.register(BkrepoModelSecretParam::class.java, BkrepoModelSecretTokenService())
        SecretTokenServiceFactory.register(BkrepoProjectSecretParam::class.java, BkrepoProjectSecretTokenService())
        SecretTokenServiceFactory.register(DefaultSecretParam::class.java, DefaultSecretTokenService())
    }

    fun callBackProjectEvent(projectEventType: ProjectEventType, callbackData: ProjectCallbackData) {
        // 查询事件相关的回调记录，创建事件暂时过滤掉paasCC的回调，否则发布瞬间可能导致项目创建失败
        // 参考：com.tencent.devops.project.service.impl.TxProjectExtServiceImpl.createExtProjectInfo
        val callBackList = projectCallbackDao.get(
            dslContext = dslContext,
            event = projectEventType.name,
            url = null,
            ignoreTypes = if (projectEventType == ProjectEventType.CREATE) {
                setOf(PaasCCSecretParam.classType)
            } else {
                emptySet()
            }
        )
        callBackList.map {
            val secretParam = JsonUtil.to(it.secretParam, ISecretParam::class.java).decode(aesKey)
            val secretTokenService = SecretTokenServiceFactory.getSecretTokenService(secretParam)
            // 1.获取URL/请求头/URL参数
            val secretRequestParam = secretTokenService.getSecretRequestParam(
                userId = secretParam.userId,
                projectId = callbackData.projectId,
                secretParam = secretParam
            )
            // 2.获取请求体
            val requestBody = secretTokenService.getRequestBody(
                secretParam = secretParam,
                projectCallbackData = callbackData
            )
            // 3.处理url占位符
            secretRequestParam.url = formatUrl(
                url = secretRequestParam.url,
                projectId = callbackData.projectId,
                projectEventType = projectEventType.name,
                projectEnglishName = callbackData.projectEnglishName
            )
            logger.info(
                "start send project callback|eventType[${it.event}]|url[${secretRequestParam.url}]|" +
                        "secretType[${it.secretType}]"
            )
            // 4.发请求
            send(
                secretRequestParam = secretRequestParam,
                requestBody = requestBody,
                method = secretParam.method,
                failAction = { exception -> secretTokenService.requestFail(exception) },
                successAction = { responseBody -> secretTokenService.requestSuccess(responseBody) }
            )
        }
    }

    private fun send(
        secretRequestParam: SecretRequestParam,
        requestBody: String,
        method: String,
        failAction: ((exception: Exception) -> Unit) = { },
        successAction: ((responseBody: String) -> Unit) = { }
    ) {
        with(secretRequestParam) {
            OkHttpUtils.sendRequest(
                method = method,
                url = url,
                headers = insertCommonHeader(header),
                params = params,
                requestBody = requestBody,
                failAction = failAction,
                successAction = successAction
            )
        }
    }

    /**
     * 插入平台级Header
     */
    private fun insertCommonHeader(headers: Map<String, String>?): Map<String, String> {
        val targetHeaders = mutableMapOf<String, String>()
        headers?.let {
            targetHeaders.putAll(it)
        }
        targetHeaders[DEVOPS_SEND_TIMESTAMP] = LocalDateTime.now().timestamp().toString()
        targetHeaders[DEVOPS_CALLBACK_FLAG] = CALLBACK_FLAG
        return targetHeaders
    }

    /**
     * 如果url中包含占位符，则进行替换
     * {projectId} -> 蓝盾项目Id,32位字符
     * {eventType} -> 项目事件类型
     * {englishName} -> 蓝盾项目英文名
     */
    private fun formatUrl(
        url: String,
        projectId: String,
        projectEventType: String,
        projectEnglishName: String
    ): String {
        val encodeProjectId = URLEncoder.encode(URL_PLACEHOLDER_PROJECT_ID, "UTF-8")
        val encodeProjectEnglishName = URLEncoder.encode(URL_PLACEHOLDER_ENGLISH_NAME, "UTF-8")
        val encodeEventType = URLEncoder.encode(URL_PLACEHOLDER_EVENT_TYPE, "UTF-8")
        return url
            .replace(encodeProjectId, projectId)
            .replace(encodeEventType, projectEventType)
            .replace(encodeProjectEnglishName, projectEnglishName)
            .replace(URL_PLACEHOLDER_PROJECT_ID, projectId)
            .replace(URL_PLACEHOLDER_EVENT_TYPE, projectEventType)
            .replace(URL_PLACEHOLDER_ENGLISH_NAME, projectEnglishName)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectCallbackControl::class.java)
        const val CALLBACK_FLAG = "devops_project"
        const val URL_PLACEHOLDER_PROJECT_ID = "{projectId}"
        const val URL_PLACEHOLDER_ENGLISH_NAME = "{projectEnglishName}"
        const val URL_PLACEHOLDER_EVENT_TYPE = "{eventType}"
    }
}