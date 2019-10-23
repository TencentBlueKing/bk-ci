package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.build.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.build.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.AtomBuildArchiveElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.git.GitPullMode
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.utils.enum.CodePullStrategy
import com.tencent.devops.common.pipeline.utils.enum.GitPullModeType
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.AtomMarketInitPipelineResp
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 初始化流水线进行打包归档
 * author: carlyin
 * since: 2019-01-08
 */
@Service
class AtomMarketInitPipelineService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val buildService: PipelineBuildService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomMarketInitPipelineService::class.java)
    }

    /**
     * 初始化流水线进行打包归档
     */
    fun initPipeline(userId: String, projectCode: String, atomBaseInfo: AtomBaseInfo, repositoryHashId: String, repositoryPath: String?, script: String, buildEnv: Map<String, String>?): Result<AtomMarketInitPipelineResp> {
        var containerSeqId = 0
        // stage-1
        val stageFirstElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val stageFirstElements = listOf<Element>(stageFirstElement)
        val params = mutableListOf<BuildFormProperty>()
        params.add(BuildFormProperty("atomCode", true, BuildFormPropertyType.STRING, atomBaseInfo.atomCode, null, null, null, null, null, null, null, null))
        params.add(BuildFormProperty("version", true, BuildFormPropertyType.STRING, atomBaseInfo.version, null, null, null, null, null, null, null, null))
        params.add(BuildFormProperty("script", true, BuildFormPropertyType.STRING, script, null, null, null, null, null, null, null, null))
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
        val stageFirst = Stage(stageFirstContainers, "stage-1")
        // stage-2
        val stageSecondPullCodeElement = CodeGitElement(
            name = "拉取Git仓库代码",
            id = "T-2-1-1",
            status = null,
            repositoryHashId = repositoryHashId,
            branchName = "",
            revision = "",
            strategy = CodePullStrategy.FRESH_CHECKOUT,
            path = repositoryPath,
            enableSubmodule = true,
            gitPullMode = GitPullMode(GitPullModeType.BRANCH, "master")
        )
        val stageSecondLinuxScriptElement = LinuxScriptElement(
            name = "执行Linux脚本",
            id = "T-2-1-2",
            status = null,
            scriptType = BuildScriptType.SHELL,
            script = "\${script}",
            continueNoneZero = false
        )
        val stageSecondAtomBuildArchiveElement = AtomBuildArchiveElement("原子发布归档", "T-2-1-3")
        val stageSecondElements = listOf(stageSecondPullCodeElement, stageSecondLinuxScriptElement, stageSecondAtomBuildArchiveElement)
        val stageSecondContainer = VMBuildContainer(
            id = containerSeqId.toString(),
            name = "构建环境",
            elements = stageSecondElements,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.LINUX,
            vmNames = emptySet(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 480,
            buildEnv = buildEnv,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = DockerDispatchType("tlinux2.2")
        )
        val stageSecondContainers = listOf<Container>(stageSecondContainer)
        val stageSecond = Stage(stageSecondContainers, "stage-2")
        val stages = mutableListOf(stageFirst, stageSecond)
        val atomCode = atomBaseInfo.atomCode
        val pipelineName = "am-$projectCode-$atomCode-" + System.currentTimeMillis()
        val model = Model(pipelineName, pipelineName, stages)
        logger.info("model is:$model")
        // 保存流水线信息
        val pipelineId = pipelineService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams["atomCode"] = atomCode
        startParams["version"] = atomBaseInfo.version
        startParams["script"] = script
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
            logger.info("buildManualStartup error is :$e")
            atomBuildStatus = AtomStatusEnum.BUILD_FAIL
        }
        // val executePipelineThread = ExecutePipelineThread(client, buildService, userId, StartType.SERVICE, projectCode, atomBaseInfo, pipelineId, startParams, ChannelCode.AM, false, false, null)
        // Thread(executePipelineThread).start()
        return Result(AtomMarketInitPipelineResp(pipelineId, buildId, atomBuildStatus))
    }
}
