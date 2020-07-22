package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.service.ICustomPipelineService
import com.tencent.bk.codecc.task.service.TaskRegisterService
import com.tencent.bk.codecc.task.service.TaskService
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.bk.codecc.task.vo.CustomProjVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.service.ServicePublicScanResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("bkPipelineService")
class BKPipelineServiceImpl @Autowired constructor(
        private val client: Client,
        private val tofClientApi: TofClientApi,
        private val customProjRepository: CustomProjRepository,
        private val objectMapper: ObjectMapper,
        @Qualifier("pipelineTaskRegisterService") private val taskRegisterService: TaskRegisterService,
        private val taskService: TaskService
) : ICustomPipelineService {
    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    @Value("\${custom.projectName.svnName}")
    private val svnName: String? = null

    @Value("\${custom.projectName.svnPassword}")
    private val svnPassWord: String? = null

    @Value("\${custom.projectName.proxy}")
    private val proxy:String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(BKPipelineServiceImpl::class.java)
    }

    override fun getCustomProjEntity(triggerPipelineReq: TriggerPipelineOldReq): CustomProjEntity? {
        val customProjList = customProjRepository.findByCustomProjSourceAndUrl(
                triggerPipelineReq.triggerSource, triggerPipelineReq.gitUrl)
        return if (customProjList.isNotEmpty()) {
            customProjList[0]
        } else {
            null
        }
    }

    override fun handleWithCheckProjPipeline(
            triggerPipelineReq: TriggerPipelineOldReq,
            userId: String
    ): CustomProjEntity {
        var newCustomProjEntity = CustomProjEntity()
        newCustomProjEntity.url = triggerPipelineReq.gitUrl
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
        taskDetailVO.gongfengFlag = false
        taskDetailVO.customProjInfo = newCustomProjVO
        taskDetailVO.atomCode = "CodeccCheckAtom"
        taskDetailVO.codeLang = ComConstants.CodeLang.PYTHON.langValue()


        taskDetailVO.devopsTools = objectMapper.writeValueAsString(listOf("PYLINT"))
        val toolConfigParamJsonVO = ToolConfigParamJsonVO()
        toolConfigParamJsonVO.toolName = "PYLINT"
        toolConfigParamJsonVO.varName = "py_version"
        toolConfigParamJsonVO.chooseValue = "py2"
        taskDetailVO.devopsToolParams = listOf(toolConfigParamJsonVO)
        val checkerSetVO = CheckerSetVO()
        checkerSetVO.checkerSetId = "blueking_saas_rules"
        checkerSetVO.toolList = setOf("COVERITY", "PYLINT", "SENSITIVE", "HORUSPY", "CCN", "DUPC")
        taskDetailVO.checkerSetList = listOf(checkerSetVO)
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

    /**
     * 为蓝鲸创建个性化流水线
     */
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

        val containerFirst = TriggerContainer(
                id = null,
                name = "demo",
                elements = arrayListOf(manualTriggerElement),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                params = emptyList(),
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
        val originalScript = "# 您可以通过setEnv函数设置插件间传递的参数\n# setEnv \"FILENAME\" \"package.zip\"\n# 然后在后续的插件的表单中使用\${FILENAME}引用这个变量\n\n# 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" \$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n\n# cd \${WORKSPACE} 可进入当前工作空间目录\n\ncd \${WORKSPACE}\n\ncd \${WORKSPACE}\n\nexport http_proxy='$proxy' \nexport https_proxy='$proxy'\n\nsvn co ${triggerPipelineReq.gitUrl} --username $svnName --password $svnPassWord"

        val codeCloneElement = LinuxScriptElement(
                id = null,
                status = null,
                scriptType = BuildScriptType.SHELL,
                script = if (triggerPipelineReq.gitUrl!!.startsWith("https")) {
                    originalScript.plus(" << EOF\np\nyes\nEOF")
                } else {
                    originalScript
                },
                continueNoneZero = false,
                enableArchiveFile = false,
                archiveFile = ""
        )

        val codeCheckElement = MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtom",
                data = mapOf(
                        "input" to mapOf(
                                "languages" to listOf("PYTHON"),
                                "tools" to listOf("PYLINT"),
                                "asynchronous" to "false",
                                "pyVersion" to "py2",
                                "languageRuleSetMap" to mapOf("PYTHON_RULE" to listOf("standard_python")),
                                "PYTHON_RULE" to listOf("standard_python")
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

        logger.info(
                "assemble pipeline parameter successfully! task id: $taskId, project id: $projectId"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
                name = "CUSTOMPIPELINE_$taskId",
                desc = "个性化工蜂集群流水线$taskId",
                stages = arrayListOf(stageFirst, stageSecond),
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
        logger.info("start to create blue king public scan project")
        val result = client.getDevopsService(ServicePublicScanResource::class.java).createCodeCCScanProject(
                userId, projectCreateInfo
        )
        if (result.isNotOk() || null == result.data) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data!!.projectId
    }

    override fun getParamMap(customProjEntity: CustomProjEntity): MutableMap<String, String> {
        return mutableMapOf()
    }

    override fun updateCustomizedCheckProjPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        taskId: Long,
        userId: String?,
        projectId: String,
        pipelineId: String
    ) {
    }
}