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

package com.tencent.devops.common.archive.client

import com.google.gson.JsonParser
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

class JfrogClient constructor(
    private val gatewayUrl: String,
    private val projectId: String,
    private val pipelineId: String,
    private val buildId: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JfrogClient::class.java)
    }

    private fun getHost(): String {
        return HomeHostUtil.getHost(gatewayUrl)
    }

    // 从仓库匹配到所有文件
    fun matchFiles(regex: String, isCustom: Boolean): List<String> {
        val searchUrl = "${getHost()}/jfrog/api/service/search/aql"
        val requestBody = getRequestBody(regex, isCustom)
        logger.info("requestBody:" + requestBody.removePrefix("items.find(").removeSuffix(")"))
        val request = Request.Builder()
                .url(searchUrl)
                .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
                .build()

        val files = mutableListOf<String>()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()

            val results = JsonParser().parse(body).asJsonObject["results"].asJsonArray
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val path = getPath(obj["path"].asString, obj["name"].asString, isCustom)
                files.add(path)
            }
        }

        return files
    }

    // 从仓库下载文件到指定目录
    fun downloadFile(regex: String, isCustom: Boolean, destPath: String): List<File> {
        val searchUrl = "${getHost()}/jfrog/api/service/search/aql"
        val requestBody = getRequestBody(regex, isCustom)
        logger.info("requestBody:" + requestBody.removePrefix("items.find(").removeSuffix(")"))
        val request = Request.Builder()
                .url(searchUrl)
                .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
                .build()

        val files = mutableListOf<File>()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()

            val results = JsonParser().parse(body).asJsonObject["results"].asJsonArray
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val path = getPath(obj["path"].asString, obj["name"].asString, isCustom)
                val url = getUrl(path, isCustom)

                val destFile = File(destPath, obj["name"].asString)
                OkhttpUtils.downloadFile(url, destFile)
                files.add(destFile)
                logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
            }
        }

        return files
    }

    // 处理jfrog传回的路径
    private fun getPath(path: String, name: String, isCustom: Boolean): String {
        return if (isCustom) {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId") + "/" + name
        } else {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId/$pipelineId/$buildId") + "/" + name
        }
    }

    // 获取jfrog传回的url
    private fun getUrl(realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "${getHost()}/jfrog/storage/service/custom/$realPath"
        } else {
            "${getHost()}/jfrog/storage/service/archive/$realPath"
        }
    }

    private fun getRequestBody(regex: String, isCustom: Boolean): String {
        val pathPair = getPathPair(regex)
        val parent = pathPair["parent"]
        val child = pathPair["child"]
        return if (isCustom) {
            val path = Paths.get("bk-custom/$projectId$parent").normalize().toString()
            "items.find(\n" +
                    "    {\n" +
                    "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"$path\"}, \"name\":{\"\$match\":\"$child\"}\n" +
                    "    }\n" +
                    ")"
        } else {
            val path = Paths.get("bk-archive/$projectId/$pipelineId/$buildId$parent").normalize().toString()
            "items.find(\n" +
                    "    {\n" +
                    "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"$path\"}, \"name\":{\"\$match\":\"$child\"}\n" +
                    "    }\n" +
                    ")"
        }
    }

    fun uploadFile(userId: String? = null, repoName: String, path: String, file: File, properties: Map<String, String>? = null) {
        logger.info("uploadFile, userId: $userId, projectId: $projectId, repoName: $repoName, path: $path")

        var url = if (repoName.toLowerCase() == "pipeline") "${getHost()}/service/result/$projectId/$path" else "${getHost()}/custom/service/result/$projectId/$path?"
        properties?.forEach {
            url += ";${it.key}=${it.value}"
        }
        val request = Request.Builder()
                .url(url)
                .put(RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("jfrog upload file failed. url:$url. response:$responseContent")
                throw RemoteServiceException("jfrog upload file failed. url:$url. response:$responseContent")
            }
        }
    }

    private fun getPathPair(regex: String): Map<String, String> {
        // 文件夹，匹配所有文件
        if (regex.endsWith("/")) return mapOf("parent" to "/" + regex.removeSuffix("/"), "child" to "*")

        val f = File(regex)
        return if (f.parent.isNullOrBlank()) mapOf("parent" to "", "child" to regex)
        else mapOf("parent" to "/" + f.parent, "child" to f.name)
    }
}