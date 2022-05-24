package com.tencent.bk.codecc.task.service.impl

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.BuildIdRelationshipRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GrayToolProjectRepository
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository
import com.tencent.bk.codecc.task.dao.mongotemplate.GrayToolReportDao
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskIdVO
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.CheckerConstants
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.MD5Utils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service("grayTaskRegisterServiceImpl")
class GrayTaskRegisterServiceImpl @Autowired constructor(
    private val toolMetaRepository: ToolMetaRepository,
    private val gongfengPublicProjRepository: GongfengPublicProjRepository,
    private val buildIdRelationRepository: BuildIdRelationshipRepository,
    private val grayToolReportDao: GrayToolReportDao,
    private val grayToolProjectRepository: GrayToolProjectRepository
) : AbstractTaskRegisterService() {
    companion object {
        private val logger = LoggerFactory.getLogger(GrayTaskRegisterServiceImpl::class.java)
    }

    @Value("\${codecc.public.account:#{null}}")
    private val codeccPublicAccount: String? = null

    @Value("\${codecc.public.password:#{null}}")
    private val codeccPublicPassword: String? = null

    private val toolLanguageMappingCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, Pair<Long, Set<String>>>(
            object : CacheLoader<String, Pair<Long, Set<String>>>() {
                override fun load(toolName: String): Pair<Long, Set<String>> {
                    return try {
                        val languageSet = findLanguageSetByToolName(toolName)
                        logger.info("toolName[$toolName] language set [$languageSet].")
                        languageSet
                    } catch (t: Throwable) {
                        logger.info("toolName[$toolName] fail to get language set.")
                        Pair(0L, setOf())
                    }
                }
            }
        )

    override fun registerTask(taskDetailVO: TaskDetailVO, userName: String): TaskIdVO {
        logger.info("start to register task")
        taskDetailVO.createFrom = ComConstants.BsTaskCreateFrom.BS_CODECC.value()
        // 手动添加cloc工具
        val originalToolName = taskDetailVO.toolNames
        // 1. 创建流水线
        val gongfengPublicProjEntity = gongfengPublicProjRepository.findFirstById(taskDetailVO.gongfengProjectId)
        val batchRegisterVO = BatchRegisterVO()
        batchRegisterVO.repositoryUrl = gongfengPublicProjEntity.httpUrlToRepo
        batchRegisterVO.scmType = "GIT_URL_TYPE"
        batchRegisterVO.branch = gongfengPublicProjEntity.defaultBranch
        batchRegisterVO.userName = codeccPublicAccount
        batchRegisterVO.passWord = codeccPublicPassword
        val pipelineTaskInfoEntity = TaskInfoEntity()
        pipelineTaskInfoEntity.projectId = taskDetailVO.projectId
        pipelineTaskInfoEntity.taskId = 0L
        pipelineTaskInfoEntity.projectName = null
        taskDetailVO.pipelineId = pipelineService.assembleCreatePipeline(
            registerVO = batchRegisterVO,
            defaultExecuteTime = "",
            defaultExecuteDate = listOf(),
            userName = userName,
            relPath = "",
            taskInfoEntity = pipelineTaskInfoEntity
        )
        val nameEn = with(taskDetailVO) {
            getTaskStreamName(projectId, pipelineId, createFrom)
        }
        taskDetailVO.nameEn = nameEn
        taskDetailVO.nameCn = if (null == taskDetailVO.gongfengProjectId) {
            val suffix = MD5Utils.getMD5(nameEn).substring(0, 5)
            "GRAYTASK_$suffix"
        } else {
            "GRAYTASK_${taskDetailVO.gongfengProjectId}"
        }
        taskDetailVO.codeLang = toolLanguageMappingCache.get(originalToolName).first + 1073741824L
        val taskInfoEntity = createTask(taskDetailVO, userName)

        // 2. 根据工具名获取规则集清单
        setGrayCheckerSetsAccordingToolName(taskDetailVO, originalToolName)
        // 3.给项目安装对应的灰度规则集
        if (!taskDetailVO.checkerSetList.isNullOrEmpty()) {
            taskDetailVO.checkerSetList.forEach {
                val checkerSetRelationshipVO = CheckerSetRelationshipVO()
                checkerSetRelationshipVO.projectId = taskDetailVO.projectId
                checkerSetRelationshipVO.type = CheckerConstants.CheckerSetRelationshipType.PROJECT.name
                client.get(ServiceCheckerSetRestResource::class.java).setRelationships(
                    it.checkerSetId, userName,
                    checkerSetRelationshipVO
                )
            }
        }
        // 4.配置工具信息,注意，对于灰度逻辑，默认工具清单只有一个(需要另外再添加cloc工具)
        val toolConfigInfoVO = ToolConfigInfoVO()
        toolConfigInfoVO.taskId = taskInfoEntity.taskId
        toolConfigInfoVO.toolName = originalToolName
        val clocToolConfigInfoVO = ToolConfigInfoVO()
        clocToolConfigInfoVO.taskId = taskInfoEntity.taskId
        clocToolConfigInfoVO.toolName = ComConstants.Tool.CLOC.name
        taskDetailVO.toolConfigInfoList = listOf(toolConfigInfoVO, clocToolConfigInfoVO)
        upsert(taskDetailVO, taskInfoEntity, userName, mutableListOf(originalToolName, ComConstants.Tool.CLOC.name))
        // 更新关联的规则集
        client.get(ServiceCheckerSetRestResource::class.java).batchRelateTaskAndCheckerSet(
            userName,
            taskInfoEntity.projectId,
            taskInfoEntity.taskId,
            taskDetailVO.checkerSetList,
            false
        )
        // 5.设置强制全量标志位
        client.get(ServiceToolBuildInfoResource::class.java)
            .setForceFullScan(taskDetailVO.taskId, listOf(originalToolName, ComConstants.Tool.CLOC.name))
        return TaskIdVO(taskInfoEntity.taskId, taskInfoEntity.nameEn)
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): Boolean {
        logger.info("no need to update task!")
        return true
    }

    override fun createTask(taskDetailVO: TaskDetailVO, userName: String): TaskInfoEntity {
        taskDetailVO.status = TaskConstants.TaskStatus.ENABLE.value()
        taskDetailVO.atomCode = ComConstants.AtomCode.CODECC_V3.code()

        if (checkeIsStreamRegistered(taskDetailVO.nameEn)) {
            logger.info("task has been registered! task name: ${taskDetailVO.nameEn}")
        }
        val taskInfoEntity = TaskInfoEntity()
        BeanUtils.copyProperties(taskDetailVO, taskInfoEntity, "toolConfigInfoList")
        val currentTime = System.currentTimeMillis()
        // 工具名后面再配置
        taskInfoEntity.toolNames = null
        taskInfoEntity.createdBy = userName
        taskInfoEntity.createdDate = currentTime
        taskInfoEntity.updatedBy = userName
        taskInfoEntity.updatedDate = currentTime
        taskInfoEntity.taskMember = listOf(userName)
        taskInfoEntity.taskOwner = listOf(userName)
        taskInfoEntity.pipelineId = taskDetailVO.pipelineId

        // 更新编译平台
        taskInfoEntity.compilePlat = taskDetailVO.compilePlat
        taskInfoEntity.osType = taskDetailVO.osType
        taskInfoEntity.projectBuildType = taskDetailVO.projectBuildType
        taskInfoEntity.projectBuildCommand = taskDetailVO.projectBuildCommand

        // 生成任务id
        val taskId = redisTemplate.opsForValue().increment(RedisKeyConstants.CODECC_TASK_ID, 1L) ?: 0L
        taskInfoEntity.taskId = taskId
        taskDetailVO.taskId = taskId

        // 默认全量扫描
        if (null == taskDetailVO.scanType) {
            taskInfoEntity.scanType = ComConstants.ScanType.FULL.code
        }

        // 处理共通化路径
        pathFilterService.addDefaultFilterPaths(taskInfoEntity)
        val taskInfoResult = taskRepository.save(taskInfoEntity)

        // 缓存创建来源
        redisTemplate.opsForHash<String, String>()
            .put("$PREFIX_TASK_INFO$taskId", KEY_CREATE_FROM, taskInfoResult.createFrom)
        return taskInfoResult
    }

    /**
     * 根据工具名查询灰度规则集
     */
    private fun setGrayCheckerSetsAccordingToolName(
        taskDetailVO: TaskDetailVO,
        toolName: String
    ) {
        val languageSet = toolLanguageMappingCache.get(toolName).second
        val clocCheckerSet = CheckerSetVO()
        clocCheckerSet.checkerSetId = "standard_cloc"
        if (!languageSet.isNullOrEmpty()) {
            taskDetailVO.checkerSetList = languageSet.map {
                val checkerSetVO = CheckerSetVO()
                checkerSetVO.checkerSetId = "${toolName.toLowerCase()}_${it.toLowerCase()}_all_checkers"
                checkerSetVO
            }.plus(clocCheckerSet)
        }
    }

    /**
     * 根据工具名查询语言
     */
    private fun findLanguageSetByToolName(toolName: String): Pair<Long, Set<String>> {
        val toolMetaInfo = toolMetaRepository.findFirstByName(toolName)
        if (null == toolMetaInfo || toolMetaInfo.entityId.isNullOrBlank()) {
            return Pair(0L, setOf())
        }
        return Pair(toolMetaInfo.lang, pipelineService.convertCodeCCLangToString(toolMetaInfo.lang))
    }

    /**
     * 处理当前灰度报告
     */
    fun processGrayReport(buildId: String, taskId: Long, grayTaskStatVO: GrayTaskStatVO?) {
        logger.info("start to process gray report, build id: $buildId")
        // 1.先通过buildId查询关联的codeccBuildId
        val buildIdRelationshipEntity = buildIdRelationRepository.findFirstByBuildId(buildId)
        if (null == buildIdRelationshipEntity || buildIdRelationshipEntity.codeccBuildId.isNullOrBlank()) {
            logger.info("build id entity is null or codecc build id is null")
            return
        }
        val codeccBuildId = buildIdRelationshipEntity.codeccBuildId
        // 2. 查询任务详情
        val taskInfoEntity = taskRepository.findFirstByTaskId(taskId)
        if (null == taskInfoEntity || taskInfoEntity.taskId == 0L) {
            logger.info("task info is null")
            return
        }
        // 3. 查询灰度项目信息
        val grayToolProjectEntity = grayToolProjectRepository.findFirstByProjectId(taskInfoEntity.projectId)
        if (null == grayToolProjectEntity || grayToolProjectEntity.status != ComConstants.ToolIntegratedStatus.G.value()) {
            logger.info("gray tool project is not qualified")
            return
        }
        val defectCount =
            if (null != grayTaskStatVO) (grayTaskStatVO.totalSerious ?: 0) + (grayTaskStatVO.totalNormal
                ?: 0) + (grayTaskStatVO.totalPrompt ?: 0)
            else null
        grayToolReportDao.incrCurrentReportInfo(
            taskInfoEntity.projectId,
            codeccBuildId,
            taskId,
            defectCount,
            grayTaskStatVO?.elapsedTime,
            (null != grayTaskStatVO && grayTaskStatVO.currStep == ComConstants.Step4MutliTool.COMMIT.value() &&
                grayTaskStatVO.flag == ComConstants.StepFlag.SUCC.value())
        )
    }
}
