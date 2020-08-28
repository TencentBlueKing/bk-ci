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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketCheckImageElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.CheckImageInitPipelineResp
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CheckImageInitPipelineService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val pipelineSettingService: PipelineSettingService,
    private val buildService: PipelineBuildService
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
        logger.info("initCheckImagePipeline userId is: $userId,projectCode is: $projectCode,checkImageInitPipelineReq is: $checkImageInitPipelineReq")
        var containerSeqId = 0
        val imageCode = checkImageInitPipelineReq.imageCode
        val imageName = checkImageInitPipelineReq.imageName
        val version = checkImageInitPipelineReq.version
        val imageType = checkImageInitPipelineReq.imageType
        val registryUser = checkImageInitPipelineReq.registryUser
        val registryPwd = checkImageInitPipelineReq.registryPwd
        // stage-1
        val stageFirstElement = ManualTriggerElement(id = "T-1-1-1")
        val stageFirstElements = listOf<Element>(stageFirstElement)
        val params = mutableListOf<BuildFormProperty>()
        params.add(BuildFormProperty(
            id = "imageCode",
            required = true,
            type = BuildFormPropertyType.STRING,
            defaultValue = imageCode,
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        params.add(BuildFormProperty(
            id = "imageName",
            required = true,
            type = BuildFormPropertyType.STRING,
            defaultValue = imageName,
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        params.add(BuildFormProperty(
            id = "version",
            required = true,
            type = BuildFormPropertyType.STRING,
            defaultValue = version,
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        params.add(BuildFormProperty(
            id = "imageType",
            required = false,
            type = BuildFormPropertyType.STRING,
            defaultValue = "",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        params.add(BuildFormProperty(
            id = "registryUser",
            required = false,
            type = BuildFormPropertyType.STRING,
            defaultValue = "",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        params.add(BuildFormProperty(
            id = "registryPwd",
            required = false,
            type = BuildFormPropertyType.STRING,
            defaultValue = "",
            options = null,
            desc = null,
            repoHashId = null,
            relativePath = null,
            scmType = null,
            containerType = null,
            glob = null,
            properties = null
        ))
        val stageFirstContainer = TriggerContainer(
            id = containerSeqId.toString(),
            name = "构建触发",
            elements = stageFirstElements,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = params,
            buildNo = null
        )
        containerSeqId++
        val stageFirstContainers = listOf<Container>(stageFirstContainer)
        val stageFirst = Stage(stageFirstContainers, VMUtils.genStageId(1))
        // stage-2
        val stageSecondCheckImageElement = MarketCheckImageElement(
            id = "T-2-1-1",
            registryUser = checkImageInitPipelineReq.registryUser,
            registryPwd = checkImageInitPipelineReq.registryPwd
        )
        val stageSecondElements = listOf(stageSecondCheckImageElement)
        val stageSecondContainer = VMBuildContainer(
            id = containerSeqId.toString(),
            elements = stageSecondElements,
            baseOS = VMBaseOS.LINUX,
            vmNames = emptySet(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 480,
            buildEnv = mapOf(),
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = DockerDispatchType(DockerVersion.TLINUX2_2.value)
        )
        val stageSecondContainers = listOf<Container>(stageSecondContainer)
        val stageSecond = Stage(stageSecondContainers, VMUtils.genStageId(2))
        val stages = mutableListOf(stageFirst, stageSecond)
        var pipelineName = "im-$projectCode-$imageCode-${System.currentTimeMillis()}"
        if (pipelineName.toCharArray().size > 64) {
            pipelineName = "im-" + projectCode.substring(0, Integer.min(9, projectCode.length)) + "-" + UUIDUtil.generate()
        }
        val model = Model(pipelineName, pipelineName, stages)
        logger.info("model is:$model")
        // 保存流水线信息
        val pipelineId = pipelineService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        if (false == checkImageInitPipelineReq.sendNotify) {
            // 不发送通知
            val settingRecord = pipelineSettingService.getSetting(pipelineId)!!
            val setting = PipelineSetting(
                projectId = projectCode,
                pipelineId = settingRecord.pipelineId,
                pipelineName = pipelineName,
                desc = settingRecord.desc,
                runLockType = PipelineRunLockType.valueOf(settingRecord.runLockType),
                successSubscription = Subscription(),
                failSubscription = Subscription(),
                labels = emptyList(),
                waitQueueTimeMinute = DateTimeUtil.secondToMinute(settingRecord.waitQueueTimeSecond),
                maxQueueSize = settingRecord.maxQueueSize,
                maxPipelineResNum = settingRecord.maxPipelineResNum,
                maxConRunningQueueSize = settingRecord.maxConRunningQueueSize
            )
            pipelineService.saveSetting(
                userId = userId,
                projectId = projectCode,
                pipelineId = pipelineId,
                setting = setting,
                channelCode = ChannelCode.AM
            )
        }
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams["imageCode"] = imageCode
        startParams["imageName"] = imageName
        startParams["version"] = version
        if (null != imageType)
            startParams["imageType"] = imageType
        if (null != registryUser)
            startParams["registryUser"] = registryUser
        if (null != registryPwd)
            startParams["registryPwd"] = registryPwd
        var imageCheckStatus = ImageStatusEnum.CHECKING
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
            logger.info("buildManualStartup result is:$buildId")
        } catch (e: Exception) {
            logger.info("buildManualStartup error is :$e", e)
            imageCheckStatus = ImageStatusEnum.CHECK_FAIL
        }
        return Result(CheckImageInitPipelineResp(pipelineId, buildId, imageCheckStatus))
    }
}
