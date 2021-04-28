/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.webhook

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.ArtifactEventType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.webhook.WebHookSetting
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import java.io.IOException

open class WebHookService {

    private val httpClient = HttpClientBuilderFactory.create().build()

    private val jsonMediaType = MediaType.parse(MediaTypes.APPLICATION_JSON)

    @Async
    open fun hook(context: ArtifactContext, type: ArtifactEventType) {
        // CompositeConfiguration extends LocalConfiguration
        val configuration = when (context.repositoryDetail.category) {
            RepositoryCategory.LOCAL -> context.getLocalConfiguration()
            RepositoryCategory.COMPOSITE -> context.getCompositeConfiguration()
            else -> return
        }
        configuration.webHook.webHookList.takeIf { it.isNotEmpty() }?.run {
            val data = ArtifactWebHookData(context.artifactInfo, type)
            val requestBody = RequestBody.create(jsonMediaType, data.toJsonString())
            this.forEach { info -> remoteCall(info, requestBody) }
        }
    }

    private fun remoteCall(webHookSetting: WebHookSetting, requestBody: RequestBody) {
        try {
            val builder = Request.Builder().url(webHookSetting.url).post(requestBody)
            webHookSetting.headers?.forEach { (key, value) -> builder.addHeader(key, value) }
            val request = builder.build()
            val response = httpClient.newCall(request).execute()
            assert(response.isSuccessful)
            logger.info("Execute web hook[$webHookSetting] success.")
        } catch (exception: IOException) {
            logger.error("Execute web hook[$webHookSetting] error.", exception)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebHookService::class.java)
    }
}
