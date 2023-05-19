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

package com.tencent.devops.image.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.config.DockerConfig
import com.tencent.devops.image.constants.ImageMessageCode.IMAGE_COPYING_IN_PROGRESS
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.pojo.ImageItem
import com.tencent.devops.image.pojo.ImageListResp
import com.tencent.devops.image.pojo.ImagePageData
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class ImageArtifactoryService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dockerConfig: DockerConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ImageArtifactoryService::class.java)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    }

    private val credential: String

    init {
        credential = makeCredential()
    }

    fun listPublicImages(searchKey: String, start: Int, limit: Int): ImagePageData {
        val aql = generateListPublicImageAql(searchKey)
        logger.info("aql: $aql")

        val images = aqlSearchImage(aql)
        val repoNames = images.map { it.repo }.toSet().toList().sortedBy { it }
        val repos = repoNames.map {
            DockerRepo().apply {
                repoUrl = dockerConfig.imagePrefix
                repo = it
                type = "public"
                createdBy = "system"
                name = parseName(it!!)
            }
        }

        val total = repos.size
        val pageRange = getPageIndex(total, start, limit)
        val resultRepos = repos.subList(pageRange.first, pageRange.second)

        return ImagePageData(resultRepos, start, limit, total)
    }

    private fun generateListPublicImageAql(searchKey: String): String {
        return "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/public/*\"}}," +
            "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
    }

    private fun listProjectImagesByAql(aql: String, start: Int, limit: Int): ImagePageData {
        val images = aqlSearchImage(aql)
        val repoNames = images.map { it.repo }.toSet().toList().sortedBy { it }
        val repos = repoNames.map {
            DockerRepo().apply {
                repoUrl = dockerConfig.imagePrefix
                repo = it
                type = "private"
                createdBy = "system"
                name = parseName(it!!)
            }
        }

        val total = repos.size
        val pageRange = getPageIndex(total, start, limit)
        val resultRepos = repos.subList(pageRange.first, pageRange.second)

        return ImagePageData(resultRepos, start, limit, total)
    }

    fun listProjectImages(projectCode: String, searchKey: String, start: Int, limit: Int): ImagePageData {
        val aql = generateListProjectImagesAql(projectCode, searchKey)
        logger.info("aql: $aql")

        return listProjectImagesByAql(aql, start, limit)
    }

    private fun generateListProjectImagesAql(projectCode: String, searchKey: String): String {
        return "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/$projectCode/*\"}}," +
            "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
    }

    fun listAllProjectImages(projectCode: String, searchKey: String?): ImageListResp {
        // 查询项目镜像列表
        val aql = generateListProjectImagesAql(projectCode, searchKey ?: "")
        val projectImages = aqlSearchImage(aql)
        val imageList = mutableListOf<ImageItem>()
        handleImageList(projectImages, imageList)
        // 获取项目Docker构建镜像列表
        val dockerProjectImages = listDockerBuildImages(projectCode)
        handleImageList(dockerProjectImages, imageList)
        // 获取项目devCloud镜像列表
        val devCloudProjectImages = listDevCloudImages(projectCode, false)
        handleImageList(devCloudProjectImages, imageList)
        return ImageListResp(imageList)
    }

    fun listAllPublicImages(searchKey: String?): ImageListResp {
        // 查询项目镜像列表
        val aql = generateListPublicImageAql(searchKey ?: "")
        val publicImages = aqlSearchImage(aql)
        val imageList = mutableListOf<ImageItem>()
        handleImageList(publicImages, imageList)
        // 获取项目devCloud镜像列表
        val devCloudPublicImages = listDevCloudImages("", true)
        handleImageList(devCloudPublicImages, imageList)
        return ImageListResp(imageList)
    }

    private fun handleImageList(images: List<DockerTag>, imageList: MutableList<ImageItem>) {
        val repoNames = images.map { it.repo }.toSet().toList().sortedBy { it }
        repoNames.forEach {
            imageList.add(
                ImageItem(
                    repoUrl = dockerConfig.imagePrefix!!,
                    repo = it!!,
                    name = parseName(it)
                )
            )
        }
    }

    fun listProjectBuildImages(projectCode: String, searchKey: String, start: Int, limit: Int): ImagePageData {
        val aql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/bkdevops/$projectCode/*\"}}," +
            "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
        logger.info("aql: $aql")

        return listProjectImagesByAql(aql, start, limit)
    }

    fun getBuildImageInfo(
        imageRepo: String,
        includeTagDetail: Boolean = false,
        tagStart: Int = 0,
        tagLimit: Int = 1000
    ): DockerRepo? {
        val aql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}}," +
            "{\"@docker.repoName\":\"$imageRepo\"}]}).include(\"property.key\",\"property.value\")"
        var buildImages = aqlSearchImage(aql)
        if (buildImages.isEmpty()) {
            return null
        }

        buildImages = buildImages.sortedByDescending { it.modified }
        val pageIndex = getPageIndex(buildImages.size, tagStart, tagLimit)

        val resultTags = buildImages.subList(pageIndex.first, pageIndex.second)
        logger.info("includeTagDetail: $includeTagDetail")
        resultTags.forEach {
            if (includeTagDetail) {
                val tagInfo = getTagInfo(it.repo!!, it.tag!!)
                if (tagInfo == null) {
                    logger.error("image tag not found")
                    throw RuntimeException("image tag not found")
                }
                it.size = tagInfo.size
            }
        }

        val firstImage = buildImages[0]
        return DockerRepo().apply {
            repoUrl = dockerConfig.imagePrefix
            repo = imageRepo
            type = parseType(imageRepo)
            repoType = ""
            name = parseName(imageRepo)
            created = firstImage.created
            createdBy = firstImage.createdBy
            modified = firstImage.modified
            modifiedBy = firstImage.modifiedBy
            imagePath = parseBuildImagePath(imageRepo)
            tags = resultTags
            tagCount = buildImages.size
            this.tagStart = tagStart
            this.tagLimit = tagLimit
            downloadCount = 0 // TODO 实现下载次数统计
        }
    }

    fun getImageInfo(
        imageRepo: String,
        includeTagDetail: Boolean = false,
        tagStart: Int = 0,
        tagLimit: Int = 1000
    ): DockerRepo? {
        val devAql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"@docker.repoName\":\"$imageRepo\"}]})" +
            ".include(\"property.key\",\"property.value\")"
        var devImages = aqlSearchImage(devAql)
        if (devImages.isEmpty()) {
            return null
        }

        val prodAql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-prod-for-publish\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}}," +
            "{\"@docker.repoName\":\"$imageRepo\"}]}).include(\"property.key\",\"property.value\")"
        val prodImage = aqlSearchImage(prodAql)
        val prodImageSet = prodImage.map { it.image }.toSet()

        devImages = devImages.sortedByDescending { it.modified }
        val pageIndex = getPageIndex(devImages.size, tagStart, tagLimit)

        val resultTags = devImages.subList(pageIndex.first, pageIndex.second)
        logger.info("includeTagDetail: $includeTagDetail")
        resultTags.forEach {
            if (prodImageSet.contains(it.image)) {
                it.artifactorys = listOf("DEV", "PROD")
            } else {
                it.artifactorys = listOf("DEV")
            }

            if (includeTagDetail) {
                val tagInfo = getTagInfo(it.repo!!, it.tag!!)
                if (tagInfo == null) {
                    logger.error("image tag not found")
                    throw RuntimeException("image tag not found")
                }
                it.size = tagInfo.size
            }
        }

        val firstImage = devImages[0]
        return DockerRepo().apply {
            repoUrl = dockerConfig.imagePrefix
            repo = imageRepo
            type = parseType(imageRepo)
            repoType = ""
            name = parseName(imageRepo)
            created = firstImage.created
            createdBy = firstImage.createdBy
            modified = firstImage.modified
            modifiedBy = firstImage.modifiedBy
            imagePath = parseImagePath(imageRepo)
            tags = resultTags
            tagCount = devImages.size
            this.tagStart = tagStart
            this.tagLimit = tagLimit
            downloadCount = 0 // TODO 实现下载次数统计
        }
    }

    fun parseImagePath(imageRepo: String): String {
        val prefixIndex = imageRepo.indexOf("/", 5/* 前缀为 "paas/" */)
        return if (prefixIndex == -1) {
            imageRepo
        } else {
            imageRepo.substring(prefixIndex + 1)
        }
    }

    fun parseBuildImagePath(imageRepo: String): String {
        val prefixIndex = imageRepo.indexOf("/", 14/* 前缀为 "paas/bkdevops/" */)
        return if (prefixIndex == -1) {
            imageRepo
        } else {
            imageRepo.substring(prefixIndex + 1)
        }
    }

    fun getTagInfo(imageRepo: String, imageTag: String): DockerTag? {
        val url = "${dockerConfig.registryUrl}/api/views/dockerv2"
        val requestData = mapOf(
            "path" to "$imageRepo/$imageTag",
            "repoKey" to "docker-local",
            "view" to "dockerv2"
        )

        val requestBody = ObjectMapper().writeValueAsString(requestData)
        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()

        val request = Request.Builder().url(url)
            .post(RequestBody.create(JSON, requestBody))
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                if (!response.isSuccessful) {
                    logger.error("get tag info failed, statusCode: ${response.code}")
                    throw RuntimeException("get tag info failed")
                }

                val responseBody = response.body?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)

                val tagInfo = responseData["tagInfo"] as Map<String, Any>
                val totalSize = tagInfo["totalSize"] as String

                return DockerTag().apply {
                    size = totalSize
                }
            } catch (e: Exception) {
                logger.error("get tag info failed", e)
                throw RuntimeException("get tag info failed")
            }
        }
    }

    private fun parseType(repoName: String): String {
        val splits = repoName.split("/")
        return if (splits.size >= 2 && "public" == splits[1]) "public" else "private"
    }

    private fun parseName(repoName: String): String {
        val splits = repoName.split("/")
        return if (splits.size > 2) {
            splits.subList(2, splits.size).joinToString("/")
        } else {
            repoName
        }
    }

    private fun aqlSearchImage(aql: String): List<DockerTag> {
        val url = "${dockerConfig.registryUrl}/api/search/aql"

        logger.info("POST url: $url")
        logger.info("requestAql: $aql")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
        val request = Request.Builder().url(url)
            .post(RequestBody.create(null, aql))
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                if (!response.isSuccessful) {
                    logger.error("sql search failed, statusCode: ${response.code}")
                    throw RuntimeException("aql search failed")
                }

                val responseBody = response.body?.string()
                logger.info("responseBody: $responseBody")
                return parseImages(responseBody!!)
            } catch (e: Exception) {
                logger.error("aql search failed", e)
                throw RuntimeException("aql search failed")
            }
        }
    }

    private fun parseImages(dataStr: String): List<DockerTag> {
        val responseData: Map<String, Any> = jacksonObjectMapper().readValue(dataStr)
        val results = responseData["results"] as List<Map<String, Any>>
        val images = mutableListOf<DockerTag>()
        for (it in results) {
            val dockerTag = DockerTag()
            dockerTag.created = DateTime(it["created"] as String).toString("yyyy-MM-dd HH:mm:ss")
            dockerTag.createdBy = it["created_by"] as String
            dockerTag.modified = DateTime(it["modified"] as String).toString("yyyy-MM-dd HH:mm:ss")
            dockerTag.modifiedBy = it["modified_by"] as String

            val properties = it["properties"] as List<Map<String, Any>>
            for (item in properties) {
                val key = item["key"] ?: ""
                val value = item["value"] ?: ""
                if (key == "docker.manifest") {
                    dockerTag.tag = value as String
                    continue
                }
                if (key == "docker.repoName") {
                    dockerTag.repo = value as String
                    continue
                }
                if (key == "devops.creator") {
                    dockerTag.createdBy = value as String
                }
                if (key == "devops.desc") {
                    dockerTag.desc = value as String
                }
            }

            // 过滤掉路径跟repo匹配不上的镜像
            val path = it["path"] as String
            if (path.contains('/')) {
                if (path.substring(0, path.lastIndexOf("/")) != dockerTag.repo) {
                    continue
                }
            }

            dockerTag.image = "${dockerConfig.imagePrefix}/${dockerTag.repo}:${dockerTag.tag}"
            images.add(dockerTag)
        }
        return images
    }

    private fun getPageIndex(total: Int, start: Int, limit: Int): Pair<Int, Int> {
        var pStart = start
        var pLimit = limit

        if (start < 0) {
            pStart = 0
        }
        if (limit <= 0) {
            pLimit = 10000
        }
        if (total <= 0 || start >= total) {
            return Pair(0, 0)
        }

        val startIndex = pStart
        var endIndex = pStart + pLimit
        if (endIndex > total) {
            endIndex = total
        }

        return Pair(startIndex, endIndex)
    }

    fun listDockerBuildImages(projectId: String): List<DockerTag> {
        val aql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}}," +
            "{\"path\":{\"\$match\":\"paas/bkdevops/$projectId/*\"}}]}).include(\"property.key\",\"property.value\")"
        logger.info("aql: $aql")

        return aqlSearchImage(aql)
    }

    fun listDevCloudImages(projectId: String, public: Boolean): List<DockerTag> {
        val aql = if (public) {
            "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}},{\"name\":{\"\$eq\":\"manifest.json\"}}," +
                "{\"path\":{\"\$match\":\"devcloud/public/*\"}}]}).include(\"property.key\",\"property.value\")"
        } else {
            "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}},{\"name\":{\"\$eq\":\"manifest.json\"}}," +
                "{\"path\":{\"\$match\":\"devcloud/$projectId/*\"}}]}).include(\"property.key\",\"property.value\")"
        }

        logger.info("aql: $aql")

        return aqlSearchImage(aql)
    }

    fun deleteItem(path: String) {
        val url = "${dockerConfig.registryUrl}/docker-local/$path"
        logger.info("DELETE url: $url")
//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
        val request = Request.Builder().url(url).delete()
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                if (!response.isSuccessful && response.code != 404) {
                    val responseBody = response.body?.string()
                    logger.error("delete item failed, responseBody: ", responseBody)
                    throw OperationException("delete Item failed")
                }
            } catch (e: Exception) {
                logger.error("delete item error: ", e)
                throw OperationException("delete item error")
            }
        }
    }

    fun checkItemExists(path: String): Boolean {
        val url = "${dockerConfig.registryUrl}/api/storage/docker-local/$path"
        logger.info("GET url: $url")
//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
        val request = Request.Builder().url(url).get()
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//                val response = call.execute()
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return false
                    } else {
                        val responseBody = response.body?.string()
                        logger.error("check item failed, responseBody: ", responseBody)
                        throw OperationException("check item failed")
                    }
                }
                return true
            } catch (e: Exception) {
                logger.error("check item error: ", e)
                throw OperationException("check item error")
            }
        }
    }

    fun copyItem(fromPath: String, toPath: String) {
        val url = "${dockerConfig.registryUrl}/api/copy/docker-local/$fromPath?to=/docker-local/$toPath"
        logger.info("POST url: $url")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
        val request = Request.Builder().url(url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), ""))
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string()
                    logger.error("copy item failed, responseBody: $responseBody}")
                    throw RuntimeException("aql search failed")
                }
            } catch (e: Exception) {
                logger.error("aql search failed", e)
                throw RuntimeException("aql search failed")
            }
        }
    }

    fun setItemPropertie(path: String, key: String, value: String) {
        val url = "${dockerConfig.registryUrl}/api/storage/docker-local/$path?properties=$key=$value"
        logger.info("PUT url: $url")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
        val request = Request.Builder().url(url)
            .put(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), ""))
            .header("Authorization", credential)
            .build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//                val response = call.execute()
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string()
                    logger.error("set item properties failed, responseBody: $responseBody}")
                    throw RuntimeException("set item properties failed")
                }
            } catch (e: Exception) {
                logger.error("set item properties error", e)
                throw RuntimeException("set item properties error")
            }
        }
    }

    fun copyToBuildImage(projectId: String, imageRepo: String, imageTag: String): Boolean {
        val copyFrom = "$imageRepo/$imageTag"
        val toImageRepo = when {
            imageRepo.startsWith("paas/$projectId/") -> {
                "paas/bkdevops/$projectId${imageRepo.removePrefix("paas/$projectId")}"
            }
            imageRepo.startsWith("devcloud/$projectId/") -> {
                "paas/bkdevops/$projectId${imageRepo.removePrefix("devcloud/$projectId")}"
            }
            else -> throw OperationException("imageRepo param error")
        }

        val copyTo = "$toImageRepo/$imageTag"
        logger.info("copyFrom, $copyFrom")
        logger.info("copyTo, $copyTo")
        val redisKey = "image.copyToBuildImage_$copyFrom"
        if (!redisOperation.get(redisKey).isNullOrBlank()) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(IMAGE_COPYING_IN_PROGRESS)
            )
        }

        redisOperation.set(redisKey, "true")
        try {
            if (checkItemExists(copyTo)) {
                deleteItem(copyTo)
            }
            copyItem(copyFrom, copyTo)
            val manifestPath = "$copyTo/manifest.json"
            setItemPropertie(manifestPath, "docker.repoName", toImageRepo)
            return true
        } finally {
            redisOperation.delete(redisKey)
        }
    }

    private fun makeCredential(): String =
        Credentials.basic(dockerConfig.registryUsername!!, SecurityUtil.decrypt(dockerConfig.registryPassword!!))
}
