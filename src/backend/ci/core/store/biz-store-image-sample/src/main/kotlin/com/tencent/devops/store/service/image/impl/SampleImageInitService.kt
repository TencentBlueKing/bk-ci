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
package com.tencent.devops.store.service.image.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.store.api.image.op.pojo.ImageInitRequest
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.common.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.service.image.ImageReleaseService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class SampleImageInitService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val businessConfigDao: BusinessConfigDao,
    private val imageReleaseService: ImageReleaseService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SampleImageInitService::class.java)
        private const val DEFAULT_IMAGE_CODE = "bkci"
    }

    @Suppress("ALL")
    fun imageInit(imageInitRequest: ImageInitRequest?): Result<Boolean> {
        val projectCode = imageInitRequest?.projectCode ?: "demo"
        val userId = imageInitRequest?.userId ?: "admin"
        val imageCode = imageInitRequest?.imageCode ?: DEFAULT_IMAGE_CODE
        val accessToken = imageInitRequest?.accessToken ?: ""
        val ticketId = imageInitRequest?.ticketId
        logger.info("begin init image: $imageInitRequest")
        // 创建demo项目
        val demoProjectResult = client.get(ServiceProjectResource::class).get(projectCode)
        if (demoProjectResult.isNotOk()) {
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = demoProjectResult.code.toString(),
                defaultMessage = demoProjectResult.message
            )
        }
        if (demoProjectResult.isOk() && demoProjectResult.data == null) {
            val createDemoProjectResult = client.get(ServiceProjectResource::class).create(
                userId = userId,
                projectCreateInfo = ProjectCreateInfo(
                    projectName = imageInitRequest?.projectCode ?: "Demo",
                    englishName = projectCode,
                    description = imageInitRequest?.projectDesc ?: "demo project"
                )
            )
            if (createDemoProjectResult.isNotOk() || createDemoProjectResult.data != true) {
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = createDemoProjectResult.code.toString(),
                    defaultMessage = createDemoProjectResult.message
                )
            }
        }

        // 新增镜像
        val imageCount = imageDao.countByCode(dslContext, imageCode)
        if (imageCount != 0) {
            return Result(true)
        }
        val addImageResult = imageReleaseService.addMarketImage(
            accessToken = accessToken,
            userId = userId,
            imageCode = imageCode,
            marketImageRelRequest = MarketImageRelRequest(
                projectCode = projectCode,
                imageName = imageCode,
                imageSourceType = ImageType.THIRD,
                ticketId = ticketId
            ),
            needAuth = false
        )
        if (addImageResult.isNotOk() || addImageResult.data.isNullOrBlank()) {
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = addImageResult.status.toString(),
                defaultMessage = addImageResult.message
            )
        }
        // 更新镜像
        val updateImageResult = imageReleaseService.updateMarketImage(
            userId = userId,
            marketImageUpdateRequest = MarketImageUpdateRequest(
                imageCode = imageCode,
                imageName = imageCode,
                classifyCode = "BASE",
                labelIdList = null,
                category = "PIPELINE_JOB",
                agentTypeScope = ImageAgentTypeEnum.getAllAgentTypes(),
                summary = imageInitRequest?.summary ?: "CI basic image based on tlinux2.2",
                description = imageInitRequest?.description ?: "Docker public build machine base image",
                logoUrl = imageInitRequest?.logoUrl
                    ?: "/ms/artifactory/api/user/artifactories/file/download?filePath=%2Ffile%2Fpng%2FDOCKER.png&logo=true",
                iconData = imageInitRequest?.iconData,
                ticketId = ticketId,
                imageSourceType = ImageType.THIRD,
                imageRepoUrl = imageInitRequest?.imageRepoUrl ?: "",
                imageRepoName = imageInitRequest?.imageRepoName ?: "bkci/ci",
                imageTag = imageInitRequest?.imageTag ?: "latest",
                dockerFileType = imageInitRequest?.dockerFileType ?: "INPUT",
                dockerFileContent = imageInitRequest?.dockerFileContent
                    ?: "FROM bkci/ci:latest\nRUN apt install -y git python-pip python3-pip\n",
                version = "1.0.0",
                releaseType = ReleaseTypeEnum.NEW,
                versionContent = imageInitRequest?.versionContent ?: DEFAULT_IMAGE_CODE,
                publisher = userId
            ),
            checkLatest = false,
            sendCheckResultNotify = false,
            runCheckPipeline = false
        )
        if (updateImageResult.isNotOk() || updateImageResult.data.isNullOrBlank()) {
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = updateImageResult.status.toString(),
                defaultMessage = updateImageResult.message
            )
        }
        val imageId = updateImageResult.data!!
        // 自动让镜像测试通过
        imageReleaseService.approveImage(
            userId = userId,
            imageId = imageId,
            approveImageReq = ApproveImageReq(
                imageCode = imageCode,
                publicFlag = true,
                recommendFlag = true,
                certificationFlag = false,
                rdType = ImageRDTypeEnum.THIRD_PARTY,
                weight = 1,
                result = PASS,
                message = "ok"
            )
        )
        // 将改镜像设置成job选择时默认镜像
        val defaultJobImage = mapOf(
            "code" to imageCode,
            "version" to "1.*",
            "name" to imageCode,
            "recommendFlag" to true
        )
        businessConfigDao.add(
            dslContext, BusinessConfigRequest(
                business = BusinessEnum.BUILD_TYPE,
                feature = "defaultBuildResource",
                businessValue = BuildType.DOCKER.name,
                configValue = JsonUtil.toJson(defaultJobImage),
                description = "default job image"
            )
        )
        // 同步修改镜像检查流水线
        if (imageCode != DEFAULT_IMAGE_CODE) {
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = dslContext,
                business = BusinessEnum.IMAGE.name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            pipelineModelConfig?.let {
                val pipelineModelStr = pipelineModelConfig.configValue.replace(DEFAULT_IMAGE_CODE, imageCode)
                businessConfigDao.update(
                    dslContext = dslContext,
                    request = BusinessConfigRequest(
                        business = BusinessEnum.IMAGE,
                        feature = pipelineModelConfig.feature,
                        businessValue = pipelineModelConfig.businessValue,
                        configValue = pipelineModelStr,
                        description = pipelineModelConfig.description
                    )
                )
            }
        }
        return Result(true)
    }
}
