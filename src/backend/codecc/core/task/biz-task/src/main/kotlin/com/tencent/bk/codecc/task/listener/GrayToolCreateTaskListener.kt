package com.tencent.bk.codecc.task.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.defect.api.ServiceStatisticRestResource
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.BuildIdRelationshipRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GrayTaskCategoryRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.dao.mongotemplate.GrayTaskCategoryDao
import com.tencent.bk.codecc.task.dao.mongotemplate.GrayToolReportDao
import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity
import com.tencent.bk.codecc.task.model.DisableTaskEntity
import com.tencent.bk.codecc.task.model.GrayTaskCategoryEntity
import com.tencent.bk.codecc.task.pojo.GrayToolProjectDto
import com.tencent.bk.codecc.task.pojo.TriggerPipelineModel
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.service.TaskRegisterService
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.util.OkhttpUtils
import com.tencent.devops.common.web.mq.EXCHANGE_EXTERNAL_JOB
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GrayDefectTaskSubEntity
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class GrayToolCreateTaskListener @Autowired constructor(
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val pipelineService: PipelineService,
    private val rabbitTemplate: RabbitTemplate,
    private val baseDataRepository: BaseDataRepository,
    @Qualifier("grayTaskRegisterServiceImpl") private val taskRegisterService: TaskRegisterService,
    private val grayTaskCategoryDao: GrayTaskCategoryDao,
    private val grayTaskCategoryRepository: GrayTaskCategoryRepository,
    private val taskRepository: TaskRepository,
    private val grayToolReportDao: GrayToolReportDao,
    private val buildIdRelationshipRepository: BuildIdRelationshipRepository,
    private val taskDao: TaskDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GrayToolCreateTaskListener::class.java)
    }

    @Value("\${codecc.classurl:#{null}}")
    private val publicClassUrl: String? = null

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${codecc.privatetoken:#{null}}")
    private val gitPrivateToken: String? = null

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

    fun handleWithGrayToolProject(grayToolProjectDto: GrayToolProjectDto) {
        logger.info("start to handle with new gray project, project id: ${grayToolProjectDto.gongfengProjectId}")
        try {
            val gongfengPublicProjEntity =
                gongfengPublicProjService.findProjectById(grayToolProjectDto.gongfengProjectId)
            if (null == gongfengPublicProjEntity || gongfengPublicProjEntity.id == null) {
                logger.info("no gongfeng public project correspond to id: ${grayToolProjectDto.gongfengProjectId}")
                return
            }
            val projectId = "${ComConstants.GRAY_PROJECT_PREFIX}${grayToolProjectDto.toolName}"
            // 1. 注册任务
            val taskDetailVO = TaskDetailVO()
            taskDetailVO.projectId = projectId
            taskDetailVO.gongfengProjectId = gongfengPublicProjEntity.id
            taskDetailVO.toolNames = grayToolProjectDto.toolName
            val taskIdInfo = taskRegisterService.registerTask(taskDetailVO, grayToolProjectDto.user)
            // 2. 添加定时任务
            addGongfengJob(
                grayToolProjectDto.gongfengProjectId,
                grayToolProjectDto.user,
                projectId,
                taskDetailVO.pipelineId,
                taskIdInfo.taskId
            )
            // 3.添加到灰度表
            val grayTaskCategoryEntity = GrayTaskCategoryEntity()
            grayTaskCategoryEntity.projectId = projectId
            grayTaskCategoryEntity.category = grayToolProjectDto.category
            grayTaskCategoryEntity.gongfengProjectId = grayToolProjectDto.gongfengProjectId
            grayTaskCategoryEntity.pipelineId = taskDetailVO.pipelineId
            grayTaskCategoryEntity.status = TaskConstants.TaskStatus.ENABLE.value()
            grayTaskCategoryEntity.taskId = taskIdInfo.taskId
            grayTaskCategoryDao.upsertByProjectIdAndGongfengProjectId(grayTaskCategoryEntity, grayToolProjectDto.user)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("create gray tool project fail! gongfeng id: ${grayToolProjectDto.gongfengProjectId}, tool name: ${grayToolProjectDto.toolName}, message: ${e.message}")
        }
    }

    fun executeTriggerGrayTask(triggerPipelineModel: TriggerPipelineModel) {
        logger.info("start to trigger gray task, task id: ${triggerPipelineModel.taskId}, pipeline id: ${triggerPipelineModel.pipelineId}")
        try {
            val gongfengPublicProjEntity = gongfengPublicProjService.findProjectById(triggerPipelineModel.gongfengId)
            // 如果找不到开源信息，则失效
            if (null == gongfengPublicProjEntity || null == gongfengPublicProjEntity.id) {
                logger.info("no gongfeng project found with id: ${triggerPipelineModel.gongfengId}")
                grayTaskCategoryDao.updateStatus(
                    triggerPipelineModel.projectId,
                    triggerPipelineModel.pipelineId,
                    TaskConstants.TaskStatus.DISABLE.value()
                )
                return
            }
            // 如果校验不再开源，则失效
            try {
                val url = "$gitCodePath/api/v3/projects/${gongfengPublicProjEntity.id}"
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
            } catch (e: Exception) {
                logger.info("this gongfeng project is no more open sourced, need to disable, project id: ${triggerPipelineModel.projectId}, " +
                    "pipeline id: ${triggerPipelineModel.pipelineId}")
                grayTaskCategoryDao.updateStatus(
                    triggerPipelineModel.projectId,
                    triggerPipelineModel.pipelineId,
                    TaskConstants.TaskStatus.DISABLE.value()
                )
                stopTask(triggerPipelineModel.pipelineId, "no public task for gray pool any more",
                    triggerPipelineModel.owner)
                return
            }
            // 限制下发频率
            val scheduleFreq = frequencyCache.get("schedule")
            logger.info("trigger interval period is $scheduleFreq")
            Thread.sleep(scheduleFreq.toLong())
            with(triggerPipelineModel) {
                val buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
                    owner, projectId,
                    pipelineId,
                    mapOf("scheduledTriggerPipeline" to "true"), ChannelCode.CODECC_EE
                )
                if (buildIdResult.isOk() && null != buildIdResult.data) {
                    logger.info("trigger pipeline successfully! build id: ${buildIdResult.data?.id}")
                    val buildId = buildIdResult.data!!.id
                    grayReportProcessing(buildId, this, gongfengPublicProjEntity)
                } else {
                    logger.info("trigger pipeline fail! pipeline id: $pipelineId")
                }
            }
        } catch (e: Exception) {
            logger.info(
                "trigger gray task fail! pipeline id: ${triggerPipelineModel.pipelineId}"
            )
        }
    }

    /**
     * 更新分类表（用于记录last_build_id），流水表(用于标识codeccBuildId和本次build_id关系)及报告表（用于灰度统计）
     * 1. 分类表只要构建成功，都会保存
     * 2. 流水表只有当codeccBuildId有值时，才会保存
     * 3. 报告表在触发时，计算lastReport值，在本次上报时，计算本次的值，只有当codeccBuildId有值时，才会计算
     */
    private fun grayReportProcessing(buildId: String,
                                     triggerPipelineModel: TriggerPipelineModel,
                                     gongfengPublicProjEntity: GongfengPublicProjEntity) {
        with(triggerPipelineModel) {
            val grayTaskCategoryEntity =
                grayTaskCategoryRepository.findFirstByProjectIdAndPipelineId(projectId, pipelineId)
            if (null == grayTaskCategoryEntity || grayTaskCategoryEntity.entityId.isNullOrBlank()) {
                logger.info("no gray task category found with pipeline id: $pipelineId")
                return
            }
            if (!codeccBuildId.isNullOrBlank() && !toolName.isNullOrBlank()) {
                // 1. 计算lastReport值
                val taskInfoEntity = taskRepository.findFirstByPipelineId(pipelineId)
                if (null != taskInfoEntity && 0L != taskInfoEntity.taskId) {
                    val statReportInfo = try {
                        if (!grayTaskCategoryEntity.lastBuildId.isNullOrBlank()) {
                            val statReportResult =
                                client.get(ServiceStatisticRestResource::class.java).getLintStatInfo(
                                    taskInfoEntity.taskId,
                                    toolName!!,
                                    grayTaskCategoryEntity.lastBuildId
                                )
                            if (statReportResult.isOk()) {
                                statReportResult.data
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        logger.info("get last stat info fail!")
                        null
                    }
                    val defectCount =
                        if (null != statReportInfo && statReportInfo.currStep == ComConstants.Step4MutliTool.COMMIT.value() &&
                            statReportInfo.flag == ComConstants.StepFlag.SUCC.value()) (statReportInfo.totalSerious ?: 0) + (statReportInfo.totalNormal ?: 0) + (statReportInfo.totalPrompt ?: 0)
                        else null
                    val elapsedTime = if (null != statReportInfo && statReportInfo.currStep == ComConstants.Step4MutliTool.COMMIT.value() &&
                        statReportInfo.flag == ComConstants.StepFlag.SUCC.value()) statReportInfo.elapsedTime
                    else null
                    val grayDefectTaskEntity = GrayDefectTaskSubEntity(taskId, defectCount, elapsedTime, 0, 0, false, gongfengPublicProjEntity.httpUrlToRepo)
                    grayToolReportDao.incrLastReportInfo(codeccBuildId, defectCount, elapsedTime, grayDefectTaskEntity)
                }
                // 2. 记录流水表信息
                val buildIdRelationshipEntity = BuildIdRelationshipEntity(
                    taskInfoEntity.taskId,
                    codeccBuildId,
                    pipelineId,
                    buildId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                buildIdRelationshipRepository.save(buildIdRelationshipEntity)
            }
            // 3.保存分类表
            grayTaskCategoryEntity.lastBuildId = buildId
            grayTaskCategoryRepository.save(grayTaskCategoryEntity)
        }
    }

    private fun stopTask(pipelineId: String, disableReason: String, userName: String) {
        val taskInfo = taskRepository.findFirstByPipelineId(pipelineId)
        if (null == taskInfo) {
            logger.info("no task found with id $pipelineId")
            return
        }
        val lastDisableTaskInfo = taskInfo.lastDisableTaskInfo ?: DisableTaskEntity()
        val executeTime = taskInfo.executeTime
        val executeDate = taskInfo.executeDate
        lastDisableTaskInfo.lastExecuteDate = executeDate
        lastDisableTaskInfo.lastExecuteTime = executeTime
        taskInfo.lastDisableTaskInfo = lastDisableTaskInfo
        taskInfo.executeDate = emptyList()
        taskInfo.executeTime = ""
        taskInfo.disableTime = System.currentTimeMillis().toString()
        taskInfo.disableReason = disableReason
        taskInfo.status = TaskConstants.TaskStatus.DISABLE.value()

        taskDao.updateEntity(taskInfo, userName)
    }

    /**
     * 添加工蜂定时任务
     */
    fun addGongfengJob(
        id: Int,
        userName: String,
        projectId: String,
        pipelineId: String,
        taskId: Long
    ) {
        val jobExternalDto = JobExternalDto(
            jobName = null,
            className = "TriggerGrayPoolScheduledTask",
            classUrl = "${publicClassUrl}TriggerGrayPoolScheduledTask.java",
            cronExpression = getGongfengTriggerCronExpression(id),
            jobCustomParam = mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "taskId" to taskId,
                "gongfengId" to id,
                "owner" to userName
            ),
            operType = OperationType.ADD
        )
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto)
    }

    // 将定时时间全天平均，以10分钟为间隔
    private fun getGongfengTriggerCronExpression(gongfengId: Int): String {
        val period = periodInfoCache.get("PERIOD")
        val startTime = periodInfoCache.get("STARTTIME")
        val remainder = gongfengId % (period * 6)
        val minuteNum = (remainder % 6) * 10
        val hourNum = (startTime + ((remainder / 6) % period)) % 24
        return "0 $minuteNum $hourNum * * ?"
    }

    /**
     * 获取开源扫描时间周期和时间起点
     */
    private fun getOpenSourceCheckPeriod(paramCode: String): Int {
        val baseDataEntity =
            baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_PERIOD, paramCode)
        // 如果是周期的默认值是24，起点的默认值是0
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
     * 获取appCode和原有来源映射
     */
    private fun getBaseDataInfo(paramType: String, paramCode: String): String? {
        val baseDataEntityList =
            baseDataRepository.findAllByParamTypeAndParamCode(paramType, paramCode)
        return if (baseDataEntityList.isNullOrEmpty()) null else baseDataEntityList[0].paramValue
    }
}
