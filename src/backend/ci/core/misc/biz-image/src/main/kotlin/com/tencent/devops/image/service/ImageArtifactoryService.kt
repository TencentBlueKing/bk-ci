/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.REPO_NAME_IMAGE
import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.config.BKRepoConfig
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
    private val dockerConfig: DockerConfig,
    private val client: Client,
    private val bKRepoConfig: BKRepoConfig,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ImageArtifactoryService::class.java)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    }

    private val credential: String

    init {
        credential = makeCredential()
    }

    private fun generateListPublicImageAql(searchKey: String): String {
        return "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/public/*\"}}," +
            "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
    }

    fun listProjectImages(
        userId: String,
        projectId: String,
        repoName: String = REPO_NAME_IMAGE,
        searchKey: String?,
        pageNumber: Int,
        pageSize: Int
    ): ImagePageData {
        val imagePackagePage = client.get(ServiceArtifactoryResource::class).listPackagePage(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            packageName = searchKey,
            pageNumber = pageNumber,
            pageSize = pageSize
        ).data
        val imageList = mutableListOf<DockerRepo>()
        imagePackagePage?.forEach { imagePackage ->
            imageList.add(
                DockerRepo(
                    repoUrl = bKRepoConfig.repoUrl,
                    projectId = imagePackage.projectId,
                    repo = imagePackage.repoName,
                    type = imagePackage.type,
                    repoType = "DOCKER",
                    name = imagePackage.name,
                    createdBy = imagePackage.createdBy,
                    created = imagePackage.createdDate,
                    modifiedBy = imagePackage.lastModifiedBy,
                    modified = imagePackage.lastModifiedDate,
                    desc = imagePackage.description,
                    downloadCount = imagePackage.downloads.toInt()
                )
            )
        }
        return ImagePageData(
            imageList = imageList,
            pageNumber = pageNumber,
            pageSize = pageSize,
            total = imagePackagePage?.size ?: 0
        )
    }

    private fun generateListProjectImagesAql(projectCode: String, searchKey: String): String {
        return "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
            "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/$projectCode/*\"}}," +
            "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
    }

    fun listAllProjectImages(
        userId: String,
        projectId: String,
        searchKey: String? = null,
        repoName: String = REPO_NAME_IMAGE,
        pageNumber: Int = 1,
        pageSize: Int = 100
    ): ImageListResp {
        val imageItems = client.get(ServiceArtifactoryResource::class).listPackagePage(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            packageName = searchKey,
            pageNumber = pageNumber,
            pageSize = pageSize
        ).data?.map {
            ImageItem(
                repoUrl = bKRepoConfig.repoUrl!!,
                repo = "${it.projectId}/${it.repoName}",
                name = it.name
            )
        }
        return ImageListResp(
            imageItems ?: emptyList()
        )
    }

    fun listAllPublicImages(searchKey: String?): ImageListResp {
        return listAllProjectImages(
            userId = SYSTEM,
            projectId = bKRepoConfig.repoProject!!,
            searchKey = searchKey
        )
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

    fun getImageInfo(
        userId: String,
        projectId: String,
        packageKey: String,
        repoName: String,
        includeTagDetail: Boolean = false,
        tagStart: Int = 0,
        tagLimit: Int = 100
    ): DockerRepo? {
        val imagePackage = client.get(ServiceArtifactoryResource::class).getPackageInfo(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            packageKey = packageKey
        ).data
        val imageVersions = if (includeTagDetail) {
            client.get(ServiceArtifactoryResource::class).listVersionPage(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                packageKey = packageKey,
                pageNumber = tagStart,
                pageSize = tagLimit
            ).data
        } else {
            null
        }

        if (imagePackage != null) {
            return DockerRepo(
                repoUrl = bKRepoConfig.repoUrl,
                projectId = imagePackage.projectId,
                repo = imagePackage.repoName,
                type = imagePackage.type,
                repoType = "DOCKER",
                name = imagePackage.name,
                createdBy = imagePackage.createdBy,
                created = imagePackage.createdDate,
                modifiedBy = imagePackage.lastModifiedBy,
                modified = imagePackage.lastModifiedDate,
                desc = imagePackage.description,
                downloadCount = imagePackage.downloads.toInt(),
                tags = imageVersions?.records?.map {
                    DockerTag(
                        tag = it.name,
                        projectId = imagePackage.projectId,
                        repoName = imagePackage.repoName,
                        imageName = imagePackage.name,
                        createdBy = it.createdBy,
                        created = it.createdDate,
                        modified = it.lastModifiedDate,
                        modifiedBy = it.lastModifiedBy,
                        size = it.size
                    )
                },
                tagCount = imageVersions?.count?.toInt(),
                tagStart = imageVersions?.let { tagStart },
                tagLimit = imageVersions?.let { tagLimit }
            )
        }
        return null
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

    fun getTagInfo(
        userId: String,
        projectId: String,
        repoName: String,
        imageName:String,
        imageTag: String
    ): DockerTag? {
        val imageVersions =
            client.get(ServiceArtifactoryResource::class).listVersionPage(
                userId = userId,
                projectId = projectId,
                repoName = repoName,
                packageKey = "docker://$imageName",
                version = imageTag
            ).data
        imageVersions?.records?.forEach {
            if (it.name == imageTag) {
                return DockerTag(
                    tag = it.name,
                    projectId = projectId,
                    repoName = repoName,
                    imageName = imageName,
                    createdBy = it.createdBy,
                    created = it.createdDate,
                    modified = it.lastModifiedDate,
                    modifiedBy = it.lastModifiedBy,
                    size = it.size
                )
            }
        }
        return null
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

    fun listDockerImages(
        userId: String,
        projectId: String,
        repoName: String = REPO_NAME_IMAGE,
        searchKey: String? = null,
        pageNumber: Int = 1,
        pageSize: Int = 100
    ): List<DockerTag>? {
        val imagePackagePage = client.get(ServiceArtifactoryResource::class).listPackagePage(
            userId = userId,
            projectId = projectId,
            repoName = repoName,
            packageName = searchKey,
            pageNumber = pageNumber,
            pageSize = pageSize
        ).data
        return imagePackagePage?.map {
            DockerTag(
                tag = it.latest,
                projectId = projectId,
                repoName = repoName,
                imageName = it.name
            )
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

    fun listProjectBuildImages(projectCode: String, searchKey: String, start: Int, limit: Int): ImagePageData {
        val aql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
                "{\"name\":{\"\$eq\":\"manifest.json\"}},{\"path\":{\"\$match\":\"paas/bkdevops/$projectCode/*\"}}," +
                "{\"@docker.repoName\":{\"\$match\":\"*$searchKey*\"}}]}).include(\"property.key\",\"property.value\")"
        logger.info("aql: $aql")

        return listProjectImagesByAql(aql, start, limit)
    }

    fun listDockerBuildImages(projectId: String): List<DockerTag> {
        val aql = "items.find({\"\$and\":[{\"repo\":{\"\$eq\":\"docker-local\"}}," +
                "{\"name\":{\"\$eq\":\"manifest.json\"}}," +
                "{\"path\":{\"\$match\":\"paas/bkdevops/$projectId/*\"}}]}).include(\"property.key\",\"property.value\")"
        logger.info("aql: $aql")

        return aqlSearchImage(aql)
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

    private fun makeCredential(): String =
        Credentials.basic(dockerConfig.registryUsername!!, SecurityUtil.decrypt(dockerConfig.registryPassword!!))
}
