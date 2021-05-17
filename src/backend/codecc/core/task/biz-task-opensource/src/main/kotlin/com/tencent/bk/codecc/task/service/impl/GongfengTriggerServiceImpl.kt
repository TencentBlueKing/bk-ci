package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.task.component.GongfengProjectChecker
import com.tencent.bk.codecc.task.constant.EPC_APP_CODE
import com.tencent.bk.codecc.task.constant.OTEAM_APP_CODE
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.BuildIdRelationshipRepository
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengFailPageRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengStatProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengTriggerParamRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongotemplate.CustomProjDao
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.listener.GongfengCreateTaskListener
import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.model.CustomProjVersionEntity
import com.tencent.bk.codecc.task.model.GongFengTriggerParamEntity
import com.tencent.bk.codecc.task.model.GongfengFailPageEntity
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet
import com.tencent.bk.codecc.task.model.PipelineIdRelationshipEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.CodeCCAccountAuthInfo
import com.tencent.bk.codecc.task.pojo.CodeCCPipelineReq
import com.tencent.bk.codecc.task.pojo.CodeCCTokenAuthInfo
import com.tencent.bk.codecc.task.pojo.CustomTriggerPipelineModel
import com.tencent.bk.codecc.task.pojo.GongfengBranchModel
import com.tencent.bk.codecc.task.pojo.GongfengCommitModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.bk.codecc.task.pojo.OwnerInfo
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.bk.codecc.task.service.GongfengOteamCoverityService
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.GongfengTriggerService
import com.tencent.bk.codecc.task.service.PipelineIdRelationService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.bk.codecc.task.vo.CustomProjVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Result
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
import com.tencent.devops.common.util.OkhttpUtils
import com.tencent.devops.common.web.mq.EXCHANGE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.common.web.mq.EXCHANGE_EXTERNAL_JOB
import com.tencent.devops.common.web.mq.ROUTE_CUSTOM_PIPELINE_TRIGGER
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.pojo.ProjectCreateInfo
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDate
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
    private val gongfengProjectChecker: GongfengProjectChecker,
    private val gongfengPublicProjRepository: GongfengPublicProjRepository,
    private val objectMapper: ObjectMapper,
    private val gongfengFailPageRepository: GongfengFailPageRepository,
    private val openSourceTaskRegisterService: OpenSourceTaskRegisterServiceImpl,
    private val gongfengTriggerParamRepository: GongfengTriggerParamRepository,
    private val pipelineService: PipelineService,
    private val taskDao: TaskDao,
    private val tofClientApi: TofClientApi,
    private val baseDataRepository: BaseDataRepository,
    private val gongfengCreateTaskListener: GongfengCreateTaskListener,
    private val gongfengOteamCoverityService: GongfengOteamCoverityService,
    private val pipelineIdRelationService: PipelineIdRelationService
) : GongfengTriggerService {

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengTriggerServiceImpl::class.java)
    }

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${codecc.privatetoken:#{null}}")
    private val gitPrivateToken: String? = null

    @Value("\${codecc.classurl:#{null}}")
    private val publicClassUrl: String? = null

    /**
     * 用于appCode和来源映射缓存
     */
    private val appcodeMappingCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String>(
            object : CacheLoader<String, String>() {
                override fun load(appCode: String): String {
                    return try {
                        getBaseDataInfo(ComConstants.KEY_APP_CODE_MAPPING, appCode)
                            ?: appCode
                    } catch (t: Throwable) {
                        logger.info("appCode[$appCode] failed to get appCodeMapping.")
                        appCode
                    }
                }
            }
        )

    /**
     * 用于插件版本缓存
     */
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

    /**
     * 用于缓存开源扫描时间周期
     */
    private val periodInfoCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, Int>(
            object : CacheLoader<String, Int>() {
                override fun load(paramType: String): Int {
                    return getOpenSourceCheckPeriod(paramType)
                }
            }
        )

    /**
     * 用于codecc集群路由缓存
     */
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

    /**
     * 用于触发频率的缓存
     */
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

        var customProjEntity = customProjDao.findByGongfengIdAndUrlAndBranch(
            appcodeMappingCache.get(appCode),
            triggerPipelineReq.gongfengProjectId,
            triggerPipelineReq.gitUrl,
            triggerPipelineReq.branch,
            triggerPipelineReq.logicRepo
        )
        //应运管要求，设置验重机制
        val dupValidateResult = oteamDuplicateValidate(
            customProjEntity = customProjEntity,
            appCode = appCode,
            triggerPipelineReq = triggerPipelineReq
        )
        val needUpdateTask = dupValidateResult.first
        customProjEntity = dupValidateResult.second
        val gongfengPublicProjEntity = dupValidateResult.third

        val codeccBuildId = UUIDUtil.generate()

        //如果为空，则需要重新新建项目和流水线
        if (null == customProjEntity) {
            //检查项目的有效性
            checkGongfengProject(triggerPipelineReq, customProjEntity)
            //创建流水线
            val newCustomProjEntity = handleWithCheckProjPipeline(appCode, triggerPipelineReq, userId)
            //组装运行时参数并下发
            return with(newCustomProjEntity) {
                val paramMap = assembleRuntimeParam("true", codeccBuildId, appCode, triggerPipelineReq, this)
                val customTriggerPipelineModel = CustomTriggerPipelineModel(
                    customProjEntity = this,
                    runtimeParam = paramMap,

                    userId = userId
                )
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CUSTOM_PIPELINE_TRIGGER,
                    "$ROUTE_CUSTOM_PIPELINE_TRIGGER.${(newCustomProjEntity.dispatchRoute?:ComConstants.CodeCCDispatchRoute.INDEPENDENT).name.toLowerCase()}",
                    customTriggerPipelineModel
                )
                //如果是已有开源记录，但是流水线信息为空，则说明应该有失效任务，将流水线和项目id赋值给该失效任务,并且将失效任务打开
                if (needUpdateTask) {
                    val updatedTask = gongfengCreateTaskListener.setValueToInvalidTask(
                        TriggerPipelineModel(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            taskId = 0L,
                            gongfengId = gongfengProjectId,
                            owner = ""
                        )
                    )
                    if (null != gongfengPublicProjEntity) {
                        gongfengPublicProjEntity.projectId = projectId
                        gongfengPublicProjEntity.pipelineId = pipelineId
                        gongfengPublicProjRepository.save(gongfengPublicProjEntity)
                    }
                    if (null != updatedTask) {
                        this.taskId = updatedTask.taskId
                        gongfengProjectChecker.startTask(updatedTask)
                        customProjRepository.save(this)
                    }
                }
                assembleReturnModel(this, "true", codeccBuildId)
            }
        } else {
            //检查项目的有效性
            checkGongfengProject(triggerPipelineReq, customProjEntity)
            //为了兼容原版，如果没有更新成新的modeljson，则进行更新
            updateCustomizedCheckProjPipeline(
                triggerPipelineReq.codeCCPipelineReq,
                customProjEntity,
                getCodeCCDispatchRoute(appCode, triggerPipelineReq),
                triggerPipelineReq.useYml,
                userId,
                appCode
            )
            //更新触发记录相应字段
            customProjEntity.gongfengProjectId = triggerPipelineReq.gongfengProjectId
            customProjEntity.url = triggerPipelineReq.gitUrl
            customProjEntity.commonModelJson = true
            customProjEntity.runtimeParam =
                triggerPipelineReq.codeCCPipelineReq.runtimeParamMap?.associate { it.paramCode to it.paramValue }
            if (!triggerPipelineReq.checkerSetRange.isNullOrEmpty()) {
                customProjEntity.checkerSetRange = triggerPipelineReq.checkerSetRange!!.map {
                    val opensourceCheckerSet = OpenSourceCheckerSet()
                    opensourceCheckerSet.checkerSetId = it.checkerSetId
                    if (null != it.version) {
                        opensourceCheckerSet.version = it.version
                    }
                    opensourceCheckerSet
                }
            }
            customProjRepository.save(customProjEntity)
            return with(customProjEntity) {
                val paramMap = assembleRuntimeParam("false", codeccBuildId, appCode, triggerPipelineReq, this)
                val customTriggerPipelineModel = CustomTriggerPipelineModel(
                    customProjEntity = this,
                    runtimeParam = paramMap,
                    userId = userId
                )
                rabbitTemplate.convertAndSend(
                    EXCHANGE_CUSTOM_PIPELINE_TRIGGER,
                    "$ROUTE_CUSTOM_PIPELINE_TRIGGER.${(customProjEntity.dispatchRoute?:ComConstants.CodeCCDispatchRoute.INDEPENDENT).name.toLowerCase()}",
                    customTriggerPipelineModel
                )
                assembleReturnModel(this, "false", codeccBuildId)
            }
        }
    }

    override fun stopRunningApiTask(
        codeccBuildId: String,
        appCode: String,
        userId: String
    ) {
        logger.info("stop running api task, codecc build id: $codeccBuildId")
        val buildIdRelationShipEntity = buildIdRelationshipRepository.findFirstByCodeccBuildId(codeccBuildId)
        if (null == buildIdRelationShipEntity || buildIdRelationShipEntity.pipelineId.isNullOrBlank()) {
            logger.info("no build id relationship entity found with codecc build id: $codeccBuildId")
            return
        }
        val taskInfoEntity = taskRepository.findByPipelineId(buildIdRelationShipEntity.pipelineId)
        if (null == taskInfoEntity || taskInfoEntity.taskId == 0L) {
            logger.info("no task found with pipeline id ${buildIdRelationShipEntity.pipelineId}")
            return
        }
        //停止流水线
        with(taskInfoEntity) {
            client.getDevopsService(ServiceBuildResource::class.java).manualShutdown(
                userId, projectId, pipelineId, buildIdRelationShipEntity.buildId, ChannelCode.GONGFENGSCAN
            )
        }
    }

    /**
     * 处理oteam去重校验
     */
    private fun oteamDuplicateValidate(
        customProjEntity: CustomProjEntity?,
        appCode: String,
        triggerPipelineReq: TriggerPipelineReq
    ): Triple<Boolean, CustomProjEntity?, GongfengPublicProjEntity?> {
        var gongfengPublicProjEntity: GongfengPublicProjEntity? = null
        var resultCustomProjEntity = customProjEntity
        var needUpdateTask = false
        if (null == resultCustomProjEntity && OTEAM_APP_CODE == appCode) {
            gongfengPublicProjEntity = gongfengPublicProjRepository.findById(triggerPipelineReq.gongfengProjectId ?: 0)
            if (gongfengPublicProjEntity != null) {
                if (!gongfengPublicProjEntity.pipelineId.isNullOrBlank() && !gongfengPublicProjEntity.projectId.isNullOrBlank()) {
                    resultCustomProjEntity = newCustomProjEntity(triggerPipelineReq, appCode)
                    resultCustomProjEntity.projectId = gongfengPublicProjEntity.projectId
                    resultCustomProjEntity.pipelineId = gongfengPublicProjEntity.pipelineId
                    resultCustomProjEntity.commonModelJson = true
                    customProjRepository.save(resultCustomProjEntity)
                } else {
                    val taskInfoEntity = findCorrespondedTask(triggerPipelineReq.gongfengProjectId ?: 0)
                    //如果该代码库对应的定时开源扫描任务包含有流水线信息，则直接用该条流水线，并且启用该任务
                    if (null != taskInfoEntity && !taskInfoEntity.pipelineId.isNullOrBlank()) {
                        resultCustomProjEntity = newCustomProjEntity(triggerPipelineReq, appCode)
                        resultCustomProjEntity.projectId = taskInfoEntity.projectId
                        resultCustomProjEntity.pipelineId = taskInfoEntity.pipelineId
                        resultCustomProjEntity.taskId = taskInfoEntity.taskId
                        resultCustomProjEntity.commonModelJson = true
                        customProjRepository.save(resultCustomProjEntity)
                        gongfengProjectChecker.startTask(taskInfoEntity)
                        gongfengPublicProjEntity.projectId = taskInfoEntity.projectId
                        gongfengPublicProjEntity.pipelineId = taskInfoEntity.pipelineId
                        gongfengPublicProjRepository.save(gongfengPublicProjEntity)
                    } else {
                        //如果不是，则新增流水线，并且更新原有对应的失效task
                        needUpdateTask = true
                    }
                }
            }
        }
        return Triple(needUpdateTask, resultCustomProjEntity, gongfengPublicProjEntity)
    }

    /**
     * 检查项目的有效性
     */
    private fun checkGongfengProject(triggerPipelineReq: TriggerPipelineReq, customProjEntity: CustomProjEntity?) {
        val checkResult =
            if (triggerPipelineReq.checkGongfengProject != null && triggerPipelineReq.checkGongfengProject!!) {
                logger.info("start to check gongfeng project, trigger req: $triggerPipelineReq")
                val taskInfoEntity = if (null == customProjEntity || customProjEntity.pipelineId.isNullOrBlank()) {
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
                        gongfengPublicProjRepository.findById(taskInfoEntity.gongfengProjectId)
                    gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(taskInfoEntity.gongfengProjectId)
                } else {
                    gongfengPublicProjEntity = gongfengPublicProjRepository.findById(gongfengProjectId)
                    gongfengStatProjEntity = gongfengStatProjRepository.findFirstById(gongfengProjectId)
                }

                gongfengProjectChecker.configurableCheck(
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
            val dispatchRoute = customTriggerPipelineModel.customProjEntity.dispatchRoute
            val apiFreq = if(null != dispatchRoute) (frequencyCache.get(dispatchRoute.name) ?: "16000") else frequencyCache.get("api")
            logger.info("trigger interval period is $apiFreq, dispatch route is $dispatchRoute")
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
                    (runtimeParam["firstTrigger"] ?: "false").toBoolean()
                )
                buildIdRelationshipRepository.save(buildIdRelationshipEntity)
                //如果是oteam的项目，则需要记录流水线维度的流水表
                if (customProjEntity.appCode == OTEAM_APP_CODE) {
                    val pipelineIdRelationshipEntity = PipelineIdRelationshipEntity(
                        customProjEntity.taskId,
                        customProjEntity.projectId,
                        customProjEntity.pipelineId,
                        ComConstants.ScanStatus.PROCESSING.code,
                        LocalDate.now()
                    )
                    pipelineIdRelationService.updateFailOrProcessRecord(pipelineIdRelationshipEntity)
                }

            }
        } catch (e: Exception) {
            logger.info("trigger pipeline fail! project id: ${customTriggerPipelineModel.customProjEntity.projectId}, url: ${customTriggerPipelineModel.customProjEntity.url}")
        }
    }

    override fun triggerGongfengTaskByRepoId(repoId: String, commitId: String?): String? {
        var nCommitId = commitId
        var startTime = System.currentTimeMillis()
        // 如果commitId为空，则去工蜂获取
        if (nCommitId.isNullOrBlank()) {
            nCommitId = getGongfengCommitInfo(repoId)?.id
        }
        logger.info("get commit info cost: ${System.currentTimeMillis() - startTime}")
        startTime = System.currentTimeMillis()
        // 从工蜂获取项目是否开源
        val gongfengModel = getGongfengProjectInfo(repoId)
        logger.info("get repo info cost: ${System.currentTimeMillis() - startTime}")
        val gongfengId = gongfengModel!!.id
        val owner: String? = null

        // 从开源和私有两个表拿数据，工蜂信息没有同步就说明没创建，抛出 2300020
        var taskInfoEntity =
            taskRepository.findByGongfengProjectIdAndStatusAndProjectIdRegex(
                gongfengModel.id,
                TaskConstants.TaskStatus.ENABLE.value(),
                "^(CODE_)"
            )
        if (null == taskInfoEntity || taskInfoEntity.taskId == 0L) {
            var customProjEntity =
                customProjRepository.findByGongfengProjectIdAndCustomProjSource(gongfengModel.id, "TEG_CUSTOMIZED")
            if (null != customProjEntity && !customProjEntity.pipelineId.isNullOrBlank()) {
                taskInfoEntity = taskRepository.findByPipelineId(customProjEntity.pipelineId)
            }
            if (null == taskInfoEntity || taskInfoEntity.taskId == 0L || taskInfoEntity.status != TaskConstants.TaskStatus.ENABLE.value()) {
                customProjEntity = customProjRepository.findByGongfengProjectIdAndCustomProjSource(
                    gongfengModel.id,
                    "bkdevops-plugins"
                )
                if (null != customProjEntity && !customProjEntity.pipelineId.isNullOrBlank()) {
                    taskInfoEntity = taskRepository.findByPipelineId(customProjEntity.pipelineId)
                }
            }
        }

        // 如果工蜂代码库已经同步到codecc，但是定时任务还未启动，扫描任务还未创建
        if (taskInfoEntity == null) {
            logger.info("no task info found!")
            val triggerParam: GongFengTriggerParamEntity? =
                gongfengTriggerParamRepository.findByGongfengId(gongfengId)
            // 如果启动参数未保存，则扫描任务还未创建
            if (triggerParam == null) {
                logger.info("cann not find trigger param info, repoId: $repoId | commitId: $nCommitId")
                throw CodeCCException("2300020")
            }
            val triggerPipelineModel = TriggerPipelineModel(
                projectId = triggerParam.projectId,
                pipelineId = triggerParam.pipelineId,
                taskId = triggerParam.taskId,
                gongfengId = triggerParam.gongfengId,
                owner = triggerParam.owner,
                commitId = nCommitId
            )
            return executeTriggerPipeline(
                triggerPipelineModel,
                gongfengModel.httpUrlToRepo!!,
                gongfengModel.name
            )
        }

        // 工蜂信息已经同步 并且 扫描任务已经创建
        val triggerPipelineModel = TriggerPipelineModel(
            projectId = taskInfoEntity.projectId,
            pipelineId = taskInfoEntity.pipelineId,
            taskId = taskInfoEntity.taskId,
            gongfengId = taskInfoEntity.gongfengProjectId,
            owner = owner ?: taskInfoEntity.createdBy,
            commitId = nCommitId
        )
        return executeTriggerPipeline(
            triggerPipelineModel,
            gongfengModel.httpUrlToRepo!!,
            gongfengModel.name
        )
    }

    private fun getTaskFromPublic(id: Int, isFindCustom: Boolean): TaskInfoEntity? {
        logger.info("execute opensource trigger pipeline by repoId $id")
        val proj: GongfengPublicProjEntity = gongfengPublicProjRepository.findById(id)
            ?: return if (!isFindCustom) {
                getTaskFromCustom(id, true)
            } else {
                throw CodeCCException("2300020")
            }

        val taskInfoEntity = taskRepository.findByGongfengProjectIdAndStatusAndProjectIdRegex(
            proj.id,
            TaskConstants.TaskStatus.ENABLE.value(),
            "^(CODE_)"
        )

        return if (taskInfoEntity == null && !isFindCustom) {
            getTaskFromCustom(id, true)
        } else {
            taskInfoEntity
        }
    }

    private fun getTaskFromCustom(id: Int, isFindPublic: Boolean): TaskInfoEntity? {
        logger.info("get custom task to trigger pipeline by repoId $id")
        var proj = customProjRepository.findByGongfengProjectIdAndCustomProjSource(
            id,
            "bkdevops-plugins"
        ) ?: null

        if (proj == null) {
            proj = customProjRepository.findByGongfengProjectIdAndCustomProjSource(
                id,
                OTEAM_APP_CODE
            ) ?: null
        }

        if (proj == null) {
            logger.error("task has been not created: repoId: $id")
            return if (!isFindPublic) {
                getTaskFromPublic(id, true)
            } else {
                throw CodeCCException("2300020")
            }
        }

        val taskInfoEntity = taskRepository.findByTaskIdAndStatus(
            proj.taskId,
            TaskConstants.TaskStatus.ENABLE.value()
        ) ?: null

        return if (taskInfoEntity == null && !isFindPublic) {
            getTaskFromPublic(id, true)
        } else {
            taskInfoEntity
        }
    }

    override fun createTaskByRepoId(repoId: String, langs: List<String>): Boolean {
        logger.info("create task by repoId: $repoId | $langs")
        val gongfengModel = getGongfengProjectInfo(repoId)
        if (gongfengModel == null) {
            logger.error("create task by repoId fail: can not get gongfeng model, repoId: $repoId")
            return false
        }

        return executeCreateSingleTask(gongfengModel, langs)
    }

    private fun updateGongfengInfo(gongfengModel: GongfengPublicProjModel) {
        if (gongfengModel.visibilityLevel!! >= 10) {
            // 如果存在于开源代码库中，则更新代码库信息
            val gongfengPublicProjEntity: GongfengPublicProjEntity? =
                gongfengPublicProjRepository.findById(gongfengModel.id)
            if (gongfengPublicProjEntity != null) {
                logger.info("gongfeng public project is exists: ${gongfengModel.id} ${gongfengModel.pathWithNameSpace}")
                val newGongfengPublicProjEntity =
                    objectMapper.readValue(
                        objectMapper.writeValueAsString(gongfengModel),
                        GongfengPublicProjEntity::class.java
                    )
                BeanUtils.copyProperties(newGongfengPublicProjEntity, gongfengPublicProjEntity)
                gongfengPublicProjEntity.synchronizeTime = System.currentTimeMillis()
                gongfengPublicProjRepository.save(gongfengPublicProjEntity)
            } else {
                logger.info("new gongfeng public project: ${gongfengModel.id} ${gongfengModel.pathWithNameSpace}")
                val newGongfengPublicProjEntity =
                    objectMapper.readValue(
                        objectMapper.writeValueAsString(gongfengModel),
                        GongfengPublicProjEntity::class.java
                    )
                newGongfengPublicProjEntity.synchronizeTime = System.currentTimeMillis()
                gongfengPublicProjRepository.save(newGongfengPublicProjEntity)
            }
        }
    }

    private fun executeTriggerPipeline(
        triggerPipelineModel: TriggerPipelineModel,
        repoUrl: String,
        name: String
    ): String? {
        try {
            logger.info("start to trigger pipeline process! pipeline id: ${triggerPipelineModel.pipelineId}, user name : ${triggerPipelineModel.owner}")
            //获取任务信息，对比版本号
            var taskInfo = if (triggerPipelineModel.taskId > 0) {
                taskRepository.findByTaskId(triggerPipelineModel.taskId)
            } else {
                taskRepository.findByPipelineId(triggerPipelineModel.pipelineId)
            }
            if (null == taskInfo) {
                taskInfo = setValueToInvalidTask(triggerPipelineModel)
            }

            // 如果现有数据库中没有存代码库 owner 信息，需要去工蜂拉取
            val owner = triggerPipelineModel.owner
            //查询分支信息，比较提交信息进行扫描
            var branchInfo: GongfengBranchModel? = null
            try {
                val url =
                    "$gitCodePath/api/v3/projects/${triggerPipelineModel.gongfengId}/repository/branches/master"
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                branchInfo = objectMapper.readValue(result, GongfengBranchModel::class.java)
            } catch (e: Exception) {
                logger.info("no permission to the master branch, gongfeng id: ${triggerPipelineModel.gongfengId}")
            }

            var startTime = System.currentTimeMillis()
            // 更新流水线编排
            pipelineService.updateExistsCommonPipeline(
                triggerPipelineModel.gongfengId,
                repoUrl,
                triggerPipelineModel.projectId,
                triggerPipelineModel.taskId,
                triggerPipelineModel.pipelineId,
                triggerPipelineModel.owner,
                ComConstants.CodeCCDispatchRoute.OPENSOURCE,
                triggerPipelineModel.commitId
            )
            logger.info("update pipeline cost: ${System.currentTimeMillis() - startTime}")
            startTime = System.currentTimeMillis()

            val buildIdResult: Result<BuildId>
            with(triggerPipelineModel) {
                buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    owner, projectId,
                    pipelineId, mapOf("scheduledTriggerPipeline" to "true"), ChannelCode.GONGFENGSCAN
                )
                logger.info("trigger pipeline successfully! build id: ${buildIdResult.data?.id}")
            }
            logger.info("execute pipeline cost: ${System.currentTimeMillis() - startTime}")
            if (null != taskInfo) {
                if (null != branchInfo) {
                    taskDao.updateNameCnAndCommitId(
                        if (taskInfo.nameCn.startsWith("CODEPIPELINE")) {
                            name
                        } else {
                            null
                        },
                        branchInfo.commit.id,
                        System.currentTimeMillis(),
                        taskInfo.taskId
                    )
                }
            }

            // 只有 buildId 不为空的情况下才能在buildIdRelationship中获取到任务扫描状态
            if (StringUtils.isNotBlank(buildIdResult.data?.id)) {
                val buildIdRelationshipEntity = BuildIdRelationshipEntity(
                    taskInfo?.taskId ?: 0,
                    UUIDUtil.generate(),
                    taskInfo?.pipelineId ?: "",
                    buildIdResult.data?.id,
                    null,
                    ComConstants.ScanStatus.PROCESSING.code,
                    null,
                    0,
                    triggerPipelineModel.commitId,
                    true
                )
                // 保存执行信息，状态为执行中，等待idcsync更新状态
                buildIdRelationshipRepository.save(buildIdRelationshipEntity)
            }

            return buildIdResult.data?.id
        } catch (e: Exception) {
            logger.error(
                "trigger pipeline fail for gongfeng project scan! pipeline id: ${triggerPipelineModel.pipelineId}",
                e
            )
            gongfengFailPageRepository.save(
                GongfengFailPageEntity(
                    null, listOf(triggerPipelineModel.gongfengId),
                    "trigger pipeline", e.message, e.stackTrace!!.contentToString()
                )
            )
        }
        return null
    }

    /**
     * 触发单个工蜂扫描任务
     *
     * @param gongfengPublicProjModel 工蜂代码库信息模版
     */
    private fun executeCreateSingleTask(
        gongfengPublicProjModel: GongfengPublicProjModel,
        langs: List<String>
    ): Boolean {
        var isSuccess: Boolean
        logger.info("start to handle with new project")
        try {
            isSuccess = if (gongfengPublicProjModel.visibilityLevel!! < 10) {
                handleWithCheckProjPipeline(gongfengPublicProjModel, langs)
            } else {
                handleWithGongfengPublicProject(gongfengPublicProjModel, langs)
            }
        } catch (e: Exception) {
            logger.error("handle with gong feng public project fail!, err: ${e.message}")
            val gongfengHandleFailEntity = GongfengFailPageEntity(
                1,
                listOf(gongfengPublicProjModel.id),
                "create task",
                e.message,
                e.stackTrace?.contentToString()
            )
            gongfengFailPageRepository.save(gongfengHandleFailEntity)
            isSuccess = false
        }

        return isSuccess
    }

    /**
     * 对工蜂开源项目进行处理
     */
    private fun handleWithGongfengPublicProject(newProject: GongfengPublicProjModel, langs: List<String>): Boolean {
        logger.info("start to handle with new project, project id: ${newProject.id}")

        // 验证目前的owner在职、非机器人
        if (null != newProject.owner) {
            if (newProject.owner!!.state != "active" || !validateOwnerName(newProject.owner!!.userName)) {
                newProject.owner = null
            }
        }
        // 上一步验证失败，则获取项目的其他在职、非机器人owner
        if (null == newProject.owner) {
            setProjectOwnerByProject(newProject)
        }

        if (null == newProject.owner) {
            logger.info("no owner info for project: ${newProject.id}")
            gongfengFailPageRepository.save(
                GongfengFailPageEntity(
                    null, listOf(newProject.id),
                    "create task", "no owner info for project", null
                )
            )
            openSourceTaskRegisterService.registerDisabledTask(
                newProject.id, null, null,
                ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code
            )
            return false
        }

        val projectId: String
        val pipelineId: String

        try {
            //获取项目管理员
            val adminUser = newProject.owner!!.userName
            logger.info("project user name: $adminUser")
            //注册蓝盾项目
            projectId = pipelineService.createGongfengDevopsProject(newProject)
            logger.info("create project successfully! project id: $projectId")
            // 设置规则集
            val langCheckerSetMap = openSourceTaskRegisterService.setCheckerSetsAccordingToLanguage(langs)
            logger.info("lang checker set $langCheckerSetMap")
            val languageRuleSetMap = mutableMapOf<String, List<String>>()
            langCheckerSetMap.forEach { (k, v) ->
                languageRuleSetMap[k] = v.map { c -> c.checkerSetId }
            }
            val tools =
                langCheckerSetMap.values.flatMap { checkerSetVOList -> checkerSetVOList.flatMap { checkerSetVO -> checkerSetVO.toolList } }
            //创建蓝盾流水线 lang tool checkset
            pipelineId = pipelineService.createGongfengDevopsPipeline(newProject, projectId)
            logger.info("create pipeline successfully! pipeline id: $pipelineId")
            //添加定时任务
            addGongfengJob(
                newProject.id,
                newProject.owner!!.userName,
                projectId,
                pipelineId,
                langs,
                tools,
                langCheckerSetMap.values.flatten()
            )
            logger.info("send msg to kafka cluster successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("create task fail! gongfeng project id: ${newProject.id}")
            val gongfengHandleFailEntity = GongfengFailPageEntity(
                1,
                listOf(newProject.id),
                "create task",
                e.message,
                e.stackTrace?.contentToString()
            )
            gongfengFailPageRepository.save(gongfengHandleFailEntity)
            openSourceTaskRegisterService.registerDisabledTask(
                newProject.id, null, null,
                ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code
            )
            return false
        }

        logger.info("register task for gongfeng successfully!")
        updateGongfengInfo(newProject)
        logger.info("syccess to save gongfeng opensource project!˚")
        return true
    }

    private fun handleWithCheckProjPipeline(gongfengModel: GongfengPublicProjModel, langs: List<String>): Boolean {
        // 幂等处理
        var newCustomProjEntity: CustomProjEntity? =
            customProjRepository.findByGongfengProjectIdAndCustomProjSource(gongfengModel.id, "bkdevops-plugins")
        if (newCustomProjEntity != null) {
            logger.info("gongfeng custom project is exists: ${gongfengModel.id} ${gongfengModel.pathWithNameSpace}")
            newCustomProjEntity.gongfengProjectId = gongfengModel.id
            newCustomProjEntity.url = gongfengModel.httpUrlToRepo
            customProjRepository.save(newCustomProjEntity)
            return true
        } else {
            newCustomProjEntity = CustomProjEntity()
            logger.info("new gongfeng custom project: ${gongfengModel.id} ${gongfengModel.pathWithNameSpace}")
            newCustomProjEntity.gongfengProjectId = gongfengModel.id
            newCustomProjEntity.url = gongfengModel.httpUrlToRepo
            newCustomProjEntity.branch = "master"
            newCustomProjEntity.defectDisplay = true
            newCustomProjEntity.customProjSource = "bkdevops-plugins"
        }

        val userId = gongfengModel.owner!!.userName
        newCustomProjEntity = customProjRepository.save(newCustomProjEntity)
        val newCustomProjVO = CustomProjVO()
        org.springframework.beans.BeanUtils.copyProperties(newCustomProjEntity, newCustomProjVO)

        val projectId = pipelineService.createGongfengDevopsProject(gongfengModel)

        //获取员工组织信息
        val staffInfo = tofClientApi.getStaffInfoByUserName(userId)
        val organizationInfo = tofClientApi.getOrganizationInfoByGroupId(
            staffInfo.data?.GroupId
                ?: -1
        )

        //注册任务
        val taskDetailVO = TaskDetailVO()
        taskDetailVO.createFrom = ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
        taskDetailVO.projectId = projectId
        taskDetailVO.nameCn = gongfengModel.httpUrlToRepo
        taskDetailVO.pipelineName = "CODEPIPELINE_${gongfengModel.id}"
        taskDetailVO.gongfengFlag = true
        taskDetailVO.customProjInfo = newCustomProjVO
        taskDetailVO.nameEn = handleCnName(taskDetailVO.nameCn)
        taskDetailVO.gongfengProjectId = gongfengModel.id

        // 设置规则集
        val langCheckerSetMap = openSourceTaskRegisterService.setCheckerSetsAccordingToLanguage(langs)
        val languageRuleSetMap = mutableMapOf<String, List<String>>()
        langCheckerSetMap.forEach { (k, v) ->
            languageRuleSetMap[k] = v.map { c -> c.checkerSetId }
        }
        val tools =
            langCheckerSetMap.values.flatMap { checkerSetVOList -> checkerSetVOList.flatMap { checkerSetVO -> checkerSetVO.toolList } }

        taskDetailVO.devopsTools = objectMapper.writeValueAsString(tools)
        taskDetailVO.bgId = organizationInfo?.bgId ?: -1
        taskDetailVO.deptId = organizationInfo?.deptId ?: -1
        taskDetailVO.centerId = organizationInfo?.centerId ?: -1
        taskDetailVO.groupId = staffInfo.data?.GroupId ?: -1

        //创建蓝盾流水线
        val pipelineId = pipelineService.createGongfengDevopsPipeline(gongfengModel, projectId)
        logger.info("create custom pipeline successfully! pipeline id: $pipelineId")

        // 转换任务扫描的语言集，保证在创建Task的时候能够成功安装规则集
        val codeLang = langCheckerSetMap.values.flatten()
            .map(CheckerSetVO::getCodeLang)
            .reduce { t: Long?, u: Long? -> t?.and(u!!) }

        //按流水线注册形式注册
        taskDetailVO.pipelineId = pipelineId
        val taskResult = openSourceTaskRegisterService.registerTask(taskDetailVO, userId)
        val taskId = taskResult.taskId
        logger.info("register custom task successfully! task id: $taskId")

        // 保存任务启动参数
        val gongfengTriggerParam = GongFengTriggerParamEntity(
            gongfengModel.id,
            projectId,
            pipelineId,
            taskId,
            userId,
            langs,
            tools,
            langCheckerSetMap.values.flatten(),
            codeLang
        )
        gongfengTriggerParamRepository.save(gongfengTriggerParam)

        newCustomProjEntity.taskId = taskId
        newCustomProjEntity.pipelineId = pipelineId
        newCustomProjEntity.projectId = projectId
        customProjRepository.save(newCustomProjEntity)
        return true
    }

    private fun handleCnName(cnName: String): String? {
        if (org.apache.commons.lang.StringUtils.isEmpty(cnName)) {
            logger.error("cn name is empty!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(cnName), "")
        }
        val a = StringBuffer(cnName)
        for (i in a.indices) {
            val tmpStr = a.substring(i, i + 1)
            if (!tmpStr.matches(Regex("[a-zA-Z0-9_\\u4e00-\\u9fa5]"))) {
                a.replace(i, i + 1, "_")
            }
        }
        //长度限制50个字符以内
        return if (a.length > 50) {
            a.substring(0, 50)
        } else a.toString()
    }

    /**
     * 获取开源扫描时间周期和时间起点
     */
    private fun getOpenSourceCheckPeriod(paramCode: String): Int {
        val baseDataEntity =
            baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_PERIOD, paramCode)
        //如果是周期的默认值是24，起点的默认值是0
        return if (baseDataEntity.isNullOrEmpty()) {
            if (paramCode == "PERIOD") 24 else 0
        } else {
            try {
                baseDataEntity[0].paramValue.toInt()
            } catch (e: Exception) {
                if (paramCode == "PERIOD") 24 else 0
            }
        }
    }

    /**
     * 添加工蜂定时任务
     */
    private fun addGongfengJob(
        id: Int, userName: String, projectId: String,
        pipelineId: String, langs: List<String>,
        tools: List<String>, languageRuleSetMap: List<CheckerSetVO>
    ): JobExternalDto {
        val jobExternalDto = JobExternalDto(
            jobName = null,
            className = "TriggerPipelineScheduleTask",
            classUrl = "${publicClassUrl}TriggerPipelineScheduleTask.java",
            cronExpression = getGongfengTriggerCronExpression(id),
            jobCustomParam = mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "taskId" to 0,
                "gongfengId" to id,
                "owner" to userName
            ),
            operType = OperationType.ADD
        )
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto)

        // 转换任务扫描的语言集，保证在创建Task的时候能够成功安装规则集
        val codeLang =
            languageRuleSetMap.stream().map(CheckerSetVO::getCodeLang).reduce { t: Long?, u: Long? -> t?.and(u!!) }
        // 保存任务启动参数
        val gongfengTriggerParam = GongFengTriggerParamEntity(
            id,
            projectId,
            pipelineId,
            0L,
            userName,
            langs,
            tools,
            languageRuleSetMap,
            codeLang.get()
        )
        gongfengTriggerParamRepository.save(gongfengTriggerParam)
        return jobExternalDto
    }

    //将定时时间全天平均，以10分钟为间隔
    private fun getGongfengTriggerCronExpression(gongfengId: Int): String {
        val period = periodInfoCache.get("PERIOD")
        val startTime = periodInfoCache.get("STARTTIME")
        val remainder = gongfengId % (period * 6)
        val minuteNum = (remainder % 6) * 10
        val hourNum = (startTime + ((remainder / 6) % period)) % 24
        return "0 $minuteNum $hourNum * * ?"
    }

    /**
     * 根据项目成员清单设置owner信息
     */
    private fun setProjectOwnerByProject(newProject: GongfengPublicProjModel) {
        var page = 1
        var dataSize: Int
        do {
            val url = "$gitCodePath/api/v3/projects/${newProject.id}/members/all?page=$page&per_page=1000"
            try {
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                val ownerList: List<OwnerInfo> =
                    objectMapper.readValue(result, object : TypeReference<List<OwnerInfo>>() {})
                logger.info("get project members,project id: ${newProject.id},members size: ${ownerList.size}")
                if (ownerList.isNullOrEmpty()) {
                    return
                }
                dataSize = ownerList.size
                for (ownerInfo in ownerList) {
                    if (ownerInfo.accessLevel == 50 && ownerInfo.state == "active" && validateOwnerName(ownerInfo.userName)) {
                        logger.info("validated owner, username: ${ownerInfo.userName}, project id: ${newProject.id}")
                        newProject.owner = ownerInfo
                        return
                    }
                    logger.info("invalidated owner, username: ${ownerInfo.userName}, project id: ${newProject.id}")
                }
                page++
            } catch (e: Exception) {
                logger.error("get project member info fail! gongfeng id: ${newProject.id}")
                return
            }
        } while (dataSize >= 1000)
    }

    /**
     *  验证rtx用户名
     */
    private fun validateOwnerName(userName: String): Boolean {
        val staffInfo = tofClientApi.getStaffInfoByUserName(userName)
        if (staffInfo.code != "00") {
            logger.info("tof client api error, code : ${staffInfo.code}, message:${staffInfo.message}, username: $userName")
        }
        return userName == staffInfo.data?.LoginName
    }

    /**
     * 为首次触发的失效项目补齐数据
     */
    private fun setValueToInvalidTask(triggerPipelineModel: TriggerPipelineModel): TaskInfoEntity? {
        val taskInfoList = taskRepository.findByGongfengProjectIdIsAndCreateFromIs(
            triggerPipelineModel.gongfengId,
            ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
        )
        //防止原来因为首次触发失效的任务没有项目和流水线字段，导致会重复创建项目
        if (!taskInfoList.isNullOrEmpty()) {
            val taskInfo = taskInfoList.findLast {
                (it.projectId.isNullOrBlank() || !it.projectId.startsWith("CUSTOMPROJ_")) &&
                    (!it.nameCn.isNullOrBlank() && it.nameCn == "CODEPIPELINE_${triggerPipelineModel.gongfengId}") && it.status == TaskConstants.TaskStatus.DISABLE.value()
            }
            if (null != taskInfo && taskInfo.taskId > 0L) {
                if (taskInfo.projectId.isNullOrBlank() || taskInfo.pipelineId.isNullOrBlank()) {
                    taskDao.updateProjectIdAndPipelineId(
                        if (taskInfo.projectId.isNullOrBlank()) {
                            triggerPipelineModel.projectId
                        } else {
                            null
                        },
                        if (taskInfo.pipelineId.isNullOrBlank()) {
                            triggerPipelineModel.pipelineId
                        } else {
                            null
                        },
                        taskInfo.taskId
                    )
                }
                return taskInfo
            }
        }
        return null
    }

    /**
     * 通过repoId获取代码库详细信息
     *
     * */
    private fun getGongfengProjectInfo(repoId: String): GongfengPublicProjModel? {
        val url = "$gitCodePath/api/v3/projects/${URLEncoder.encode(repoId, "UTF-8")}"

        //从工蜂拉取信息，并按分页下发
        val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
        if (result.isBlank()) {
            logger.info("null returned from api")
            return null
        }
        return objectMapper.readValue(result, GongfengPublicProjModel::class.java)
    }

    private fun getGongfengCommitInfo(repoId: String): GongfengCommitModel? {
        val url =
            "$gitCodePath/api/v3/projects/${URLEncoder.encode(repoId, "UTF-8")}/repository/commits?page=1&&per_page=1"

        val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
        if (result.isBlank()) {
            logger.info("null returned from api")
            return null
        }
        val gongfengCommitModel: List<GongfengCommitModel> =
            objectMapper.readValue(result, object : TypeReference<List<GongfengCommitModel>>() {})
        return gongfengCommitModel.first()
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
        appCode: String,
        triggerPipelineReq: TriggerPipelineReq,
        customProjEntity: CustomProjEntity
    ): Map<String, String> {
        val paramMap = mutableMapOf<String, String>()
        paramMap["firstTrigger"] = firstTrigger
        paramMap["manualTriggerPipeline"] = "true"
        paramMap["codeccBuildId"] = codeccBuildId
        paramMap["appCode"] = appCode
        paramMap["OTeam"] = (null != customProjEntity.oTeamCiProj && customProjEntity.oTeamCiProj).toString()
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
                    if (!taskInfoEntity.toolConfigInfoList.isNullOrEmpty()) taskInfoEntity.toolConfigInfoList
                        .filter { null != it && it.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value() }
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
        val newCustomProjEntity = newCustomProjEntity(triggerPipelineReq, appCode)
        logger.info("finish create custom project, app code: $appCode")
        val projectId = createCustomDevopsProject(newCustomProjEntity, userId)
        newCustomProjEntity.projectId = projectId
        logger.info("finish create devops project, app code: $appCode")
        val pipelineId =
            createCustomizedCheckProjPipeline(
                triggerPipelineReq.codeCCPipelineReq,
                newCustomProjEntity,
                getCodeCCDispatchRoute(appCode, triggerPipelineReq),
                triggerPipelineReq.useYml,
                userId
            )

        // oteam的项目需要配置定时任务
        if (OTEAM_APP_CODE == appCode) {
            gongfengCreateTaskListener.addGongfengJob(
                triggerPipelineReq.gongfengProjectId!!,
                userId,
                projectId,
                pipelineId
            )
        }

        newCustomProjEntity.pipelineId = pipelineId
        newCustomProjEntity.commonModelJson = true
        //新增保存运行时参数机制
        newCustomProjEntity.runtimeParam =
            triggerPipelineReq.codeCCPipelineReq.runtimeParamMap?.associate { it.paramCode to it.paramValue }

        logger.info("finish create customized check project pipeline: $appCode")
        return customProjRepository.save(newCustomProjEntity)
    }

    /**
     * 获取codecc路由信息
     */
    private fun getCodeCCDispatchRoute(appCode: String, triggerPipelineReq: TriggerPipelineReq): ComConstants.CodeCCDispatchRoute{
        //modified 2020-07-01 by neildwu, 流水线切换为devcloud路由，原有的触发也要切换
        val routeConfig = dispatchRouteCache.get("Trigger")
        val appCodes = if (!routeConfig.isNullOrBlank()) {
            JsonUtil.to(routeConfig, object : TypeReference<List<String>>() {})
        } else {
            emptyList()
        }
        logger.info("app codes : $appCodes, current app code : $appCode")
        /**
         * 集群路由规则：
         * 1. 先看缓存的路由配置，如果配置appCode清单中有，则到独立集群，否则则到缓存集群
         * 2. 再看传入参数指定的路由，如果有指定则按照指定的路由进行
         * 3. 对于devcloud集群的，还需要查询devcloud切换表，用于渐变切换
         */
        return if (appCodes.contains(appCode)) {
            ComConstants.CodeCCDispatchRoute.INDEPENDENT
        } else if(null != triggerPipelineReq.codeccDispatchRoute){
            triggerPipelineReq.codeccDispatchRoute!!
        } else {
            ComConstants.CodeCCDispatchRoute.DEVCLOUD
        }

    }

    /**
     * 寻找对应的定时开源项目
     */
    private fun findCorrespondedTask(gongfengProjectId: Int): TaskInfoEntity? {
        val taskInfoList = taskRepository.findByGongfengProjectIdIsAndCreateFromIs(
            gongfengProjectId,
            ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
        )
        //防止原来因为首次触发失效的任务没有项目和流水线字段，导致会重复创建项目
        return if (!taskInfoList.isNullOrEmpty()) {
            taskInfoList.findLast {
                (it.projectId.isNullOrBlank() || (it.projectId == "CODE_$gongfengProjectId"))
            }
        } else {
            null
        }
    }

    /**
     * 创建个性化项目
     */
    private fun newCustomProjEntity(triggerPipelineReq: TriggerPipelineReq, appCode: String): CustomProjEntity {
        val newCustomProjEntity = CustomProjEntity()
        newCustomProjEntity.url = triggerPipelineReq.gitUrl
        newCustomProjEntity.branch = triggerPipelineReq.branch
        newCustomProjEntity.gongfengProjectId = triggerPipelineReq.gongfengProjectId
        newCustomProjEntity.repositoryType = triggerPipelineReq.repoType
        newCustomProjEntity.defectDisplay = triggerPipelineReq.defectDisplay
        newCustomProjEntity.appCode = appCode
        newCustomProjEntity.customProjSource = appcodeMappingCache.get(appCode)
        newCustomProjEntity.logicRepo = triggerPipelineReq.logicRepo
        newCustomProjEntity.checkerSetRange = if (!triggerPipelineReq.checkerSetRange.isNullOrEmpty()) {
            val opensourceList = triggerPipelineReq.checkerSetRange!!.map {
                val opensourceCheckerSet = OpenSourceCheckerSet()
                opensourceCheckerSet.checkerSetId = it.checkerSetId
                if (null != it.version) {
                    opensourceCheckerSet.version = it.version
                }
                opensourceCheckerSet
            }
            opensourceList
        } else {
            emptyList()
        }

        return customProjRepository.save(newCustomProjEntity)
    }

    /**
     * 创建蓝盾项目
     */
    private fun createCustomDevopsProject(customProjEntity: CustomProjEntity, userId: String): String {
        // TODO("not implemented")
        return ""
    }

    /**
     * 创建流水线
     */
    private fun createCustomizedCheckProjPipeline(
        codeCCPipelineReq: CodeCCPipelineReq,
        customProjEntity: CustomProjEntity,
        dispatchRoute: ComConstants.CodeCCDispatchRoute,
        useYml: Boolean?,
        userId: String
    ): String {
        logger.info("start to create pipeline model")
        val assembleResult = if (null != useYml && useYml) {
            try {
                Pair(
                    gongfengOteamCoverityService.parseCiYml(
                        codeCCPipelineReq, customProjEntity, userId
                    ), true
                )
            } catch (e: Exception) {
                logger.info("parse ci yml fail! will return to normal pipeline assemble")
                Pair(assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute), false)
            }
        } else {
            Pair(assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute), false)
        }
        customProjEntity.oTeamCiProj = assembleResult.second
        customProjEntity.dispatchRoute = dispatchRoute
        /**
         * 总流水线拼装
         */
        val uuid = UUIDUtil.generate()
        val pipelineModel = Model(
            name = "CUSTOM_$uuid",
            desc = "个性化工蜂集群流水线$uuid",
            stages = assembleResult.first,
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
        useYml: Boolean?,
        userId: String,
        appCode : String
    ) {
        logger.info("start to update pipeline model")
        val assembleResult = if (null != useYml && useYml) {
            try {
                Pair(
                    gongfengOteamCoverityService.parseCiYml(
                        codeCCPipelineReq, customProjEntity, userId
                    ), true
                )
            } catch (e: Exception) {
                logger.info("fail to parse ci yml, will return to normal pipeline assemble")
                if (!needToUpdatePipeline(customProjEntity, codeCCPipelineReq, appCode, dispatchRoute)) {
                    logger.info("no need to update pipeline")
                    customProjEntity.dispatchRoute = dispatchRoute
                    return
                }
                Pair(assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute), false)
            }
        } else {
            if (!needToUpdatePipeline(customProjEntity, codeCCPipelineReq, appCode, dispatchRoute)) {
                logger.info("no need to update pipeline")
                customProjEntity.dispatchRoute = dispatchRoute
                return
            }
            Pair(assemblePipelineModel(codeCCPipelineReq, customProjEntity, dispatchRoute), false)
        }
        customProjEntity.oTeamCiProj = assembleResult.second
        customProjEntity.dispatchRoute = dispatchRoute
        val previousModel = try {
            client.getDevopsService(ServicePipelineResource::class.java)
                .get(userId, customProjEntity.projectId, customProjEntity.pipelineId, ChannelCode.GONGFENGSCAN).data
        } catch (e: Exception) {
            null
        }

        val pipelineModel = Model(
            name = "",
            desc = "",
            stages = assembleResult.first,
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
     * 是否需要更新流水线
     */
    private fun needToUpdatePipeline(
        customProjEntity: CustomProjEntity,
        codeCCPipelineReq: CodeCCPipelineReq,
        appCode: String,
        dispatchRoute: ComConstants.CodeCCDispatchRoute
    ): Boolean {
        val pipelineModelInfo = customProjEntity.pipelineModelInfo
        if(appCode != EPC_APP_CODE){
            logger.info("no epc app code pass!")
            return true
        }
        //如果本次的入参与上次的入参相同，则不需要更新流水线
        return if (null != pipelineModelInfo &&
            //1. git插件版本相同
                (
                !pluginVersionCache.get("GIT").isNullOrBlank() &&
                    pluginVersionCache.get("GIT") == pipelineModelInfo.gitPluginVersion
            ) &&
            //2. codecc插件版本相同
            (!pluginVersionCache.get("CODECC").isNullOrBlank() &&
                pluginVersionCache.get("CODECC") == pipelineModelInfo.codeccPluginVersion) &&
            //3. commit id 相同
            (codeCCPipelineReq.repoInfo.commitId == pipelineModelInfo.commitId) &&
            //4. runtimemap 相同
            (judgeSameParam(
                codeCCPipelineReq.runtimeParamMap?.associate { it.paramCode to it.paramValue },
                pipelineModelInfo.runtimeMap
            )) &&
            //5. scanParamMap 相同
            (judgeSameParam(codeCCPipelineReq.codeScanInfo.scanParamMap, pipelineModelInfo.scanParamMap)) &&
            //6. 之前配置的dispatch为空或者值相同
            (pipelineModelInfo.dispatchRoute == dispatchRoute.name)
        ) {
            false
        } else {
            if (null == customProjEntity.pipelineModelInfo) {
                customProjEntity.pipelineModelInfo = CustomProjVersionEntity(
                    pluginVersionCache.get("GIT"),
                    pluginVersionCache.get("CODECC"),
                    codeCCPipelineReq.repoInfo.commitId,
                    codeCCPipelineReq.runtimeParamMap?.associate { it.paramCode to it.paramValue },
                    codeCCPipelineReq.codeScanInfo.scanParamMap,
                    dispatchRoute.name
                )
            } else {
                customProjEntity.pipelineModelInfo.codeccPluginVersion = pluginVersionCache.get("CODECC")
                customProjEntity.pipelineModelInfo.gitPluginVersion = pluginVersionCache.get("GIT")
                customProjEntity.pipelineModelInfo.commitId = codeCCPipelineReq.repoInfo.commitId
                customProjEntity.pipelineModelInfo.runtimeMap = codeCCPipelineReq.runtimeParamMap?.associate { it.paramCode to it.paramValue }
                customProjEntity.pipelineModelInfo.scanParamMap = codeCCPipelineReq.codeScanInfo.scanParamMap
                customProjEntity.pipelineModelInfo.dispatchRoute = dispatchRoute.name
            }
            true
        }
    }


    /**
     * 判断codecc扫描参数是否相同
     */
    private fun judgeSameParam(
        currentScanParamMap: Map<String, Any?>?,
        lastScanParamMap: Map<String, Any?>?
    ): Boolean {
        return if (currentScanParamMap.isNullOrEmpty() && lastScanParamMap.isNullOrEmpty()) {
            true
        } else if (currentScanParamMap.isNullOrEmpty() || lastScanParamMap.isNullOrEmpty()) {
            false
        } else {
            (currentScanParamMap.size == lastScanParamMap.size &&
                currentScanParamMap.all {
                    lastScanParamMap.any { lastScanParam ->
                        it.key == lastScanParam.key && it.value.toString() == lastScanParam.value.toString()
                    }
                })
        }
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
            ),
            BuildFormProperty(
                id = "appCode",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "触发来源",
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
                        required = false,
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
            containerId = null,
            jobId = null
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
            "pullType" to if (!codeccRepoInfo.commitId.isNullOrBlank()) "COMMIT_ID" else "BRANCH",
            "strategy" to "REVERT_UPDATE",
            "tagName" to "",
            "repositoryUrl" to customProjEntity.url
        )
        if (!codeccRepoInfo.commitId.isNullOrBlank()) {
            gitInputMap["refName"] = codeccRepoInfo.commitId!!
        } else if (!customProjEntity.branch.isNullOrBlank()) {
            gitInputMap["refName"] = customProjEntity.branch
        }
        when (codeccRepoInfo) {
            is CodeCCTokenAuthInfo -> {
                gitInputMap["accessToken"] = codeccRepoInfo.accessToken
            }
            is CodeCCAccountAuthInfo -> {
                gitInputMap["username"] = codeccRepoInfo.userName
                gitInputMap["password"] = codeccRepoInfo.passWord
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
            customCondition = "",
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
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
            customCondition = "",
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
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
                codeccTaskId = dispatchRoute.flag(),
                extraInfo = mapOf()
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
