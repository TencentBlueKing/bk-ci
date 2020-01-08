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

package com.tencent.devops.artifactory.client

import com.google.gson.JsonParser
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.service.utils.HomeHostUtil
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Paths

@Component
class JFrogServiceClient {

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogServiceClient::class.java)
    }

    @Value("\${devopsGateway.api:#{null}}")
    private var gatewayUrl: String? = null

    fun getHost(): String {
        return HomeHostUtil.getHost(gatewayUrl!!)
    }

    // 从仓库匹配到所有文件
    fun getFileDownloadUrl(params: ArtifactorySearchParam): List<String> {
        val searchUrl = "${getHost()}/jfrog/api/service/search/aql"
        val requestBody = getRequestBody(params)
        logger.info("search url: $searchUrl")
        logger.info("requestBody:" + requestBody.removePrefix("items.find(").removeSuffix(")"))
        val request = Request.Builder()
            .url(searchUrl)
            .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
            .build()

        val fileUrls = mutableListOf<String>()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("search responseBody: $body")
            val results = JsonParser().parse(body).asJsonObject["results"].asJsonArray
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val path = getPath(obj["path"].asString, obj["name"].asString, params)
                val url = getUrl(path, params.custom)
                fileUrls.add(url)
            }
        }

        return fileUrls
    }

    // 从仓库匹配到所有文件
    fun matchFiles(params: ArtifactorySearchParam): List<String> {
        val searchUrl = "${getHost()}/jfrog/api/service/search/aql"
        val requestBody = getRequestBody(params)
        logger.info("search url: $searchUrl")
        logger.info("requestBody:" + requestBody.removePrefix("items.find(").removeSuffix(")"))
        val request = Request.Builder()
            .url(searchUrl)
            .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
            .build()

        val files = mutableListOf<String>()
        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("search responseBody: $body")
            val results = JsonParser().parse(body).asJsonObject["results"].asJsonArray
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val path = getPath(obj["path"].asString, obj["name"].asString, params)
                files.add(path)
            }
        }

        return files
    }

    // 从仓库下载文件到指定目录
    fun downloadFile(params: ArtifactorySearchParam, destPath: String): List<File> {
        val searchUrl = "${getHost()}/jfrog/api/service/search/aql"
        val requestBody = getRequestBody(params)
        logger.info("search url: $searchUrl")
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
                val path = getPath(obj["path"].asString, obj["name"].asString, params)
                val url = getUrl(path, params.custom)

                val destFile = File(destPath, obj["name"].asString)
                OkhttpUtils.downloadFile(url, destFile)
                files.add(destFile)
                logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
            }
        }

        return files
    }

    // 处理jfrog传回的路径
    private fun getPath(path: String, name: String, params: ArtifactorySearchParam): String {
        with(params) {
            return if (custom) {
                path.substring(path.indexOf("/") + 1).removePrefix("/$projectId") + "/" + name
            } else {
                path.substring(path.indexOf("/") + 1).removePrefix("/$projectId/$pipelineId/$buildId") + "/" + name
            }
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

    private fun getRequestBody(params: ArtifactorySearchParam): String {
        val pathPair = getPathPair(params.regexPath)
        val parent = pathPair["parent"]
        val child = pathPair["child"]
        with(params) {
            return if (custom) {
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
    }

    private fun getPathPair(regex: String): Map<String, String> {
        // 文件夹，匹配所有文件
        if (regex.endsWith("/")) return mapOf("parent" to "/" + regex.removeSuffix("/"), "child" to "*")

        val f = File(regex)
        return if (f.parent.isNullOrBlank()) mapOf("parent" to "", "child" to regex)
        else mapOf("parent" to "/" + f.parent, "child" to f.name)
    }
}