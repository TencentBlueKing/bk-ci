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

package com.tencent.bkrepo.composer.artifact.repository

import com.google.gson.JsonParser
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.composer.INIT_PACKAGES
import com.tencent.bkrepo.composer.util.JsonUtil.wrapperPackageJson
import okhttp3.Request
import org.springframework.stereotype.Component

@Component
class ComposerRemoteRepository : RemoteRepository() {

    override fun query(context: ArtifactQueryContext): String? {
        return if (context.artifactInfo.equals("/packages.json")) {
            getPackages(context)
        } else {
            getJson(context)
        }
    }

    fun getPackages(context: ArtifactQueryContext): String? {
        val request = HttpContextHolder.getRequest()
        val host = request.requestURL.toString().removeSuffix(context.artifactInfo.getArtifactFullPath())
        return INIT_PACKAGES.wrapperPackageJson(host)
    }

    fun getJson(context: ArtifactQueryContext): String? {
        val remoteConfiguration = context.getRemoteConfiguration()
        val artifactUri = context.artifactInfo.getArtifactFullPath()
        val remotePackagesUri = "${remoteConfiguration.url.removeSuffix("/")}$artifactUri"
        val okHttpClient = createHttpClient(remoteConfiguration)
        val request = Request.Builder().url(remotePackagesUri)
            .addHeader("Connection", "keep-alive")
            .get().build()
        val result = okHttpClient.newCall(request).execute().body()?.string()
        try {
            JsonParser.parseString(result).asJsonObject
        } catch (e: IllegalStateException) {
            return null
        }
        return result
    }
}
