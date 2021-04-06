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

package com.tencent.bkrepo.dockerapi.service.harbor

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.dockerapi.client.HarborClient
import com.tencent.bkrepo.dockerapi.config.HarborProperties
import com.tencent.bkrepo.dockerapi.constant.DEFAULT_DOCKER_REPO_NAME
import com.tencent.bkrepo.dockerapi.constant.PUBLIC_PROJECT_ID
import com.tencent.bkrepo.dockerapi.pojo.DockerRepo
import com.tencent.bkrepo.dockerapi.pojo.DockerTag
import com.tencent.bkrepo.dockerapi.pojo.QueryImageTagRequest
import com.tencent.bkrepo.dockerapi.pojo.QueryProjectImageRequest
import com.tencent.bkrepo.dockerapi.pojo.QueryPublicImageRequest
import com.tencent.bkrepo.dockerapi.service.ImageService
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@ConditionalOnProperty(prefix = "dockerapi", name = ["realm"], havingValue = "harbor")
class HarborImageServiceImpl(
    private val harborClient: HarborClient,
    private val harborProperties: HarborProperties
) : ImageService {
    override fun queryPublicImage(request: QueryPublicImageRequest): Page<DockerRepo> {
        with(request) {
            return queryProjectImage(
                QueryProjectImageRequest(
                    searchKey,
                    PUBLIC_PROJECT_ID, DEFAULT_DOCKER_REPO_NAME, pageNumber, pageSize
                )
            )
        }
    }

    override fun queryProjectImage(request: QueryProjectImageRequest): Page<DockerRepo> {
        logger.info("queryProjectImage, request: $request")
        val projectId = request.projectId
        val pageNumber = request.pageNumber
        val pageSize = request.pageSize
        val searchKey = request.searchKey ?: ""
        var harborProject = harborClient.getProjectByName(projectId)
        if (harborProject == null) {
            harborClient.createProject(projectId)
            harborProject = harborClient.getProjectByName(projectId)
        }
        val harborRepos = harborClient.listImage(harborProject!!.projectId, searchKey, pageNumber, pageSize)
        val repoType = if (projectId == "public") {
            "public"
        } else {
            "private"
        }
        val images = harborRepos.map {
            DockerRepo(
                type = repoType,
                createdBy = "system",
                created = DateTime(it.createTime).toString("yyyy-MM-dd HH:mm:ss"),
                modified = DateTime(it.updateTime).toString("yyyy-MM-dd HH:mm:ss"),
                imageName = it.name,
                imagePath = it.name,
                tagCount = it.tagsCount.toLong(),
                downloadCount = it.pullCount.toLong()
            )
        }

        if (harborRepos.size < pageSize && pageNumber == 1) {
            return Page(pageNumber, pageSize, harborRepos.size.toLong(), images)
        }

        // 迂回获取查询总repo数
        var tmpPage = 1
        var tmpTotal = 0
        while (true) {
            var moreHarborRepos = harborClient.listImage(harborProject.projectId, searchKey, tmpPage, 100)
            if (moreHarborRepos.size < 100) {
                tmpTotal += moreHarborRepos.size
                break
            } else {
                tmpTotal += 100
                tmpPage++
            }
        }

        return Page(pageNumber, pageSize, tmpTotal.toLong(), images)
    }

    override fun queryImageTag(request: QueryImageTagRequest): Page<DockerTag> {
        logger.info("queryImageTag, request: $request")
        val imageRepo = request.imageRepo
        val harborTags = harborClient.listTag(imageRepo)
        val tags = harborTags.map {
            val createdStr = DateTime(it.created).toString("yyyy-MM-dd HH:mm:ss")
            DockerTag(
                tag = it.name,
                imageName = imageRepo,
                imagePath = imageRepo,
                image = "${harborProperties.imagePrefix}/$imageRepo:${it.name}",
                createdBy = "system",
                created = createdStr,
                size = HumanReadable.size(it.size),
                modified = createdStr
                // modifiedBy  无
            )
        }
        return Page(request.pageNumber, request.pageSize, tags.size.toLong(), tags)
    }

    private fun getPrintSize(size: Long): String {
        if (size < 0) {
            throw ArithmeticException("size must be larger than 0")
        }

        var doubleSize = size.toDouble()
        if (doubleSize < 1024) {
            return "$doubleSize bytes"
        } else {
            doubleSize = BigDecimal(doubleSize / 1024).setScale(2, BigDecimal.ROUND_DOWN).toDouble()
        }
        if (doubleSize < 1024) {
            return "$doubleSize KB"
        } else {
            doubleSize = BigDecimal(doubleSize / 1024).setScale(2, BigDecimal.ROUND_DOWN).toDouble()
        }
        return if (doubleSize < 1024) {
            "$doubleSize MB"
        } else {
            "${BigDecimal(doubleSize / 1024).setScale(2, BigDecimal.ROUND_DOWN).toDouble()} GB"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HarborImageServiceImpl::class.java)
    }
}
