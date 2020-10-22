package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.task.component.GongfengProjectChecker
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.BuildIdRelationshipRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.CustomProjRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengStatProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongodbtemplate.CustomProjDao
import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity
import com.tencent.bk.codecc.task.pojo.CodeCCAccountAuthInfo
import com.tencent.bk.codecc.task.pojo.CodeCCPipelineReq
import com.tencent.bk.codecc.task.pojo.CodeCCTokenAuthInfo
import com.tencent.bk.codecc.task.pojo.CustomTriggerPipelineModel
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.util.UUIDUtil
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
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.web.mq.EXCHANGE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.common.web.mq.ROUTE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.project.api.service.service.ServicePublicScanResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class GongfengTriggerServiceImpl @Autowired constructor(
    private val client: Client,
    private val customProjDao: CustomProjDao,
    private val customProjRepository: CustomProjRepository,
    private val taskRepository: TaskRepository,
    private val baseRepository: BaseDataRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val gongfengStatProjRepository: GongfengStatProjRepository,
    private val buildIdRelationshipRepository: BuildIdRelationshipRepository,
    private val gongfengProjectChecker: GongfengProjectChecker
) : GongfengTriggerService {

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengTriggerServiceImpl::class.java)
    }

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    private val appcodeMappingCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String>(
            object : CacheLoader<String, String>() {
                override fun load(appCode: String): String {
                    return try {
                        getBaseDataInfo(ComConstants.KEY_APP_CODE_MAPPING, appCode) ?: appCode
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get appCodeMapping.")
                        appCode
                    }
                }
            }
        )

    private val pluginVersionCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String?>(
            object : CacheLoader<String, String?>() {
                override fun load(versionType: String): String? {
                    return try {
                        logger.info("begin to get plugin version: $versionType")
                        getOpenSourceVersion(versionType)
                    } catch (t: Throwable) {
                        logger.info("get plugin version fail! version type: $versionType, message: ${t.message}")
                        null
                    }
                }
            }
        )

    private val dispatchRouteCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String>(
            object : CacheLoader<String, String>() {
                override fun load(scanType: String): String? {
                    return try {
                        getBaseDataInfo(ComConstants.KEY_OPENSOURCE_ROUTE, scanType)
                    } catch (t: Throwable) {
                        logger.info("scanType[$scanType] failed to get dispatch route.")
                        null
                    }
                }
            }
        )

    private val frequencyCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String>(
            object : CacheLoader<String, String>() {
                override fun load(triggerType: String): String? {
                    return try {
                        getBaseDataInfo(ComConstants.KEY_OPENSOURCE_FREQUENCY, triggerType)
                    } catch (t: Throwable) {
                        logger.info("triggerType[$triggerType] failed to get opensource frequency.")
                        null
                    }
                }
            }
        )

    override fun triggerCustomProjectPipeline(
        triggerPipelineReq: TriggerPipelineReq,
        appCode: String,
        userId: String
    ): TriggerPipelineRsp {
        logger.info("start to trigger custom project pipeline. {}", triggerPipelineReq)
        if (triggerPipelineReq.gitUrl.isNullOrBlank()) {
            logger.error("git url or branch is emtpy!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("url"))
        }
        //校验工蜂id的一致性
        if (null != triggerPipelineReq.gongfengProjectId) {
            if (null == triggerPipelineReq.codeCCPipelineReq.codeccPreTreatment?.gongfengProjectId ||
                triggerPipelineReq.codeCCPipelineReq.codeccPreTreatment?.gongfengProjectId != triggerPipelineReq.gongfengProjectId
            ) {
                logger.info("outside gongfeng project id should be consistent with inside gongfeng project!")
                throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("gongfengProjectId"))
            }
        }
        //取出相应枚举，及对应的bean
        /*val customProjSource = enumValueByBaseDataComponent.getEnumValueByName(
            appCode.toUpperCase(),
            CustomProjSource::class.java
        )
        if (null == customProjSource) {
            logger.error("no enum value found by name $appCode")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }*/
        val customProjEntity = customProjDao.findByGongfengIdAndUrlAndBranch(
            appcodeMappingCache.get(appCode),
            triggerPipelineReq.gongfengProjectId,
            triggerPipelineReq.gitUrl,
            triggerPipelineReq.branch,
            triggerPipelineReq.logicRepo
        )
        val codeccBuildId = UUIDUtil.generate()
        //如果为空，则需要重新新建项目和流水线
        if (null == customProjEntity) {

            checkGongfengProject(triggerPipelineReq, customProjEntity)

            val newCustomProjEntity = handleWithCheckProjPipeline(appCode, triggerPipelineReq, userId)
            return with(newCustomProjEntity) {
                val paramMap = assembleRuntimeParam("true", codeccBuildId, triggerPipelineReq)
                val customTriggerPipelineModel = CustomTriggerPipelineModel(
                    customProjEntity = this,
                    runtimeParam = paramMap,
                    userId = userId
                )
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CUSTOM_PIPELINE_TRIGGER,
                    ROUTE_CUSTOM_PIPELINE_TRIGGER,
                    customTriggerPipelineModel
                )
                assembleReturnModel(this, "true", codeccBuildId)
            }
        } else {
            //校验本次上报的id和url映射和原有映射是否一致
            if ((triggerPipelineReq.gongfengProjectId ?: 0 != customProjEntity.gongfengProjectId ?: 0) ||
                (triggerPipelineReq.gitUrl != customProjEntity.url)
            ) {
                logger.info("different id-url mapping since last trigger")
                throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("id--url mapping"))
            }

            checkGongfengProject(triggerPipelineReq, customProjEntity)

            //为了兼容原版，如果没有更新成新的modeljson，则进行更新
            //流水线切换为devcloud路由，原有的触发也要切换
            val routeConfig = dispatchRouteCache.get("Trigger")
            val appCodes = if (!routeConfig.isNullOrBlank()) {
                JsonUtil.to(routeConfig, object : TypeReference<List<String>>() {})
            } else {
                emptyList()
            }
            updateCustomizedCheckProjPipeline(
                triggerPipelineReq.codeCCPipelineReq,
                customProjEntity,
                if (appCodes.contains(appCode)) ComConstants.CodeCCDispatchRoute.INDEPENDENT else ComConstants.CodeCCDispatchRoute.DEVCLOUD,
                userId
            )
            customProjEntity.commonModelJson = true
            customProjRepository.save(customProjEntity)
            return with(customProjEntity) {
                val paramMap = assembleRuntimeParam("false", codeccBuildId, triggerPipelineReq)
                val customTriggerPipelineModel = CustomTriggerPipelineModel(
                    customProjEntity = this,
                    runtimeParam = paramMap,
                    userId = userId
                )
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CUSTOM_PIPELINE_TRIGGER,
                    ROUTE_CUSTOM_PIPELINE_TRIGGER,
                    customTriggerPipelineModel
                )
                assembleReturnModel(this, "false", codeccBuildId)
            }
        }
    }

    private fun checkGongfengProject(triggerPipelineReq: TriggerPipelineReq, customProjEntity: CustomProjEntity?) {
        val checkResult =
            if (triggerPipelineReq.checkGongfengProject != null && triggerPipelineReq.checkGongfengProject!!) {
                logger.info("start to check gongfeng project, trigger req: $triggerPipelineReq")
                val taskInfoEntity = if (null == customProjEntity) {
                    null
                } else {
                    taskRepository.findByPipelineId(customProjEntity.pipelineId)
                }
                val gongfengProjectId = if (null != triggerPipelineReq.gongfengProjectId) {
                    triggerPipelineReq.gongfengProjectId
                } else {
                    triggerPipelineReq.codeCCPipelineReq.codeccPreTreatment?.gongfengProjectId
                }

                if (null == taskInfoEntity && null == gongfengProjectId) {
                    throw CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, arrayOf("gongfengProjectId"))
                }

                val gongfengPublicProjEntity: GongfengPublicProjEntity?
                val gongfengStatProjEntity: GongfengStatProjEntity?
                if (null != taskInfoEntity) {
                    gongfengPublicProjEntity =
                        gongfengPublicProjService.findProjectById(taskInfoEntity.gongfengProjectId)
                    gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(taskInfoEntity.gongfengProjectId)
                } else {
                    gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(gongfengProjectId)
                    gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(gongfengProjectId)
                }

                gongfengProjectChecker.check(
                    TaskConstants.GongfengCheckType.CUSTOM_TRIGGER_TASK,
                    gongfengPublicProjEntity,
                    gongfengStatProjEntity,
                    taskInfoEntity
                )
            } else {
                0
            }

        if (null != checkResult && checkResult != 0) {
            logger.info(
                "gongfeng project check failed, reason: {}, gongfeng project url: {}",
                checkResult,
                triggerPipelineReq.gitUrl
            )
            throw CodeCCException(TaskMessageCode.CHECK_GONGFENG_PROJ_FAIL, arrayOf(checkResult.toString()))
        }
    }

    /**
     * 手动触发个性化流水线
     */
    override fun manualStartupCustomPipeline(customTriggerPipelineModel: CustomTriggerPipelineModel) {
        try {
            val apiFreq = frequencyCache.get("api")
            logger.info("trigger interval period is $apiFreq")
            Thread.sleep(apiFreq.toLong())
            with(customTriggerPipelineModel) {
                val buildResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    userId, customProjEntity.projectId, customProjEntity.pipelineId,
                    runtimeParam, ChannelCode.GONGFENGSCAN
                )
                if (buildResult.isNotOk() || null == buildResult.data) {
                    logger.info("trigger pipeline fail! project id: ${customProjEntity.projectId}, url: ${customProjEntity.url}")
                }
                logger.info("trigger pipeline successfully! pipeline id: ${customProjEntity.pipelineId}, build id: ${buildResult.data!!.id}")
                val taskId = if (customProjEntity.taskId != 0L) {
                    customTriggerPipelineModel.customProjEntity.taskId
                } else {
                    val customProject = customProjRepository.findFirstByPipelineId(customProjEntity.pipelineId)
                    customProject.taskId
                }
                val buildIdRelationshipEntity = BuildIdRelationshipEntity(
                    taskId,
                    runtimeParam["codeccBuildId"],
                    customProjEntity.pipelineId,
                    buildResult.data!!.id,
                    null,
                    ComConstants.ScanStatus.PROCESSING.code,
                    null,
                    0L,
                    null,
                    (runtimeParam["firstTrigger"]?:"false").toBoolean()
                )
                buildIdRelationshipRepository.save(buildIdRelationshipEntity)
            }
        } catch (e: Exception) {
            logger.info("trigger pipeline fail! project id: ${customTriggerPipelineModel.customProjEntity.projectId}, url: ${customTriggerPipelineModel.customProjEntity.url}")
        }
    }

    /**
     * 获取appCode和原有来源映射
     */
    private fun getBaseDataInfo(paramType: String, paramCode: String): String? {
        val baseDataEntityList =
            baseRepository.findAllByParamTypeAndParamCode(paramType, paramCode)
        return if (baseDataEntityList.isNullOrEmpty()) null else baseDataEntityList[0].paramValue
    }

    /**
     * 装配运行时参数传值
     */
    private fun assembleRuntimeParam(
        firstTrigger: String,
        codeccBuildId: String,
        triggerPipelineReq: TriggerPipelineReq
    ): Map<String, String> {
        val paramMap = mutableMapOf<String, String>()
        paramMap["firstTrigger"] = firstTrigger
        paramMap["manualTriggerPipeline"] = "true"
        paramMap["codeccBuildId"] = codeccBuildId
        if (!triggerPipelineReq.codeCCPipelineReq.runtimeParamMap.isNullOrEmpty()) {
            triggerPipelineReq.codeCCPipelineReq.runtimeParamMap!!.forEach {
                paramMap[it.paramCode] = it.paramValue
            }
        }
        return paramMap
    }

    /**
     * 装配返回模型
     */
    private fun assembleReturnModel(
        customProjEntity: CustomProjEntity,
        firstTrigger: String,
        codeccBuildId: String
    ): TriggerPipelineRsp {
        return with(customProjEntity) {
            val taskInfoEntity = taskRepository.findByPipelineId(pipelineId)
            if (null != taskInfoEntity) {
                val toolList =
                    if (taskInfoEntity.toolConfigInfoList.isNotEmpty()) taskInfoEntity.toolConfigInfoList.filter { it.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value() }
                        .map { it.toolName }
                    else
                        emptyList()
                TriggerPipelineRsp(
                    projectId = projectId,
                    taskId = taskInfoEntity.taskId,
                    pipelineId = pipelineId,
                    toolList = toolList,
                    firstTrigger = firstTrigger,
                    codeccBuildId = codeccBuildId
                )
            } else {
                TriggerPipelineRsp(
                    projectId = projectId,
                    taskId = 0L,
                    pipelineId = pipelineId,
                    toolList = listOf(),
                    firstTrigger = firstTrigger,
                    codeccBuildId = codeccBuildId
                )
            }
        }
    }

    /**
     * 新仓库处理
     */
    private fun handleWithCheckProjPipeline(
        appCode: String, triggerPipelineReq: TriggerPipelineReq,
        userId: String
    ): CustomProjEntity {
        if (triggerPipelineReq.gitUrl.isNullOrBlank()) {
            logger.error("git url or branch is emtpy!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("branch"))
        }
        var newCustomProjEntity = CustomProjEntity()
        newCustomProjEntity.url = triggerPipelineReq.gitUrl
        newCustomProjEntity.branch = triggerPipelineReq.branch
        newCustomProjEntity.gongfengProjectId = triggerPipelineReq.gongfengProjectId
        newCustomProjEntity.repositoryType = triggerPipelineReq.repoType
        newCustomProjEntity.defectDisplay = triggerPipelineReq.defectDisplay
        newCustomProjEntity.appCode = appCode
        newCustomProjEntity.customProjSource = appcodeMappingCache.get(appCode)
        newCustomProjEntity.logicRepo = triggerPipelineReq.logicRepo
        newCustomProjEntity = customProjRepository.save(newCustomProjEntity)
        logger.info("finish create custom project, app code: $appCode")
        val projectId = createCustomDevopsProject(newCustomProjEntity, userId)
        newCustomProjEntity.projectId = projectId
        logger.info("finish create devops project, app code: $appCode")
        //modified 2020-07-01 by neildwu, 流水线切换为devcloud路由，原有的触发也要切换
        val routeConfig = dispatchRouteCache.get("Trigger")
        val appCodes = if (!routeConfig.isNullOrBlank()) {
            JsonUtil.to(routeConfig, object : TypeReference<List<String>>() {})
        } else {
            emptyList()
        }
        logger.info("app codes : $appCodes, current app code : $appCode")
        val pipelineId =
            createCustomizedCheckProjPipeline(
                triggerPipelineReq.codeCCPipelineReq,
                newCustomProjEntity,
                if (appCodes.contains(appCode)) ComConstants.CodeCCDispatchRoute.INDEPENDENT else ComConstants.CodeCCDispatchRoute.DEVCLOUD,
                userId
            )
        logger.info("finish create customized check project pipeline: $appCode")
        newCustomProjEntity.pipelineId = pipelineId
        newCustomProjEntity.commonModelJson = true
        return customProjRepository.save(newCustomProjEntity)
    }

    /**
     * 创建蓝盾项目
     */
    private fun createCustomDevopsProject(customProjEntity: CustomProjEntity, userId: String): String {
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

    /**
     * 创建流水线
     */
    private fun createCustomizedCheckProjPipeline(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        dispatchRoute: ComConstants.CodeCCDispatchRoute,
        userId: String
    ): String {
        logger.info("start to create pipeline model")
        val stageList = assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute)
        /**
         * 总流水线拼装
         */
        val uuid = UUIDUtil.generate()
        val pipelineModel = Model(
            name = "CUSTOM_$uuid",
            desc = "个性化工蜂集群流水线$uuid",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )
        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).create(
            userId, customProjEntity.projectId, pipelineModel, ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data?.id
            ?: "" else throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
    }

    /**
     * 更新流水线
     */
    private fun updateCustomizedCheckProjPipeline(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        dispatchRoute: ComConstants.CodeCCDispatchRoute,
        userId: String
    ) {
        logger.info("start to update pipeline model")
        val stageList = assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute)
        val previousModel = try {
            client.getDevopsService(ServicePipelineResource::class.java)
                .get(userId, customProjEntity.projectId, customProjEntity.pipelineId, ChannelCode.GONGFENGSCAN).data
        } catch (e: Exception) {
            null
        }

        val pipelineModel = Model(
            name = "",
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        if (null != previousModel) {
            pipelineModel.name = previousModel.name
            pipelineModel.desc = previousModel.desc
        } else {
            val uuid = UUIDUtil.generate()
            pipelineModel.name = "CUSTOM_$uuid"
            pipelineModel.desc = "个性化工蜂集群流水线$uuid"
        }
        client.getDevopsService(ServicePipelineResource::class.java).edit(
            userId = userId,
            projectId = customProjEntity.projectId,
            pipelineId = customProjEntity.pipelineId,
            pipeline = pipelineModel,
            channelCode = ChannelCode.GONGFENGSCAN
        )
    }

    /**
     * 组装流水线模型
     */
    private fun assemblePipelineModel(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        dispatchRoute: ComConstants.CodeCCDispatchRoute
    ): List<Stage> {
        val stageList = mutableListOf<Stage>()
        /**
         * 第一个stage的内容
         */
        val elementFirst = ManualTriggerElement(
            name = "手动触发",
            id = null,
            status = null,
            canElementSkip = false,
            useLatestParameters = false
        )

        /**
         * 配置运行时参数
         */
        val runtimeParams: MutableList<BuildFormProperty> = mutableListOf(
            BuildFormProperty(
                id = "manualTriggerPipeline",
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "是否是手动触发流水线",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "codeccBuildId",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "codecc构建id",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ),
            BuildFormProperty(
                id = "firstTrigger",
                required = false,
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
        if (!codeCCPipelineReq.runtimeParamMap.isNullOrEmpty()) {
            codeCCPipelineReq.runtimeParamMap!!.forEach {
                runtimeParams.add(
                    BuildFormProperty(
                        id = it.paramCode,
                        required = true,
                        type = BuildFormPropertyType.STRING,
                        defaultValue = 0,
                        options = null,
                        desc = it.paramDesc,
                        repoHashId = null,
                        relativePath = null,
                        scmType = null,
                        containerType = null,
                        glob = null,
                        properties = null
                    )
                )
            }
        }

        val containerFirst = TriggerContainer(
            id = null,
            name = "demo",
            elements = arrayListOf(elementFirst),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = runtimeParams,
            templateParams = null,
            buildNo = null,
            canRetry = null,
            containerId = null
        )
        val stageFirst = Stage(
            containers = arrayListOf(containerFirst),
            id = null
        )
        stageList.add(stageFirst)

        /**
         * 第二个stage内容(主job)
         */
        val secondElementList = mutableListOf<Element>()
        /**
         * 拼装过滤插件
         */
        if (null != codeCCPipelineReq.codeccPreTreatment) {
            val codeccPreTreatment = codeCCPipelineReq.codeccPreTreatment
            val codeCCFilterElement = MarketBuildAtomElement(
                name = "动态判断是否需要扫描",
                atomCode = "CodeCCFilter",
                data = mapOf(
                    "input" to if (null == codeccPreTreatment!!.gongfengProjectId) mapOf() else mapOf(
                        "gongfengProjectId" to codeccPreTreatment.gongfengProjectId.toString()
                    ),
                    "output" to mapOf()
                )
            )
            secondElementList.add(codeCCFilterElement)
        }

        /**
         * 拼装拉取代码插件
         */
        val codeccRepoInfo = codeCCPipelineReq.repoInfo
        val gitInputMap = mutableMapOf(
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
            "pullType" to if(!codeccRepoInfo.commitId.isNullOrBlank()) "COMMIT_ID" else "BRANCH",
            "strategy" to "REVERT_UPDATE",
            "tagName" to "",
            "repositoryUrl" to customProjEntity.url
        )
        if(!codeccRepoInfo.commitId.isNullOrBlank()){
            gitInputMap["refName"] = codeccRepoInfo.commitId!!
        } else if (!customProjEntity.branch.isNullOrBlank()) {
            gitInputMap["refName"] = customProjEntity.branch
        }
        when (codeccRepoInfo) {
            is CodeCCAccountAuthInfo -> {
                gitInputMap["username"] = codeccRepoInfo.userName
                gitInputMap["password"] = codeccRepoInfo.passWord
            }
            is CodeCCTokenAuthInfo -> {
                gitInputMap["accessToken"] = codeccRepoInfo.accessToken
            }
        }
        val gitElement = MarketBuildAtomElement(
            name = "拉取代码",
            atomCode = "gitCodeRepoCommon",
            version = (pluginVersionCache.get("GIT") ?: "2.*"),
            data = mapOf(
                "input" to gitInputMap,
                "output" to mutableMapOf()
            )
        )
        gitElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = false,
            retryWhenFailed = true,
            retryCount = 3,
            timeout = 900L,
            runCondition = RunCondition.PRE_TASK_SUCCESS,
            otherTask = "",
            customVariables = listOf(),
            customCondition = ""
        )

        secondElementList.add(gitElement)

        /**
         * 拼装codecc扫描插件
         */
        val codeScanInfo = codeCCPipelineReq.codeScanInfo
        val scanInputMap = mutableMapOf(
            "languages" to listOf<String>(),
            "tools" to listOf("CLOC"),
            "asynchronous" to "false",
            "openScanPrj" to true
        )
        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtomDebug",
                version = (pluginVersionCache.get("CODECC") ?: "4.*"),
                data = mapOf(
                    "input" to if (!codeScanInfo.scanParamMap.isNullOrEmpty()) scanInputMap.plus(codeScanInfo.scanParamMap!!) else scanInputMap,
                    "output" to mutableMapOf()
                )
            )

        codeccElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = true,
            retryWhenFailed = false,
            retryCount = 1,
            timeout = 900L,
            runCondition = RunCondition.PRE_TASK_SUCCESS,
            otherTask = "",
            customVariables = listOf(),
            customCondition = ""
        )
        secondElementList.add(codeccElement)

        /**
         * 拼接devnet回调插件
         */
        if (!codeCCPipelineReq.customPostDevnetAtom.isNullOrEmpty()) {
            val customPostDevnetAtom = codeCCPipelineReq.customPostDevnetAtom
            customPostDevnetAtom!!.forEachIndexed { index, codeCCMarketAtom ->
                if (!codeCCMarketAtom.atomCode.contains("callback", true)) {
                    return@forEachIndexed
                }
                val callbackElement = MarketBuildAtomElement(
                    name = "代码扫描回调触发$index",
                    atomCode = codeCCMarketAtom.atomCode,
                    data = mapOf(
                        "input" to if (null == codeCCMarketAtom.inputs) mapOf(
                            "triggerSource" to appcodeMappingCache.get(
                                customProjEntity.appCode
                            )
                        ) else codeCCMarketAtom.inputs!!.plus(
                            mapOf(
                                "triggerSource" to appcodeMappingCache.get(
                                    customProjEntity.appCode
                                )
                            )
                        ),
                        "output" to if (null == codeCCMarketAtom.outputs) mapOf() else codeCCMarketAtom.outputs!!
                    )
                )
                if (!codeCCMarketAtom.version.isNullOrBlank()) {
                    callbackElement.version = codeCCMarketAtom.version!!
                }
                secondElementList.add(callbackElement)
            }
        }

        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = secondElementList,
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
                codeccTaskId = dispatchRoute.flag()
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 600,
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
        stageList.add(stageSecond)

        /**
         * 第三个stage内容(非必须)
         */
        if (!codeCCPipelineReq.customPostIDCAtom.isNullOrEmpty()) {
            val customPostIDCAtom = codeCCPipelineReq.customPostIDCAtom
            val thirdElementList = mutableListOf<Element>()
            customPostIDCAtom!!.forEachIndexed { index, codeCCMarketAtom ->
                if (!codeCCMarketAtom.atomCode.contains("callback", true)) {
                    return@forEachIndexed
                }
                val callbackElement = MarketBuildAtomElement(
                    name = "代码扫描回调触发$index",
                    atomCode = codeCCMarketAtom.atomCode,
                    data = mapOf(
                        "input" to if (null == codeCCMarketAtom.inputs) mapOf(
                            "triggerSource" to appcodeMappingCache.get(
                                customProjEntity.appCode
                            )
                        ) else codeCCMarketAtom.inputs!!.plus(
                            mapOf(
                                "triggerSource" to appcodeMappingCache.get(
                                    customProjEntity.appCode
                                )
                            )
                        ),
                        "output" to if (null == codeCCMarketAtom.outputs) mapOf() else codeCCMarketAtom.outputs!!
                    )
                )
                if (!codeCCMarketAtom.version.isNullOrBlank()) {
                    callbackElement.version = codeCCMarketAtom.version!!
                }
                thirdElementList.add(callbackElement)
            }

            if (!thirdElementList.isNullOrEmpty()) {
                val containerThird = NormalContainer(
                    id = null,
                    name = "demo1",
                    elements = thirdElementList,
                    status = null,
                    startEpoch = null,
                    systemElapsed = null,
                    elementElapsed = null,
                    enableSkip = false,
                    conditions = null
                )
                val stageThird = Stage(
                    containers = arrayListOf(containerThird),
                    id = null
                )
                stageList.add(stageThird)
            }
        }

        logger.info(
            "assemble pipeline parameter successfully! url : ${customProjEntity.url}, branch: ${customProjEntity.branch}"
        )
        return stageList
    }

    /**
     * 获取开源扫描插件版本号
     */
    private fun getOpenSourceVersion(versionType: String): String? {
        val baseDataList =
            baseRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_VERSION, versionType)
        return if (baseDataList.isNullOrEmpty()) null else baseDataList[0].paramValue
    }
}