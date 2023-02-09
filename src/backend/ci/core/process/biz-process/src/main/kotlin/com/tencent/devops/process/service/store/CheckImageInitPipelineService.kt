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

package com.tencent.devops.process.service.store

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.process.pojo.CheckImageInitPipelineResp
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class CheckImageInitPipelineService @Autowired constructor(
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {
    private val logger = LoggerFactory.getLogger(CheckImageInitPipelineService::class.java)

    /**
     * 初始化流水线进行验证镜像合法性
     */
    fun initCheckImagePipeline(
        userId: String,
        projectCode: String,
        checkImageInitPipelineReq: CheckImageInitPipelineReq
    ): Result<CheckImageInitPipelineResp> {
        val imageCode = checkImageInitPipelineReq.imageCode
        val imageName = checkImageInitPipelineReq.imageName
        val version = checkImageInitPipelineReq.version
        val imageType = checkImageInitPipelineReq.imageType
        val registryUser = checkImageInitPipelineReq.registryUser
        val registryPwd = checkImageInitPipelineReq.registryPwd
        // 保存流水线信息
        val model = JsonUtil.to(checkImageInitPipelineReq.pipelineModel, Model::class.java)
        val pipelineId = pipelineInfoFacadeService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams["imageCode"] = imageCode
        startParams["imageName"] = imageName
        startParams["version"] = version
        imageType?.let { startParams["imageType"] = it }
        registryUser?.let { startParams["registryUser"] = it }
        registryPwd?.let { startParams["registryPwd"] = it }
        var imageCheckStatus = ImageStatusEnum.CHECKING
        var buildId: String? = null
        try {
            buildId = pipelineBuildFacadeService.buildManualStartup(
                userId = userId,
                startType = StartType.SERVICE,
                projectId = projectCode,
                pipelineId = pipelineId,
                values = startParams,
                channelCode = ChannelCode.AM,
                checkPermission = false,
                isMobile = false,
                startByMessage = null
            )
            logger.info("buildManualStartup result is:$buildId")
        } catch (t: Throwable) {
            logger.error("$pipelineId buildManualStartup error:", t)
            imageCheckStatus = ImageStatusEnum.CHECK_FAIL
        }
        return Result(CheckImageInitPipelineResp(pipelineId, buildId, imageCheckStatus))
    }
}
