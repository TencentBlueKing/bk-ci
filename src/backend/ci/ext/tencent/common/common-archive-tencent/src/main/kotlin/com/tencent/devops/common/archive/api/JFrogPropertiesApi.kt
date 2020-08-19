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

package com.tencent.devops.common.archive.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.api.pojo.ArtifactProperties
import com.tencent.devops.common.archive.util.JFrogUtil
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class JFrogPropertiesApi constructor(
    private val jFrogConfigProperties: JFrogConfigProperties,
    private val objectMapper: ObjectMapper
) {
    private val baseUrl = jFrogConfigProperties.url!!
    private val credential = JFrogUtil.makeCredential(jFrogConfigProperties.username!!, jFrogConfigProperties.password!!)

    fun getProperties(path: String): Map<String, List<String>> {
        logger.info("getProperties, path: $path")
        val encodePath = URLEncoder.encode(path.removePrefix(JFrogUtil.getRepoPath()), "UTF-8")
        val url = "$baseUrl/api/artifactproperties?path=$encodePath&repoKey=generic-local"
        val request = Request.Builder().url(url).header("Authorization", credential).get().build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("get file properties failed, encodePath: $encodePath, responseContent: $responseContent")
                throw RuntimeException("get file properties failed")
            }

            val artifactProperties = objectMapper.readValue<ArtifactProperties>(responseContent)
            return artifactProperties.artifactProperties.associate {
                it.name to listOf(it.value)
            }
        }
    }

    fun setProperties(path: String, properties: Map<String, List<String>>, recursive: Boolean = false) {
        if (properties.isEmpty()) return

        val recursiveInt = if (recursive) 1 else 0
        val url = "$baseUrl/api/storage/$path?properties=${encodeProperties(properties)}&recursive=$recursiveInt"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .put(RequestBody.create(MediaType.parse("application/json"), ""))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to set jfrog properties $path. $responseContent")
                throw RuntimeException("Fail to set jfrog properties")
            }
        }
    }

    fun deleteProperties(path: String, propertyKeys: List<String>, recursive: Boolean = false) {
        if (propertyKeys.isEmpty()) return

        val recursiveInt = if (recursive) 1 else 0
        val url = "$baseUrl/api/storage/$path?properties=${propertyKeys.joinToString(",")}&recursive=$recursiveInt"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .delete()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to delete jfrog properties $path. $responseContent")
                throw RuntimeException("Fail to delete jfrog properties")
            }
        }
    }

    private fun encodeProperties(properties: Map<String, List<String>>): String {
        val propertiesSb = StringBuilder()
        properties.forEach { key, values ->
            if (values.isNotEmpty()) {
                if (propertiesSb.isNotEmpty()) propertiesSb.append(";")

                val valueSb = StringBuilder()
                values.forEach { value ->
                    if (valueSb.isNotEmpty()) valueSb.append(",")
                    valueSb.append(encodeProperty(value))
                }
                propertiesSb.append("${encodeProperty(key)}=$valueSb")
            }
        }
        return propertiesSb.toString()
    }

    private fun encodeProperty(str: String): String {
        return str.replace(",", "%5C,")
            .replace("\\", "%5C\\")
            .replace("|", "%5C|")
            .replace("=", "%5C=")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogPropertiesApi::class.java)
    }
}