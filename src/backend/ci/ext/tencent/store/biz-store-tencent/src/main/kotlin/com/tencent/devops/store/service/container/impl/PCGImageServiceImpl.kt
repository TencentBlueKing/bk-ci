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

package com.tencent.devops.store.service.container.impl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.pojo.container.pcg.PCGDockerImage
import com.tencent.devops.store.service.container.PCGImageService
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@RefreshScope
class PCGImageServiceImpl @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) : PCGImageService {

    private val PCG_BG_ID = 29292L

    private val ALL_PROJECTS_ENALBE = "__devops__all_projects"
    private val PCG_IMAGE_ENABLE_REDIS_KEY = "atom:pcg:image:enable:key"

    @Value("\${pcg.image.url:#{null}}")
    private val pcgImageUrl: String? = "http://ciserver.wsd.com/interface?skey=8b116e40-5a06-4d17-a1a8-8a131f1b732d&operator=&interface_name=get_tc_container4web&interface_params={\"from\":\"landun\"}"

    private val projectEnableCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*projectId*/, Boolean/*Enable*/>(
            object : CacheLoader<String, Boolean>() {
                override fun load(projectId: String): Boolean {
                    try {
                        val members = redisOperation.getSetMembers(PCG_IMAGE_ENABLE_REDIS_KEY)
                        if (members != null && members.isNotEmpty()) {
                            val enable = members.contains(ALL_PROJECTS_ENALBE) || members.contains(projectId)
                            if (enable) {
                                logger.info("[$projectId] The project is already enable in redis")
                                return enable
                            }
                        }
                        val projectRecord = client.get(ServiceProjectResource::class).get(projectId)
                        if (projectRecord.isNotOk() || projectRecord.data == null) {
                            logger.warn("[$projectId] Fail to get the project detail - $projectRecord")
                            return false
                        }
                        val project = projectRecord.data!!
                        logger.info("[$projectId] Get the project - ($project)")
                        if (project.bgId == null) {
                            logger.warn("[$projectId] The bg id is empty")
                            return false
                        }
                        val projectEnable = project.bgId == PCG_BG_ID.toString()
                        logger.info("[$projectId] The project $projectEnable")
                        return projectEnable
                    } catch (t: Throwable) {
                        logger.warn("Fail to get the project detail - ($projectId)", t)
                    }
                    return false
                }
            }
        )

    private val pcgImageCache = ArrayList<PCGDockerImage>()
    private var pcgImageCacheLastUpdate: Long = 0

    override fun enableProject(projectId: String) {
        logger.info("[$projectId] Enable the pcg project")
        redisOperation.addSetValue(PCG_IMAGE_ENABLE_REDIS_KEY, projectId)
    }

    override fun disableProject(projectId: String) {
        logger.info("[$projectId] Disable the pcg project")
        redisOperation.removeSetMember(PCG_IMAGE_ENABLE_REDIS_KEY, projectId)
    }

    override fun projectEnable(projectId: String): Boolean {
        return try {
            projectEnableCache.get(projectId)
        } catch (t: Throwable) {
            logger.warn("Fail to get the pcg project enable image -[$projectId]")
            false
        }
    }

    override fun getPCGImages(): List<PCGDockerImage> {
        if (isImageCacheExpire()) {
            synchronized(this) {
                if (isImageCacheExpire()) {
                    reloadImage()
                }
            }
        }
        return ArrayList<PCGDockerImage>(pcgImageCache)
    }

    /**
     * Expire every 2 minutes
     */
    private fun isImageCacheExpire(): Boolean {
        if ((System.currentTimeMillis() - pcgImageCacheLastUpdate) >= 2 * 60 * 1000) {
            return true
        }
        return false
    }

    private fun reloadImage() {
        logger.info("Start to reload the image with url ($pcgImageUrl)")
        try {
            if (pcgImageUrl.isNullOrBlank()) {
                logger.warn("The pcg image url is empty")
                return
            }

            val request = Request.Builder()
                .url(pcgImageUrl!!)
                .get()
                .build()
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.warn("Fail to get the pcg images with url($pcgImageUrl) and response(${response.code()}|${response.message()}|$responseContent)")
                    return
                }
                pcgImageCache.clear()
                pcgImageCache.addAll(getImage(responseContent))
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to reload pcg image with url - ($pcgImageUrl)", ignored)
        } finally {
            pcgImageCacheLastUpdate = System.currentTimeMillis()
        }
    }

    private fun getImage(response: String): List<PCGDockerImage> {
        logger.info("Get the pcg response ($response)")
        val imageResponse: PCGImageResponse = objectMapper.readValue(response)
        if (imageResponse.ret_code != 200) {
            logger.warn("Fail to get the pcg response")
            return emptyList()
        }
        val result = ArrayList<PCGDockerImage>()
        imageResponse.data.forEach lit@{ from ->
            result.forEach { to ->
                if (from.img_name == to.img_name &&
                    from.img_ver == to.img_ver &&
                    from.language == to.language &&
                    from.os == to.os) {
                    return@lit
                }
            }
            result.add(from)
        }
        return result
    }

//    private val okHttpClient: OkHttpClient = okhttp3.OkHttpClient.Builder()
//        .connectTimeout(5L, TimeUnit.SECONDS)
//        .readTimeout(300*5L, TimeUnit.SECONDS) // Set to 15 minutes
//        .writeTimeout(60L, TimeUnit.SECONDS)
//        .build()

    companion object {
        private val logger = LoggerFactory.getLogger(PCGImageServiceImpl::class.java)
    }

    /**
     * {
     *   "data":[
     *     {
     *       "img_ver":"3.2.2.1.rc",
     *       "compile_env":"64位编译环境",
     *       "os":"tlinux",
     *       "img_name":"tc/tlinux/comm",
     *       "img_from":"maunl",
     *       "ip":"tc-tlinux-dsc",
     *       "name":"运营部业务分析组开发机",
     *       "language":"C++",
     *       "install_type":"new"
     *     },
     *     ..
     *     ..
     *     ..
     *   ],
     *   "call_chain":"anonymous",
     *   "err_msg":"",
     *   "sub_code":200,
     *   "record_cnt":493,
     *   "ret_code":200
     * }
     *
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PCGImageResponse(
        val data: List<PCGDockerImage>,
        val ret_code: Int
    )
}
