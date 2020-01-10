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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.service.pojo.JFrogAQLFileInfo
import com.tencent.devops.artifactory.service.pojo.JFrogAQLResponse
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class JFrogAQLService @Autowired constructor(private val objectMapper: ObjectMapper) {
    @Value("\${jfrog.url:#{null}}")
    private val JFROG_BASE_URL: String? = null
    @Value("\${jfrog.username:#{null}}")
    private val JFROG_USERNAME: String? = null
    @Value("\${jfrog.password:#{null}}")
    private val JFROG_PASSWORD: String? = null

//    private val okHttpClient = okhttp3.OkHttpClient.Builder()
//        .connectTimeout(5L, TimeUnit.SECONDS)
//        .readTimeout(60L, TimeUnit.SECONDS)
//        .writeTimeout(60L, TimeUnit.SECONDS)
//        .build()

    /**
     * 通过JFrog AQL接口查询文件，并根据文件创建时间倒序返回
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param offset
     * @param limit
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *          "repo":{"$eq":"generic-local"}, "type":"file", "$or": [{"path":{"$eq":"bk-custom/aarontest1/test7"}}, {"path":{"$match":"bk-custom/aarontest1/test7/\*"}}]
     *      }
     *  ).sort({"$desc": ["created"]}).offset(0).limit(10)'
     *
     *  注意：
     *  jfrog path不是以"/"结尾，所以这里既要查eq也要查match
     *
     */
    fun listByCreateTimeDesc(
        parentFolder: String,
        relativePaths: Set<String>,
        offset: Int? = null,
        limit: Int? = null
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            if (relativePaths.isEmpty()) {
                return emptyList()
            }

            val roadList = parentFolder.split("/")
            val repoKey = roadList.first()
            val relativeParentPath = parentFolder.removePrefix("$repoKey/")

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append(" \"repo\": {\"\$eq\": \"$repoKey\"},")
            sb.append(" \"type\": \"file\",")
            sb.append(" \"\$or\": [")
            sb.append(
                relativePaths.map {
                    val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                    "{\"path\": {\"\$eq\": \"$path\"}}"
                }.plus(
                    relativePaths.map {
                        val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                        "{\"path\": {\"\$match\": \"$path/*\"}}"
                    }
                ).joinToString(",")
            )
            sb.append("           ]")
            sb.append("}).sort({\"\$desc\": [\"modified\"]})")

            if (offset != null && limit != null) {
                sb.append(".offset($offset).limit($limit)")
            }

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to list file by create time. $responseContent")
                    throw RuntimeException("Fail to list file by create time")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("listByCreateTimeDesc cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 通过JFrog AQL接口根据元数据查询文件，并根据文件创建时间倒序返回
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param names 匹配文件名，支持模糊匹配
     * @param props 元数据键值对
     * @param offset
     * @param limit
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *          "repo":{"$eq":"generic-local"}, "type":"file",
     *          "$or": [{"path":{"$match":"bk-custom/aarontest1/test7"}}],
     *          "$or": [{"name":{"$match":"*.ipa"}}],
     *          "and": [{"property.key": {"$eq": "key"}, "property.value": {"$eq": "value"}}]
     *      }
     *  ).sort({"$desc": ["modified"]}).offset(0).limit(10)'
     *
     */
    fun searchByProperty(
        parentFolder: String,
        relativePaths: Set<String>,
        names: Set<String>,
        props: List<Pair<String, String>>,
        offset: Int? = null,
        limit: Int? = null
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            if (relativePaths.isEmpty() && props.isEmpty() && names.isEmpty()) {
                return emptyList()
            }

            val roadList = parentFolder.split("/")
            val repoKey = roadList.first()
            val relativeParentPath = parentFolder.removePrefix("$repoKey/")

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append("\"repo\": {\"\$eq\": \"$repoKey\"}")
            sb.append(",\"type\": \"file\"")

            // 相对路径条件
            if (relativePaths.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    relativePaths.map {
                        val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                        "{\"path\": {\"\$match\": \"$path*\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 文件名匹配
            if (names.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    names.map {
                        "{\"name\": {\"\$match\": \"$it\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 元数据and查询
            if (props.isNotEmpty()) {
                sb.append(",\"\$and\": [")
                sb.append(
                    props.map {
                        "{\"property.key\": {\"\$eq\": \"${it.first}\"}, \"property.value\": {\"\$eq\": \"${it.second}\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 排序
            sb.append("}).sort({\"\$desc\": [\"modified\"]})")

            // 分页
            if (offset != null && limit != null) {
                sb.append(".offset($offset).limit($limit)")
            }

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to search by property. $responseContent")
                    throw RuntimeException("Fail to search by property")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchByProperty cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 通过JFrog AQL接口根据元数据查询文件和元数据
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param names 匹配文件名，支持模糊匹配
     * @param props 元数据键值对
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *          "repo":{"$eq":"generic-local"}, "type":"file",
     *          "$or": [{"path":{"$match":"bk-custom/aarontest1/test7"}}],
     *          "and": [{"property.key": {"$eq": "key"}, "property.value": {"$eq": "value"}}]
     *      }
     *  ).include("property",..)'
     *
     */
    fun searchFileAndPropertyByPropertyByAnd(
        parentFolder: String,
        relativePaths: Set<String>,
        names: Set<String>,
        props: List<Pair<String, String>>
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            if (relativePaths.isEmpty() && props.isEmpty() && names.isEmpty()) {
                return emptyList()
            }

            val roadList = parentFolder.split("/")
            val repoKey = roadList.first()
            val relativeParentPath = parentFolder.removePrefix("$repoKey/")

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append("\"repo\": {\"\$eq\": \"$repoKey\"}")
            sb.append(",\"type\": \"file\"")

            // 相对路径条件
            if (relativePaths.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    relativePaths.map {
                        val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                        "{\"path\": {\"\$match\": \"$path*\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 文件名匹配
            if (names.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    names.map {
                        "{\"name\": {\"\$match\": \"$it\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 元数据and查询
            if (props.isNotEmpty()) {
                sb.append(",\"\$and\": [")
                sb.append(
                    props.map {
                        "{\"property.key\": {\"\$eq\": \"${it.first}\"}, \"property.value\": {\"\$eq\": \"${it.second}\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            sb.append("}).include(\"property\")")

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to search by property. $responseContent")
                    throw RuntimeException("Fail to search by property")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchFileAndPropertyByPropertyByAnd cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 通过JFrog AQL接口根据元数据查询文件和元数据
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param names 匹配文件名，支持模糊匹配
     * @param props 元数据键值对
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *          "repo":{"$eq":"generic-local"}, "type":"file",
     *          "$or": [{"path":{"$match":"bk-custom/aarontest1/test7*"}}, {"property.key": {"$eq": "key"}, "property.value": {"$eq": "value"}}],
     *      }
     *  ).include("property",..)'
     *
     */
    fun searchFileAndPropertyByPropertyByOr(
        parentFolder: String,
        relativePaths: Set<String>,
        names: Set<String>,
        props: List<Pair<String, String>>
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            if (relativePaths.isEmpty() && props.isEmpty() && names.isEmpty()) {
                return emptyList()
            }

            val roadList = parentFolder.split("/")
            val repoKey = roadList.first()
            val relativeParentPath = parentFolder.removePrefix("$repoKey/")

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append("\"repo\": {\"\$eq\": \"$repoKey\"}")
            sb.append(",\"type\": \"file\"")

            val andList = mutableListOf<String>()
            // 相对路径条件
            if (relativePaths.isNotEmpty()) {
                val relativePathSb = StringBuilder()
                relativePathSb.append("{\"\$or\": [")
                relativePathSb.append(
                    relativePaths.map {
                        val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                        "{\"path\": {\"\$match\": \"$path*\"}}"
                    }.joinToString(",")
                )
                relativePathSb.append("]}")
                // andList.add(relativePathSb.toString())
            }

            // 文件名匹配
            if (names.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    names.map {
                        "{\"name\": {\"\$match\": \"$it\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 元数据or查询
            if (props.isNotEmpty()) {
                val propSb = StringBuilder()
                propSb.append("{\"\$or\": [")

                val strList = mutableListOf<String>()
                props.forEach {
                    strList.add("{\"\$and\": [{\"property.key\": {\"\$eq\": \"${it.first}\"}, \"property.value\": {\"\$eq\": \"${it.second}\"}}]}")
                }

                propSb.append(strList.joinToString(","))
                propSb.append("]}")
                andList.add(propSb.toString())
            }

            sb.append(", \"\$and\": [${andList.joinToString(",")}]")
            sb.append("}).include(\"property\")")

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to search by property. $responseContent")
                    throw RuntimeException("Fail to search by property")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchFileAndPropertyByPropertyByOr cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 通过JFrog AQL接口根据正则匹配文件和元数据
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param names 匹配文件名，支持模糊匹配
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *          "repo":{"$eq":"generic-local"}, "type":"file",
     *          "$or": [{"path":{"$match":"bk-custom/aarontest1/test7"}}],
     *          "or": [{"name": {"$match": "*.ipa"}, {"name": {"$match": "*.apk"}}]
     *      }
     *  ).include("property",..)'
     *
     */
    fun searchFileByRegex(
        parentFolder: String,
        relativePaths: Set<String>,
        names: Set<String>
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        try {
            if (relativePaths.isEmpty() && names.isEmpty()) {
                return emptyList()
            }

            val roadList = parentFolder.split("/")
            val repoKey = roadList.first()
            val relativeParentPath = parentFolder.removePrefix("$repoKey/")

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append("\"repo\": {\"\$eq\": \"$repoKey\"}")
            sb.append(",\"type\": \"file\"")

            // 相对路径条件
            if (relativePaths.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    relativePaths.map {
                        val path = "$relativeParentPath${it.removePrefix("/").removeSuffix("/")}"
                        "{\"path\": {\"\$eq\": \"$path\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            // 文件名匹配
            if (names.isNotEmpty()) {
                sb.append(",\"\$or\": [")
                sb.append(
                    names.map {
                        "{\"name\": {\"\$match\": \"$it\"}}"
                    }.joinToString(",")
                )
                sb.append("           ]")
            }

            sb.append("}).include(\"property\")")

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to search by regex. $responseContent")
                    throw RuntimeException("Fail to search by regex")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchFileByRegex cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 通过JFrog AQL接口根据时间段文件和元数据
     *
     * 参数：
     * @param parentFolder 父目录包含repo，例如: generic-local/bk-custom/a90/
     * @param relativePaths 父目录下相对了路径，例如: {/a/, /b/}
     * @param names 匹配文件名，支持模糊匹配
     *
     * 返回：
     * @return List<JFrogAQLFileInfo>
     * JFrogAQLFileInfo(
     *   path: parentFolder下的相对路径
     *   name: 文件名
     *   size: 文件大小字节
     *   created: 创建时间
     * )
     *
     * JFrog接口：
     *  curl -X POST 'http://test.artifactory.xxx.com/api/search/aql' -H 'Content-Type:text/plain' -d 'items.find(
     *      {
     *         "modified" : {"$gt" : "2019-07-12T19:20:30.45+01:00"},
     *         "modified" : {"$lt" : "2019-07-19T19:20:30.45+01:00"}})
     *
     *      }
     *  ).include("property",..).sort({"$desc": ["modified"]}).offset(0).limit(10)'
     *
     */

    fun searchFileByTime(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()
        try {
            if (startTime == 0L && endTime == 0L) {
                return emptyList()
            }

            val startTimeDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneId.systemDefault())
            val endTimeDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(endTime), ZoneId.systemDefault())

            val sb = StringBuilder()
            sb.append("items.find({")
            sb.append("\"modified\": {\"\$gt\": \"$startTimeDateTime\"}")
            sb.append(",\"modified\": {\"\$lt\": \"$endTimeDateTime\"}")

            sb.append("}).include(\"property\")")
            sb.append(".sort({\"\$desc\": [\"modified\"]})")

            sb.append(".offset($offset)")
            sb.append(".limit($limit)")
            logger.info("searchFileByTime:$sb")
            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                logger.info("searchFileByTime result code:${response.code()},result message:${response.message()}")
                if (!response.isSuccessful) {
                    logger.error("Fail to search by regex. $responseContent")
                    throw RuntimeException("Fail to search by regex")
                }
                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
//                            "/${it.path.removePrefix(relativeParentPath)}/${it.name}",
                        it.path,
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchFileByTime cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    /**
     * 根据路径（目录）和 属性查询文件
     *
     * {
     *    "repo": "docker-local",
     *    "type": "file",
     *    "$and": [{
     *        "path":{"$match":"bk-custom/a90*"}
     *    },{
     *        "@userId":{"$eq":"jack"}
     *    }]
     * }
     *
     */
    fun searchByPathAndProperties(
        path: String,
        properties: Map<String, String>
    ): List<JFrogAQLFileInfo> {
        val startTimestamp = System.currentTimeMillis()

        val roadList = path.split("/")
        val repoKey = roadList.first()
        val filterPath = path.removePrefix("$repoKey/").removeSuffix("/")

        try {
            val sb = StringBuilder()
            sb.append("items.find(")
            sb.append("{\"repo\":\"$repoKey\",\"type\":\"file\"")
            sb.append(",\"\$and\":[{\"path\":{\"\$match\":\"$filterPath*\"}}")
            if (properties.isNotEmpty()) {
                sb.append(",")
                sb.append(properties.map { "{\"@${it.key}\":{\"\$eq\":\"${it.value}\"}}" }.joinToString(","))
            }
            sb.append("]})")

            val url = "$JFROG_BASE_URL/api/search/aql"
            val mediaType = MediaType.parse("text/plain")
            val requestBody = RequestBody.create(mediaType, sb.toString())
            val request = Request.Builder().url(url)
                .header("Authorization", makeCredential())
                .post(requestBody)
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("aql search failed. $responseContent")
                    throw RuntimeException("aql search failed")
                }

                val jFrogAQLFileInfoList =
                    objectMapper.readValue<JFrogAQLResponse<JFrogAQLFileInfo>>(responseContent).results
                return jFrogAQLFileInfoList.map {
                    JFrogAQLFileInfo(
                        "${it.path.removePrefix(filterPath)}/${it.name}",
                        it.name,
                        it.size,
                        it.created,
                        it.modified,
                        it.properties ?: emptyList()
                    )
                }
            }
        } finally {
            logger.info("searchFileAndPropertyByPropertyByAnd cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    private fun makeCredential(): String = Credentials.basic(JFROG_USERNAME!!, JFROG_PASSWORD!!)

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogAQLService::class.java)
    }
}