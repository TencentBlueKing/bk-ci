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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.KEY_BRANCH
import com.tencent.devops.common.api.constant.KEY_COMMIT_ID
import com.tencent.devops.common.api.constant.KEY_INVALID_OS_INFO
import com.tencent.devops.common.api.constant.KEY_OS_ARCH
import com.tencent.devops.common.api.constant.KEY_OS_NAME
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VALID_OS_ARCH_FLAG
import com.tencent.devops.common.api.constant.KEY_VALID_OS_NAME_FLAG
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.process.pojo.AtomMarketInitPipelineResp
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_RUNTIME_VERSION
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 初始化流水线进行打包归档
 * since: 2019-01-08
 */
@Suppress("ALL")
@Service
class AtomMarketInitPipelineService @Autowired constructor(
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
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
        val model = JsonUtil.to(atomMarketInitPipelineReq.pipelineModel, Model::class.java)
        // 保存流水线信息
        val pipelineId = pipelineInfoFacadeService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        val atomBaseInfo = atomMarketInitPipelineReq.atomBaseInfo
        startParams[KEY_ATOM_CODE] = atomBaseInfo.atomCode
        startParams[KEY_VERSION] = atomBaseInfo.version
        val language = atomBaseInfo.language
        if (!language.isNullOrBlank()) {
            startParams[KEY_LANGUAGE] = language
        }
        startParams[KEY_SCRIPT] = atomMarketInitPipelineReq.script
        val branch = atomBaseInfo.branch
        if (!branch.isNullOrBlank()) {
            startParams[KEY_BRANCH] = branch
        }
        val commitId = atomBaseInfo.commitId
        if (!commitId.isNullOrBlank()) {
            startParams[KEY_COMMIT_ID] = commitId
        }
        val osName = atomBaseInfo.osName
        if (!osName.isNullOrBlank()) {
            startParams[KEY_OS_NAME] = osName
        }
        val osArch = atomBaseInfo.osArch
        if (!osArch.isNullOrBlank()) {
            startParams[KEY_OS_ARCH] = osArch
        }
        val invalidOsInfo = atomBaseInfo.invalidOsInfo
        if (!invalidOsInfo.isNullOrBlank()) {
            startParams[KEY_INVALID_OS_INFO] = invalidOsInfo
        }
        val runtimeVersion = atomBaseInfo.runtimeVersion
        if (!runtimeVersion.isNullOrBlank()) {
            startParams[KEY_RUNTIME_VERSION] = runtimeVersion
        }
        atomMarketInitPipelineReq.validOsNameFlag?.let {
            startParams[KEY_VALID_OS_NAME_FLAG] = it.toString()
        }
        atomMarketInitPipelineReq.validOsArchFlag?.let {
            startParams[KEY_VALID_OS_ARCH_FLAG] = it.toString()
        }
        var atomBuildStatus = AtomStatusEnum.BUILDING
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
            logger.info("atomMarketBuildManualStartup result is:$buildId")
        } catch (t: Throwable) {
            logger.error("$pipelineId buildManualStartup error:", t)
            atomBuildStatus = AtomStatusEnum.BUILD_FAIL
        }
        return Result(AtomMarketInitPipelineResp(pipelineId, buildId, atomBuildStatus))
    }
}
