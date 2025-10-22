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

package com.tencent.devops.image.resources

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.image.pojo.ImageListResp
import com.tencent.devops.image.pojo.ImagePageData
import com.tencent.devops.image.service.ImageArtifactoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class ServiceImageResourceImpl @Autowired constructor(
    private val artifactoryService: ImageArtifactoryService
) : ServiceImageResource {
    companion object {
        private val logger = LoggerFactory.getLogger(UserImageResourceImpl::class.java)
    }

    override fun listDockerBuildImages(userId: String, projectId: String): Result<List<DockerTag>> {
        checkUserAndProject(userId, projectId)
        try {
            return Result(artifactoryService.listDockerBuildImages(projectId))
        } catch (e: Exception) {
            logger.error("list docker build image failed", e)
            throw RuntimeException("list docker build image failed")
        }
    }

    override fun setBuildImage(
        userId: String,
        projectId: String,
        imageRepo: String,
        imageTag: String
    ): Result<Boolean> {
        if (imageRepo.isBlank()) {
            throw OperationException("imageRepo required")
        }
        if (imageTag.isBlank()) {
            throw OperationException("imageTag required")
        }

        return try {
            Result(artifactoryService.copyToBuildImage(projectId, imageRepo, imageTag))
        } catch (e: OperationException) {
            Result(1, e.message!!)
        }
    }

    override fun listPublicImages(userId: String, searchKey: String?, start: Int?, limit: Int?): Result<ImagePageData> {
        val vSearchKey = searchKey ?: ""
        val (vStart, vLimit) = pair(start, limit)
        return Result(artifactoryService.listPublicImages(vSearchKey, vStart, vLimit))
    }

    override fun listProjectImages(
        userId: String,
        projectId: String,
        searchKey: String?,
        start: Int?,
        limit: Int?
    ): Result<ImagePageData> {
        val vSearchKey = searchKey ?: ""
        val (vStart, vLimit) = pair(start, limit)
        return Result(artifactoryService.listProjectImages(projectId, vSearchKey, vStart, vLimit))
    }

    override fun listAllPublicImages(userId: String, searchKey: String?): Result<ImageListResp> {
        return Result(artifactoryService.listAllPublicImages(searchKey))
    }

    override fun listAllProjectImages(userId: String, projectId: String, searchKey: String?): Result<ImageListResp> {
        return Result(artifactoryService.listAllProjectImages(projectId, searchKey))
    }

    override fun getImageInfo(userId: String, imageRepo: String, tagStart: Int?, tagLimit: Int?): Result<DockerRepo?> {
        val (vStart, vLimit) = pair(tagStart, tagLimit)
        return Result(artifactoryService.getImageInfo(imageRepo, true, vStart, vLimit))
    }

    private fun pair(start: Int?, limit: Int?): Pair<Int, Int> {
        val vStart = if (start == null || start == 0) 0 else start
        val vLimit = if (limit == null || limit == 0) 10000 else limit
        return Pair(vStart, vLimit)
    }

    override fun getTagInfo(userId: String, imageRepo: String, imageTag: String): Result<DockerTag?> {
        return Result(artifactoryService.getTagInfo(imageRepo, imageTag))
    }

    override fun listDevCloudImages(userId: String, projectId: String, public: Boolean): Result<List<DockerTag>> {
        checkUserAndProject(userId, projectId)

        try {
            return Result(artifactoryService.listDevCloudImages(projectId, public))
        } catch (e: Exception) {
            logger.error("list dev cloud image failed", e)
            throw RuntimeException("list dev cloud image failed")
        }
    }

    private fun checkUserAndProject(userId: String, projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId required")
        }
        if (userId.isBlank()) {
            throw ParamBlankException("userId required")
        }
    }
}
