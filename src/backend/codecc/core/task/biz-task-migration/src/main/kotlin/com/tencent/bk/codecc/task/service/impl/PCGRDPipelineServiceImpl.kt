package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.service.ICustomPipelineService
import com.tencent.bk.codecc.task.service.TaskRegisterService
import com.tencent.bk.codecc.task.service.TaskService
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.bk.codecc.task.vo.CustomProjVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.service.ServicePublicScanResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("pcgrdPipelineService")
class PCGRDPipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val tofClientApi: TofClientApi,
    private val customProjRepository: CustomProjRepository,
    private val objectMapper: ObjectMapper,
    @Qualifier("pipelineTaskRegisterService") private val taskRegisterService: TaskRegisterService,
    private val taskService : TaskService
) : ICustomPipelineService {

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    @Value("\${custom.projectName.pcgName:#{null}}")
    private val pcgName : String? = null

    @Value("\${custom.projectName.pcgPassword:#{null}}")
    private val pcgPassword : String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(PCGRDPipelineServiceImpl::class.java)
    }


    override fun getCustomProjEntity(triggerPipelineReq: TriggerPipelineOldReq) : CustomProjEntity?{
        return customProjRepository.findByCustomProjSourceAndUrlAndBranch(triggerPipelineReq.triggerSource, triggerPipelineReq.gitUrl,
            triggerPipelineReq.branch)
    }

    override fun handleWithCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        userId: String
    ) : CustomProjEntity{
        if(triggerPipelineReq.gitUrl.isNullOrBlank()){
            logger.error("git url or branch is emtpy!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("branch"))
        }
        var newCustomProjEntity = CustomProjEntity()
        newCustomProjEntity.url = triggerPipelineReq.gitUrl
        newCustomProjEntity.branch = triggerPipelineReq.branch
        newCustomProjEntity.defectDisplay = triggerPipelineReq.defectDisplay
        newCustomProjEntity.customProjSource = triggerPipelineReq.triggerSource
        newCustomProjEntity = customProjRepository.save(newCustomProjEntity)
        val newCustomProjVO = CustomProjVO()
        BeanUtils.copyProperties(newCustomProjEntity, newCustomProjVO)

        val projectId = createCustomDevopsProject(newCustomProjEntity, userId)

        //获取员工组织信息
        val staffInfo = tofClientApi.getStaffInfoByUserName(userId)
        val organizationInfo = tofClientApi.getOrganizationInfoByGroupId(staffInfo.data?.GroupId ?: -1)

        //注册任务
        val taskDetailVO = TaskDetailVO()

        taskDetailVO.projectId = projectId
        taskDetailVO.nameCn = triggerPipelineReq.gitUrl
        taskDetailVO.pipelineId = ""
        taskDetailVO.pipelineName = "CUSTOM_${newCustomProjEntity.entityId}"
        taskDetailVO.gongfengFlag = true
        taskDetailVO.customProjInfo = newCustomProjVO


        taskDetailVO.devopsTools = objectMapper.writeValueAsString(listOf("CLOC"))
        taskDetailVO.bgId = organizationInfo?.bgId ?: -1
        taskDetailVO.deptId = organizationInfo?.deptId ?: -1
        taskDetailVO.centerId = organizationInfo?.centerId ?: -1
        taskDetailVO.groupId = staffInfo.data?.GroupId ?: -1

        //按流水线注册形式注册
        val taskResult = taskRegisterService.registerTask(taskDetailVO, userId)
        val taskId = taskResult.taskId
        val taskEnName = taskResult.nameEn
        logger.info("register custom task successfully! task id: $taskId")
        //创建蓝盾流水线
        val pipelineId = createCustomizedCheckProjPipeline(triggerPipelineReq, taskId, userId, projectId)
        logger.info("create custom pipeline successfully! pipeline id: $pipelineId")
        //更新流水线信息
        val taskInfo = taskService.getTaskById(taskId)
        taskInfo.pipelineId = pipelineId
        taskService.saveTaskInfo(taskInfo)
        //todo 发送数据到数据平台

        newCustomProjEntity.taskId = taskId
        newCustomProjEntity.pipelineId = pipelineId
        newCustomProjEntity.projectId = projectId
        return customProjRepository.save(newCustomProjEntity)
    }

    override fun createCustomizedCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        taskId: Long,
        userId: String?,
        projectId: String
    ): String {
        /**
         * 第一个stage的内容
         */
        val manualTriggerElement = ManualTriggerElement(
            name = "手动触发",
            id = null,
            status = null,
            canElementSkip = false,
            useLatestParameters = false
        )

        val paramList = listOf(
            BuildFormProperty(
                id = "taskId",
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "任务id",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "firstTrigger",
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "是否首次触发",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
        )

        val containerFirst = TriggerContainer(
            id = null,
            name = "demo",
            elements = arrayListOf(manualTriggerElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = paramList,
            templateParams = null,
            buildNo = null,
            canRetry = null,
            containerId = null
        )

        val stageFirst = Stage(
            containers = arrayListOf(containerFirst),
            id = null
        )

        /**
         * 第二个stage
         */
        val codeCloneElement: Element = MarketBuildAtomElement(
            name = "拉取代码",
            atomCode = "gitCodeRepoCommon",
            version = "2.*",
            data = mapOf(
                "input" to
                    mapOf(
                        "username" to pcgName,
                        "password" to pcgPassword,
                        "refName" to triggerPipelineReq.branch,
                        "commitId" to "",
                        "enableAutoCrlf" to false,
                        "enableGitClean" to true,
                        "enableSubmodule" to false,
                        "enableSubmoduleRemote" to false,
                        "enableVirtualMergeBranch" to false,
                        "excludePath" to "",
                        "fetchDepth" to "",
                        "includePath" to "",
                        "localPath" to "",
                        "paramMode" to "SIMPLE",
                        "pullType" to "BRANCH",
                        "repositoryUrl" to triggerPipelineReq.gitUrl,
                        "strategy" to "REVERT_UPDATE",
                        "tagName" to ""/*,
                        "noScmVariable" to true*/
                    ),
                "output" to mapOf()
            )
        )

        val codeCheckElement = MarketBuildAtomElement(
            name = "CodeCC代码检查(New)",
            atomCode = "CodeccCheckAtom",
            data = mapOf(
                "input" to mapOf(
                    "languages" to listOf("OTHERS"),
                    "tools" to listOf("CLOC", "SENSITIVE"),
                    "asynchronous" to "false",
                    "languageRuleSetMap" to mapOf(
                        "OTHERS_RULE" to listOf("standard_cloc")
                    ),
                    "OTHERS_RULE" to listOf("standard_cloc")
                ),
                "output" to mapOf()
            )
        )


        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = listOf(codeCloneElement, codeCheckElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.valueOf("LINUX"),
            vmNames = emptySet(),
            maxQueueMinutes = null,
            maxRunningMinutes = 80,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = imageName,
            dispatchType = CodeCCDispatchType(
                    codeccTaskId = taskId
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 900,
                runCondition = JobRunCondition.STAGE_RUNNING,
                customVariables = null,
                customCondition = null
            ),
            mutexGroup = null,
            tstackAgentId = null
        )
        val stageSecond = Stage(
            containers = arrayListOf(containerSecond),
            id = null
        )

        val callbackElement = MarketBuildAtomElement(
            name = "代码扫描回调触发",
            atomCode = "CodeccCallbackPlugin",
            data = mapOf(
                "input" to mapOf(
                    "triggerSource" to "PCG_RD",
                    "taskId" to "\${taskId}",
                    "firstTrigger" to "\${firstTrigger}"

                ),
                "output" to mapOf()
            )
        )

        val containerThird = NormalContainer(
            id = null,
            name = "demo1",
            elements = listOf(callbackElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            enableSkip = true,
            conditions = null
        )

        val stageThird = Stage(
            containers = arrayListOf(containerThird),
            id = null
        )

        logger.info(
            "assemble pipeline parameter successfully! task id: $taskId, project id: $projectId"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
            name = "CUSTOMPIPELINE_$taskId",
            desc = "个性化工蜂集群流水线$taskId",
            stages = arrayListOf(stageFirst, stageSecond, stageThird),
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).create(
            userId
                ?: "codecc-admin", projectId, pipelineModel, ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data?.id
            ?: "" else throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
    }

    override fun createCustomDevopsProject(customProjEntity: CustomProjEntity, userId: String): String {
        val projectCreateInfo = ProjectCreateInfo(
            projectName = "CUSTOM_${customProjEntity.customProjSource}",
            englishName = "CUSTOMPROJ_${customProjEntity.customProjSource}",
            projectType = 5,
            description = "custom scan of gongfeng project/url: ${customProjEntity.url}",
            bgId = 0L,
            bgName = "",
            deptId = 0L,
            deptName = "",
            centerId = 0L,
            centerName = "",
            secrecy = false,
            kind = 0
        )
        logger.info("start to create pcg public scan project")
        val result = client.getDevopsService(ServicePublicScanResource::class.java).createCodeCCScanProject(
            userId, projectCreateInfo
        )
        if (result.isNotOk() || null == result.data) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data!!.projectId
    }


    override fun getParamMap(customProjEntity: CustomProjEntity): MutableMap<String, String> {
        return mutableMapOf("taskId" to customProjEntity.taskId.toString())
    }

    override fun updateCustomizedCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        taskId: Long,
        userId: String?,
        projectId: String,
        pipelineId : String
    ) {
        /**
         * 第一个stage的内容
         */
        val manualTriggerElement = ManualTriggerElement(
            name = "手动触发",
            id = null,
            status = null,
            canElementSkip = false,
            useLatestParameters = false
        )

        val paramList = listOf(
            BuildFormProperty(
                id = "taskId",
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "任务id",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "firstTrigger",
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "是否首次触发",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
        )

        val containerFirst = TriggerContainer(
            id = null,
            name = "demo",
            elements = arrayListOf(manualTriggerElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = paramList,
            templateParams = null,
            buildNo = null,
            canRetry = null,
            containerId = null
        )

        val stageFirst = Stage(
            containers = arrayListOf(containerFirst),
            id = null
        )

        /**
         * 第二个stage
         */
        val codeCloneElement: Element = MarketBuildAtomElement(
            name = "拉取代码",
            atomCode = "gitCodeRepoCommon",
            version = "2.*",
            data = mapOf(
                "input" to
                    mapOf(
                        "username" to pcgName,
                        "password" to pcgPassword,
                        "refName" to triggerPipelineReq.branch,
                        "commitId" to "",
                        "enableAutoCrlf" to false,
                        "enableGitClean" to true,
                        "enableSubmodule" to false,
                        "enableSubmoduleRemote" to false,
                        "enableVirtualMergeBranch" to false,
                        "excludePath" to "",
                        "fetchDepth" to "",
                        "includePath" to "",
                        "localPath" to "",
                        "paramMode" to "SIMPLE",
                        "pullType" to "BRANCH",
                        "repositoryUrl" to triggerPipelineReq.gitUrl,
                        "strategy" to "REVERT_UPDATE",
                        "tagName" to ""/*,
                        "noScmVariable" to true*/
                    ),
                "output" to mapOf()
            )
        )

        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtomDebug",
                version = "4.*",
                data = mapOf(
                    "input" to mapOf(
                        "languages" to listOf<String>(),
                        "tools" to listOf("CLOC"),
                        "asynchronous" to "false",
                        "openScanPrj" to true
                    ),
                    "output" to mapOf()
                )
            )


        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = listOf(codeCloneElement, codeccElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.valueOf("LINUX"),
            vmNames = emptySet(),
            maxQueueMinutes = null,
            maxRunningMinutes = 80,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = imageName,
            dispatchType = CodeCCDispatchType(
                codeccTaskId = ComConstants.CodeCCDispatchRoute.OPENSOURCE.flag()
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 900,
                runCondition = JobRunCondition.STAGE_RUNNING,
                customVariables = null,
                customCondition = null
            ),
            mutexGroup = null,
            tstackAgentId = null
        )
        val stageSecond = Stage(
            containers = arrayListOf(containerSecond),
            id = null
        )

        val callbackElement = MarketBuildAtomElement(
            name = "代码扫描回调触发",
            atomCode = "CodeccCallbackPlugin",
            data = mapOf(
                "input" to mapOf(
                    "triggerSource" to "PCG_RD",
                    "taskId" to "\${taskId}",
                    "firstTrigger" to "\${firstTrigger}"

                ),
                "output" to mapOf()
            )
        )

        val containerThird = NormalContainer(
            id = null,
            name = "demo1",
            elements = listOf(callbackElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            enableSkip = true,
            conditions = null
        )

        val stageThird = Stage(
            containers = arrayListOf(containerThird),
            id = null
        )

        logger.info(
            "assemble pipeline parameter successfully! task id: $taskId, project id: $projectId"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
            name = "CUSTOMPIPELINE_$taskId",
            desc = "个性化工蜂集群流水线$taskId",
            stages = arrayListOf(stageFirst, stageSecond, stageThird),
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).edit(
            userId
                ?: "codecc-admin", projectId, pipelineId, pipelineModel, ChannelCode.GONGFENGSCAN
        )
    }
}