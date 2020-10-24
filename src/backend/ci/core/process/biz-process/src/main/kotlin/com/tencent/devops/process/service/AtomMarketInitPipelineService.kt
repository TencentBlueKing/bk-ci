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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.AtomMarketInitPipelineResp
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 初始化流水线进行打包归档
 * since: 2019-01-08
 */
@Service
class AtomMarketInitPipelineService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val buildService: PipelineBuildService
) {
    private val logger = LoggerFactory.getLogger(AtomMarketInitPipelineService::class.java)

    /**
     * 初始化流水线进行打包归档
     */
    fun initPipeline(
        userId: String,
        projectCode: String,
        atomMarketInitPipelineReq: AtomMarketInitPipelineReq
    ): Result<AtomMarketInitPipelineResp> {
        logger.info("initPipeline userId is:$userId,projectCode is:$projectCode,atomMarketInitPipelineReq is:$atomMarketInitPipelineReq")
        val model = JsonUtil.to(atomMarketInitPipelineReq.pipelineModel, Model::class.java)
        logger.info("model is:$model")
        // 保存流水线信息
        val pipelineId = pipelineService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        val atomBaseInfo = atomMarketInitPipelineReq.atomBaseInfo
        startParams["atomCode"] = atomBaseInfo.atomCode
        startParams["version"] = atomBaseInfo.version
        startParams["language"] = atomBaseInfo.language
        startParams["script"] = atomMarketInitPipelineReq.script
        startParams["commitId"] = atomBaseInfo.commitId
        var atomBuildStatus = AtomStatusEnum.BUILDING
        var buildId: String? = null
        try {
            buildId = buildService.buildManualStartup(
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
            logger.info("atomMarketBuildManualStartup result is:$buildId")
        } catch (e: Exception) {
            logger.info("buildManualStartup error is :$e", e)
            atomBuildStatus = AtomStatusEnum.BUILD_FAIL
        }
        return Result(AtomMarketInitPipelineResp(pipelineId, buildId, atomBuildStatus))
    }
}
