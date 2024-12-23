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

package com.tencent.devops.worker.common.api.archive

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.pojo.FileGatewayInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_BUILD_BASE_INFO_FAIL
import com.tencent.devops.worker.common.env.AgentEnv

class ArtifactoryBuildResourceApi : AbstractBuildResourceApi() {

    fun getRealm(): String {
        val path = "/ms/artifactory/api/build/artifactories/conf/realm"
        val request = buildGet(path)
        val responseContent = request(request, "get artifactory realm error")
        return objectMapper.readValue<Result<String>>(responseContent).data!!
    }

    fun getFileGatewayInfo(): FileGatewayInfo? {
        return try {
            val path = "/artifactory/api/build/fileGateway/get"
            val request = buildGet(path)
            val response = request(
                request,
                MessageUtil.getMessageByLocale(GET_BUILD_BASE_INFO_FAIL, AgentEnv.getLocaleLanguage())
            )
            val fileGatewayResult = objectMapper.readValue<Result<FileGatewayInfo>>(response)
            fileGatewayResult.data
        } catch (e: Exception) {
            logger.warn("get file gateway exception", e)
            FileGatewayInfo("", "")
        }
    }
}
