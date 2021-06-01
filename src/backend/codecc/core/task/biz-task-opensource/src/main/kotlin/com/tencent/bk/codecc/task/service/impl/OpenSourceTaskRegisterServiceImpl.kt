package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource
import com.tencent.bk.codecc.task.constant.OTEAM_APP_CODE
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.*
import com.tencent.bk.codecc.task.model.GongFengTriggerParamEntity
import com.tencent.bk.codecc.task.model.NewTaskRetryRecordEntity
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.CustomProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.GongfengPublicProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.NewTaskRetryRecordRepository
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService
import com.tencent.bk.codecc.task.tof.TofClientApi
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskIdVO
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.constant.CommonMessageCode.UTIL_EXECUTE_FAIL
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.MD5Utils
import net.sf.json.JSONArray
import org.apache.commons.collections.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service("openSourceTaskRegisterService")
class OpenSourceTaskRegisterServiceImpl @Autowired constructor(
    private val baseDataRepository: BaseDataRepository,
    private val customProjRepository: CustomProjRepository,
    private val newTaskRetryRecordRepository: NewTaskRetryRecordRepository,
    private val gongfengPublicProjRepository: GongfengPublicProjRepository,
    private val tofClientApi: TofClientApi,
    private val gongfengTriggerParamRepository: GongfengTriggerParamRepository
) : AbstractTaskRegisterService() {

    private val filterConfigCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, Map<String, String>>(
            object : CacheLoader<String, Map<String, String>>() {
                override fun load(paramType: String): Map<String, String> {
                    return try {
                        val filterConfigMap = getFilterConfigFromDB(paramType)
                        logger.info("paramType[$paramType] filter config map:$filterConfigMap.")
                        filterConfigMap
                    } catch (t: Throwable) {
                        logger.info("paramType[$paramType] failed to get filter config map")
                        mutableMapOf()
                    }
                }
            }
        )

    private val toolNamesGrayTestBgConfigCache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<Int>>(
            object : CacheLoader<String, List<Int>>() {
                override fun load(paramType: String): List<Int> {
                    return try {
                        val baseDataEntityList = baseDataRepository.findAllByParamType(paramType)
                        logger.info("paramType[$paramType] config info:$baseDataEntityList")
                        baseDataEntityList
                            .filter { it.paramStatus.toInt() == ComConstants.Status.ENABLE.value() }
                            .map { it.paramCode.toInt() }
                    } catch (t: Throwable) {
                        logger.info("paramType[$paramType] failed to get config list")
                        mutableListOf()
                    }
                }
            }
        )

    override fun registerTask(taskDetailVO: TaskDetailVO, userName: String): TaskIdVO {
        logger.info("start to register open scan task, pipeline id: ${taskDetailVO.pipelineId}")
        var taskInfoEntity = if (taskDetailVO.taskId > 0L) {
            taskRepository.findByTaskId(taskDetailVO.taskId)
        } else if (!taskDetailVO.pipelineId.isNullOrBlank()) {
            taskRepository.findByPipelineId(taskDetailVO.pipelineId)
        } else {
            taskRepository.findByNameEn(taskDetailVO.nameEn)
        }
        taskDetailVO.createFrom = ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()

        // 未注册
        if (null == taskInfoEntity) {
            logger.info("unregistered gongfeng task")
            val nameEn = with(taskDetailVO) {
                getTaskStreamName(projectId, pipelineId, createFrom)
            }
            taskDetailVO.nameEn = nameEn

            taskDetailVO.nameCn = if (null == taskDetailVO.gongfengProjectId) {
                val suffix = MD5Utils.getMD5(nameEn).substring(0, 5)
                "CODEPIPELINE_$suffix"
            } else {
                val gongfengPublicProjEntity = gongfengPublicProjRepository.findById(taskDetailVO.gongfengProjectId)
                if (null == gongfengPublicProjEntity) {
                    "CODEPIPELINE_${taskDetailVO.gongfengProjectId}"
                } else {
                    gongfengPublicProjEntity.name
                }
            }
            // 创建任务
            taskInfoEntity = createTask(taskDetailVO, userName)
            // 由于工蜂项目自动检测语言并配置规则集，所以需要扫描两遍才会出告警，现在将两遍的扫描放在一天做，
            // 将新注册任务放入重试表中，由重试机制来做
            insertRetry(taskInfoEntity)
            // 更新工具
            upsertTools(taskDetailVO, taskInfoEntity, userName)
        }
        // 已注册
        else {
            logger.info("the task has been registered! task id: ${taskDetailVO.taskId}, pipeline id: ${taskDetailVO.pipelineId}")
            // 更新编译信息
            taskInfoEntity.compilePlat = taskDetailVO.compilePlat
            taskInfoEntity.osType = taskDetailVO.osType
            taskInfoEntity.projectBuildType = taskDetailVO.projectBuildType
            taskInfoEntity.projectBuildCommand = taskDetailVO.projectBuildCommand
            taskRepository.save(taskInfoEntity)
        }
        return TaskIdVO(taskInfoEntity.taskId, taskInfoEntity.nameEn)
    }

    override fun updateTask(taskDetailVO: TaskDetailVO, userName: String): Boolean {
        logger.info("start to update open scan task, pipeline id: ${taskDetailVO.pipelineId}, task id: ${taskDetailVO.taskId}")
        val taskInfoEntity = if (taskDetailVO.taskId > 0L) {
            taskRepository.findByTaskId(taskDetailVO.taskId)
        } else {
            taskRepository.findByPipelineId(taskDetailVO.pipelineId)
        }

        if (null == taskInfoEntity) {
            logger.info("the task has not been registered! task id: ${taskDetailVO.taskId}, pipeline id: ${taskDetailVO.pipelineId}")
            return false
        }

        try {
            taskDetailVO.codeLang = pipelineService.convertDevopsCodeLangToCodeCCWithOthers(taskDetailVO.devopsCodeLang)
        } catch (e: StreamException) {
            logger.error("deserialize devops code lang fail! code lang info: {}", taskDetailVO)
            throw CodeCCException(UTIL_EXECUTE_FAIL)
        }
        logger.info("task detail vo code lang: ${taskDetailVO.codeLang}, task info code lang: ${taskInfoEntity.codeLang}")
        if (taskInfoEntity.codeLang == null || taskDetailVO.codeLang != taskInfoEntity.codeLang || (null != taskDetailVO.forceToUpdateOpenSource && taskDetailVO.forceToUpdateOpenSource)) {
            // 是否是首次触发扫描CLOC工具
            taskInfoEntity.gongfengFlag = false

            setCheckerSetsAccordingToLanguage(
                taskDetailVO,
                taskInfoEntity.pipelineId,
                JsonUtil.to(taskDetailVO.devopsCodeLang, object : TypeReference<List<String>>() {})
            )
            // 更新任务信息
            updateTaskInfo(taskDetailVO, taskInfoEntity, userName)
            // 更新工具
            upsertTools(taskDetailVO, taskInfoEntity, userName)
        }
        return true
    }

    override fun createTask(taskDetailVO: TaskDetailVO, userName: String): TaskInfoEntity {
        taskDetailVO.status = TaskConstants.TaskStatus.ENABLE.value()
        taskDetailVO.atomCode = ComConstants.AtomCode.CODECC_V3.code()

        if (checkeIsStreamRegistered(taskDetailVO.nameEn)) {
            logger.info("task hash been registered! task name: ${taskDetailVO.nameEn}")
        }
        val taskInfoEntity = TaskInfoEntity()
        BeanUtils.copyProperties(taskDetailVO, taskInfoEntity, "toolConfigInfoList")
        val currentTime = System.currentTimeMillis()
        taskInfoEntity.createdBy = userName
        taskInfoEntity.createdDate = currentTime
        taskInfoEntity.updatedBy = userName
        taskInfoEntity.updatedDate = currentTime
        taskInfoEntity.taskMember = listOf(userName)
        taskInfoEntity.taskOwner = listOf(userName)
        taskInfoEntity.pipelineId = taskDetailVO.pipelineId

        // 获取员工组织信息
        val staffInfo = tofClientApi.getStaffInfoByUserName(userName)
        val organizationInfo = tofClientApi.getOrganizationInfoByGroupId(staffInfo.data?.GroupId ?: -1)

        taskInfoEntity.bgId = organizationInfo?.bgId ?: -1
        taskInfoEntity.deptId = organizationInfo?.deptId ?: -1
        taskInfoEntity.centerId = organizationInfo?.centerId ?: -1
        taskInfoEntity.groupId = staffInfo.data?.GroupId ?: -1

        // 更新编译平台
        taskInfoEntity.compilePlat = taskDetailVO.compilePlat
        taskInfoEntity.osType = taskDetailVO.osType
        taskInfoEntity.projectBuildType = taskDetailVO.projectBuildType
        taskInfoEntity.projectBuildCommand = taskDetailVO.projectBuildCommand

        // 生成任务id
        val taskId = redisTemplate.opsForValue().increment(RedisKeyConstants.CODECC_TASK_ID, 1L)
        taskInfoEntity.taskId = taskId
        taskDetailVO.taskId = taskId

        // 默认全量扫描
        if (null == taskDetailVO.scanType) {
            taskInfoEntity.scanType = ComConstants.ScanType.FULL.code
        }

        // 处理共通化路径
        pathFilterService.addDefaultFilterPaths(taskInfoEntity)

        // 兼容手动触发
        val customProjEntity = if (!taskDetailVO.pipelineId.isNullOrBlank()) {
            customProjRepository.findFirstByPipelineId(taskDetailVO.pipelineId)
        } else if (taskDetailVO.taskId != 0L) {
            customProjRepository.findFirstByTaskId(taskDetailVO.taskId)
        } else {
            null
        }
        if ((null != customProjEntity && !customProjEntity.pipelineId.isNullOrBlank() && !customProjEntity.url.isNullOrBlank()) && null == taskInfoEntity.customProjInfo) {
            taskInfoEntity.customProjInfo = customProjEntity
            customProjEntity.taskId = taskId
            customProjRepository.save(customProjEntity)
        } else if (null != customProjEntity && !customProjEntity.entityId.isNullOrBlank() && customProjEntity.url.isNullOrBlank() && customProjEntity.pipelineId.isNullOrBlank()) {
            taskInfoEntity.customProjInfo = null
        } else {
            taskInfoEntity.customProjInfo = null
        }
        val taskInfoResult = taskRepository.save(taskInfoEntity)

        // 缓存创建来源
        redisTemplate.opsForHash<String, String>()
            .put("$PREFIX_TASK_INFO$taskId", KEY_CREATE_FROM, taskInfoResult.createFrom)
        return taskInfoResult
    }

    /**
     * 手动创建失效项目 todo 考虑后续打开的情况
     */
    fun registerDisabledTask(
        gongfengProjectId: Int,
        pipelineId: String?,
        projectId: String?,
        opensourceDisableReason: Int
    ): TaskInfoEntity? {
        val taskInfoEntity = taskRepository.findFirstByGongfengProjectId(gongfengProjectId)
        if (null == taskInfoEntity) {
            logger.info("start to register disabled task, gongfeng project id: $gongfengProjectId")
            val nameEn = getTaskStreamName(
                "CODE_$gongfengProjectId",
                pipelineId ?: UUIDUtil.generate(),
                ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
            )
            val disableTaskInfo = TaskInfoEntity()
            disableTaskInfo.nameEn = nameEn
            disableTaskInfo.nameCn = "CODEPIPELINE_$gongfengProjectId"
            // 创建任务
            disableTaskInfo.status = TaskConstants.TaskStatus.DISABLE.value()
            disableTaskInfo.opensourceDisableReason = opensourceDisableReason
            disableTaskInfo.atomCode = ComConstants.AtomCode.CODECC_V3.code()
            disableTaskInfo.createFrom = ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
            disableTaskInfo.codeLang = 0L

            val currentTime = System.currentTimeMillis()
            val userName = "CodeCC"
            disableTaskInfo.createdBy = userName
            disableTaskInfo.createdDate = currentTime
            disableTaskInfo.updatedBy = userName
            disableTaskInfo.updatedDate = currentTime
            disableTaskInfo.taskMember = listOf(userName)
            disableTaskInfo.taskOwner = listOf(userName)

            disableTaskInfo.gongfengProjectId = gongfengProjectId
            if (!pipelineId.isNullOrBlank()) {
                disableTaskInfo.pipelineId = pipelineId
            }
            if (!projectId.isNullOrBlank()) {
                disableTaskInfo.projectId = projectId
            }

            // 赋值组织架构
            disableTaskInfo.bgId = -1
            disableTaskInfo.deptId = -1
            disableTaskInfo.centerId = -1
            disableTaskInfo.groupId = -1

            // 生成任务id
            val taskId = redisTemplate.opsForValue().increment(RedisKeyConstants.CODECC_TASK_ID, 1L)
            disableTaskInfo.taskId = taskId

            // 默认全量扫描
            disableTaskInfo.scanType = ComConstants.ScanType.FULL.code

            // 处理共通化路径
            pathFilterService.addDefaultFilterPaths(disableTaskInfo)

            val finalDisableTaskInfo = taskRepository.save(disableTaskInfo)

            // 默认配置cloc工具
            val taskDetailVO = TaskDetailVO()
            val toolConfigInfoVO = ToolConfigInfoVO()
            toolConfigInfoVO.taskId = finalDisableTaskInfo.taskId
            toolConfigInfoVO.toolName = ComConstants.Tool.CLOC.name
            taskDetailVO.taskId = finalDisableTaskInfo.taskId
            taskDetailVO.checkerSetList = emptyList()
            upsertTools(taskDetailVO, finalDisableTaskInfo, userName)
            return finalDisableTaskInfo
        } else {
            logger.info("this disabled task has been created! gongfeng project id: $gongfengProjectId")
            return null
        }
    }

    /**
     * 根据要求切换规则集
     */
    fun switchCheckerSetType(
        pipelineId: String,
        userName: String,
        openSourceCheckerSetType: ComConstants.OpenSourceCheckerSetType
    ) {
        logger.info("start to switch open source checker set type! pipeline id: $pipelineId, checker set type: $openSourceCheckerSetType")
        val taskInfoEntity = taskRepository.findByPipelineId(pipelineId)
        if (null == taskInfoEntity || null == taskInfoEntity.codeLang || taskInfoEntity.nameEn.isNullOrBlank()) {
            logger.info("none task found with pipeline id: $pipelineId")
            return
        }
        val customProjEntity = customProjRepository.findFirstByPipelineId(pipelineId)
        // 如果是非oteam项目，则配置原有规则集
        val inputOpenSourceCheckerSetType = if (null == customProjEntity ||
            (customProjEntity.projectId != "CUSTOMPROJ_TEG_CUSTOMIZED" && customProjEntity.appCode != OTEAM_APP_CODE)) {
            setOf(openSourceCheckerSetType)
        } else {
            // 如果是oteam项目，但是没有配置ci的yml文件，则配置oteam规则集
            if (null == customProjEntity.oTeamCiProj || !customProjEntity.oTeamCiProj) {
                setOf(openSourceCheckerSetType, ComConstants.OpenSourceCheckerSetType.OTEAM)
            } else {
                // 否则，需要加上ci的规则集
                setOf(openSourceCheckerSetType, ComConstants.OpenSourceCheckerSetType.OTEAM, ComConstants.OpenSourceCheckerSetType.OTEAM_CI)
            }
        }
        val taskDetailVO = TaskDetailVO()
        taskDetailVO.taskId = taskInfoEntity.taskId
        taskDetailVO.codeLang = taskInfoEntity.codeLang
        taskDetailVO.checkerSetList =
            setCheckerSetsAccordingToCodeCCLanguage(taskInfoEntity, inputOpenSourceCheckerSetType)
        upsertTools(taskDetailVO, taskInfoEntity, userName)
    }

    /**
     * 配置客户化规则集
     */
    fun setCustomizedCheckerSet(
        pipelineId: String,
        userName: String
    ) {
        logger.info("start to set customized checker set! pipeline id: $pipelineId")
        val taskInfoEntity = taskRepository.findByPipelineId(pipelineId)
        if (null == taskInfoEntity || null == taskInfoEntity.codeLang || taskInfoEntity.nameEn.isNullOrBlank()) {
            logger.info("none task found with pipeline id: $pipelineId")
            return
        }
        val customProjEntity = customProjRepository.findFirstByPipelineId(pipelineId)
        if (null == customProjEntity || customProjEntity.checkerSetRange.isNullOrEmpty()) {
            logger.info("none custom project entity found with pipeline id: $pipelineId")
            return
        }
        val taskDetailVO = TaskDetailVO()
        taskDetailVO.taskId = taskInfoEntity.taskId
        taskDetailVO.codeLang = taskInfoEntity.codeLang
        taskDetailVO.checkerSetList = setCheckerSetsAccordingCheckerSetRange(customProjEntity.checkerSetRange)
        upsertTools(taskDetailVO, taskInfoEntity, userName)
    }

    fun getFilterConfig(): Map<String, String> {
        logger.info("start to query filter config info!")
        return filterConfigCache.get(ComConstants.KEY_FILTER_CONFIG)
    }

    private fun getFilterConfigFromDB(paramType: String): Map<String, String> {
        val baseDataEntityList = baseDataRepository.findAllByParamType(paramType)
        if (baseDataEntityList.isNullOrEmpty()) {
            return mapOf()
        }
        baseDataEntityList.sortBy { it.paramExtend1.toInt() }
        return baseDataEntityList.associate { it.paramCode to it.paramValue }
    }

    /**
     * 1. 如果没有传递规则集，则默认赋值CLOC和SCC工具
     * 2. 如果传递规则集，则按照正常规则集赋值
     */
    private fun upsertTools(
        taskDetailVO: TaskDetailVO,
        taskInfoEntity: TaskInfoEntity,
        userName: String
    ) {
        val forceFullScanTools = mutableListOf<String>()
        // 蓝盾插件创建逻辑，第一次扫描就安装规则集和工具
        val gongfengTriggerParamEntity: GongFengTriggerParamEntity? = gongfengTriggerParamRepository.findByGongfengId(taskInfoEntity.gongfengProjectId)
        if (gongfengTriggerParamEntity != null) {
            logger.info("upsert tools with bk plugin")
            val checkerSet = mutableSetOf<CheckerSetVO>()
            checkerSet.addAll(taskDetailVO.checkerSetList)
            checkerSet.addAll(gongfengTriggerParamEntity.languageRuleSetMap)
            taskDetailVO.checkerSetList = checkerSet.toList()
            taskDetailVO.codeLang = gongfengTriggerParamEntity.codeLang
        }

        if (taskDetailVO.checkerSetList.isNullOrEmpty()) {
            val toolConfigInfoVO = ToolConfigInfoVO()
            toolConfigInfoVO.taskId = taskDetailVO.taskId
            toolConfigInfoVO.toolName = ComConstants.Tool.CLOC.name
            val sccToolConfigInfoVO = ToolConfigInfoVO()
            sccToolConfigInfoVO.taskId = taskDetailVO.taskId
            sccToolConfigInfoVO.toolName = ComConstants.Tool.SCC.name
            taskDetailVO.toolConfigInfoList = listOf(toolConfigInfoVO, sccToolConfigInfoVO)
            forceFullScanTools.add(ComConstants.Tool.CLOC.name)
            forceFullScanTools.add(ComConstants.Tool.SCC.name)
            upsert(taskDetailVO, taskInfoEntity, userName, forceFullScanTools)
        } else {
            logger.info("deal with checker set")
            adaptV3AtomCodeCCForOpenScan(taskDetailVO)
            if (taskDetailVO.toolConfigInfoList.isNullOrEmpty() ||
                taskDetailVO.toolConfigInfoList.none { it.toolName == ComConstants.Tool.CLOC.name }
            ) {
                // 开源扫描的强制要带CLOC和SCC工具
                val toolConfigInfoVO = ToolConfigInfoVO()
                toolConfigInfoVO.taskId = taskDetailVO.taskId
                toolConfigInfoVO.toolName = ComConstants.Tool.CLOC.name
                val sccToolConfigInfoVO = ToolConfigInfoVO()
                sccToolConfigInfoVO.taskId = taskDetailVO.taskId
                sccToolConfigInfoVO.toolName = ComConstants.Tool.SCC.name
                if (taskDetailVO.toolConfigInfoList.isNullOrEmpty()) {
                    taskDetailVO.toolConfigInfoList = listOf(toolConfigInfoVO, sccToolConfigInfoVO)
                } else {
                    taskDetailVO.toolConfigInfoList.add(toolConfigInfoVO)
                    taskDetailVO.toolConfigInfoList.add(sccToolConfigInfoVO)
                }
            }
            upsert(taskDetailVO, taskInfoEntity, userName, forceFullScanTools)
            // 更新关联的规则集
            client.get(ServiceCheckerSetRestResource::class.java)
                .batchRelateTaskAndCheckerSet(
                    userName,
                    taskInfoEntity.projectId,
                    taskInfoEntity.taskId,
                    taskDetailVO.checkerSetList,
                    true
                )
        }

        val grayTestBgIdList = toolNamesGrayTestBgConfigCache.get(ComConstants.KEY_TOOL_NAMES_GRAY_TEST_BG)
        if (grayTestBgIdList.contains(taskInfoEntity.bgId)) {
            val toolNames = taskInfoEntity.toolConfigInfoList
                .filter { it.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value() }
                .map { it.toolName }
                .toSet()
                .joinToString(ComConstants.STRING_SPLIT)
            taskInfoEntity.toolNames = toolNames
            taskRepository.save(taskInfoEntity)
        }

        // 设置强制全量扫描标志
        if (CollectionUtils.isNotEmpty(forceFullScanTools)) {
            client.get(ServiceToolBuildInfoResource::class.java)
                .setForceFullScan(taskDetailVO.taskId, forceFullScanTools)
        }
        logger.info("create task resut: $taskInfoEntity")
    }

    private fun adaptV3AtomCodeCCForOpenScan(taskDetailVO: TaskDetailVO) {
        if (null == taskDetailVO.codeLang || taskDetailVO.codeLang <= 0L) {
            logger.info("code lang less than 0L")
            return
        }
        // 初始化规则集列表
        val result = client.get(ServiceCheckerSetRestResource::class.java)
            .queryCheckerSetsForOpenScan(taskDetailVO.checkerSetList.toSet(), taskDetailVO.projectId)
        if (result.isNotOk() || CollectionUtils.isEmpty(result.data)) {
            logger.error("query checker sets fail, result: {}", result)
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        val resultCheckerSetList = result.data
        // 要对语言进行过滤
        val finalCheckerSetList = resultCheckerSetList!!
            .filter { checkerSetVO -> null != checkerSetVO.codeLang && (checkerSetVO.codeLang and taskDetailVO.codeLang > 0L) }
        taskDetailVO.checkerSetList = finalCheckerSetList
        // 初始化工具列表
        val reqToolSet = HashSet<String>()
        taskDetailVO.checkerSetList.forEach { checkerSetVO ->
            if (CollectionUtils.isNotEmpty(checkerSetVO.toolList)) {
                reqToolSet.addAll(checkerSetVO.toolList)
            }
        }
        val toolList = ArrayList<ToolConfigInfoVO>()
        reqToolSet.forEach { toolName ->
            val toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, toolName)
            toolList.add(toolConfigInfoVO)
        }
        taskDetailVO.toolConfigInfoList = toolList
    }

    fun setCheckerSetsAccordingToLanguage(
        languages: List<String>
    ): MutableMap<String, List<CheckerSetVO>> {
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val langCheckSetMap = mutableMapOf<String, List<CheckerSetVO>>()
        var otherLanguageCheckerSet = false
        if (!languages.isNullOrEmpty()) {
            languages.forEach {
                val selectedBaseData = metaLangList.find { metaLang ->
                    val langArray = JSONArray.fromObject(metaLang.paramExtend2)
                    langArray.toList().contains(it)
                }
                val checkerSetList = mutableListOf<CheckerSetVO>()
                if (null != selectedBaseData && !selectedBaseData.openSourceCheckerSets.isNullOrEmpty()) {
                    selectedBaseData.openSourceCheckerSets.forEach checkerSet@{ checkerSet ->
                        // 默认配置全量规则集
                        logger.info("register tools: $checkerSet")
                        if (!checkerSet.checkerSetType.isNullOrBlank() && checkerSet.checkerSetType == ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name) {
                            logger.info("register tools skip: $checkerSet")
                            return@checkerSet
                        }
                        val formatCheckerSet = CheckerSetVO()
                        formatCheckerSet.checkerSetId = checkerSet.checkerSetId
                        formatCheckerSet.toolList = checkerSet.toolList
                        // 如果有配置版本，则固定用版本，如果没有配置版本，则用最新版本
                        if (null != checkerSet.version) {
                            formatCheckerSet.version = checkerSet.version
                        } else {
                            formatCheckerSet.version = Int.MAX_VALUE
                        }
                        logger.info("create task by repo ,add lang: ${formatCheckerSet.codeLang} ${selectedBaseData.paramCode.toLong()}")
                        formatCheckerSet.codeLang = selectedBaseData.paramCode.toLong()
                        checkerSetList.add(formatCheckerSet)
                    }
                    langCheckSetMap[it] = checkerSetList
                }

                if (null == selectedBaseData) {
                    if (!otherLanguageCheckerSet) {
                        val otherCheckerSetList = mutableListOf<CheckerSetVO>()
                        val otherBaseData = metaLangList.find { metaLang ->
                            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
                            langArray.toList().contains("OTHERS")
                        }
                        if (null != otherBaseData && !otherBaseData.openSourceCheckerSets.isNullOrEmpty()) {
                            otherBaseData.openSourceCheckerSets.forEach { checkerSet ->
                                val formatCheckerSet = CheckerSetVO()
                                formatCheckerSet.checkerSetId = checkerSet.checkerSetId
                                formatCheckerSet.toolList = checkerSet.toolList
                                // 如果有配置版本，则固定用版本，如果没有配置版本，则用最新版本
                                if (null != checkerSet.version) {
                                    formatCheckerSet.version = checkerSet.version
                                } else {
                                    formatCheckerSet.version = Int.MAX_VALUE
                                }
                                formatCheckerSet.codeLang = otherBaseData.paramCode.toLong()
                                otherCheckerSetList.add(formatCheckerSet)
                                otherLanguageCheckerSet = true
                            }
                            langCheckSetMap["OTHERS"] = otherCheckerSetList
                        }
                    }
                }
            }
        }
        return langCheckSetMap
    }

    private fun setCheckerSetsAccordingToLanguage(
        taskDetailVO: TaskDetailVO,
        pipelineId: String,
        languages: List<String>
    ) {
        // 如果是OTeam项目，则需要另外新增规则集
        val customProjEntity = customProjRepository.findFirstByPipelineId(pipelineId)
        val finalOpensourceCheckerSetType =
            if (null == customProjEntity || (customProjEntity.projectId != "CUSTOMPROJ_TEG_CUSTOMIZED" && customProjEntity.appCode != OTEAM_APP_CODE)) {
                if (taskDetailVO.openSourceCheckerSetType == ComConstants.OpenSourceCheckerSetType.BOTH) {
                    logger.info("both checker set type, all checker set configured")
                    // 当配置Both时，表示两种类型的规则集都要配置
                    setOf(ComConstants.OpenSourceCheckerSetType.FULL, ComConstants.OpenSourceCheckerSetType.SIMPLIFIED)
                } else {
                    // 默认配置全量规则集
                    setOf(taskDetailVO.openSourceCheckerSetType ?: ComConstants.OpenSourceCheckerSetType.FULL)
                }
            } else {
                // 如果没有配置ci的yml文件，则只配置oteam的规则集
                if (null == customProjEntity.oTeamCiProj || !customProjEntity.oTeamCiProj) {
                    logger.info("oteam checker set type, oteam checker set configured")
                    setOf(
                        taskDetailVO.openSourceCheckerSetType ?: ComConstants.OpenSourceCheckerSetType.FULL,
                        ComConstants.OpenSourceCheckerSetType.OTEAM
                    )
                } else {
                    logger.info("oteam checker set type, oteam and ci checker set configured")
                    setOf(
                        taskDetailVO.openSourceCheckerSetType ?: ComConstants.OpenSourceCheckerSetType.FULL,
                        ComConstants.OpenSourceCheckerSetType.OTEAM,
                        ComConstants.OpenSourceCheckerSetType.OTEAM_CI
                    )
                }
            }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        var otherLanguageCheckerSet = false
        val checkerSetVOList = mutableListOf<CheckerSetVO>()
        if (!languages.isNullOrEmpty()) {
            languages.forEach {
                val selectedBaseData = metaLangList.find { metaLang ->
                    val langArray = JSONArray.fromObject(metaLang.paramExtend2)
                    langArray.toList().contains(it)
                }
                /**
                 * 如果有选中的语言，并且规则集配置不为空的话，则配置相应的规则集
                 */
                if (null != selectedBaseData && !selectedBaseData.openSourceCheckerSets.isNullOrEmpty()) {
                    selectedBaseData.openSourceCheckerSets.forEach checkerSet@{ checkerSet ->
                        if (!checkerSet.checkerSetType.isNullOrBlank() && !finalOpensourceCheckerSetType.contains(
                                ComConstants.OpenSourceCheckerSetType.valueOf(checkerSet.checkerSetType)
                            )
                        ) {
                            return@checkerSet
                        }
                        val formatCheckerSet = CheckerSetVO()
                        formatCheckerSet.checkerSetId = checkerSet.checkerSetId
                        formatCheckerSet.toolList = checkerSet.toolList
                        // 如果有配置版本，则固定用版本，如果没有配置版本，则用最新版本
                        if (null != checkerSet.version) {
                            formatCheckerSet.version = checkerSet.version
                        } else {
                            formatCheckerSet.version = Int.MAX_VALUE
                        }
                        checkerSetVOList.add(formatCheckerSet)
                    }
                }
                /**
                 * 如果包含有codecc不支持的语言，则配置啄木鸟-其他规则集
                 */
                if (null == selectedBaseData) {
                    if (!otherLanguageCheckerSet) {
                        val otherBaseData = metaLangList.find { metaLang ->
                            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
                            langArray.toList().contains("OTHERS")
                        }
                        if (null != otherBaseData && !otherBaseData.openSourceCheckerSets.isNullOrEmpty()) {
                            otherBaseData.openSourceCheckerSets.forEach { checkerSet ->
                                val formatCheckerSet = CheckerSetVO()
                                formatCheckerSet.checkerSetId = checkerSet.checkerSetId
                                formatCheckerSet.toolList = checkerSet.toolList
                                // 如果有配置版本，则固定用版本，如果没有配置版本，则用最新版本
                                if (null != checkerSet.version) {
                                    formatCheckerSet.version = checkerSet.version
                                } else {
                                    formatCheckerSet.version = Int.MAX_VALUE
                                }
                                checkerSetVOList.add(formatCheckerSet)
                                otherLanguageCheckerSet = true
                            }
                            taskDetailVO.codeLang = taskDetailVO.codeLang or otherBaseData.paramCode.toLong()
                        }
                    }
                }
            }
            taskDetailVO.checkerSetList = checkerSetVOList
//            taskDetailVO.devopsToolParams = paramJsonVOList
        }
    }

    /**
     * 根据传入的规则集范围配置对应规则集
     */
    private fun setCheckerSetsAccordingCheckerSetRange(
        checkerSetList: List<OpenSourceCheckerSet>
    ): List<CheckerSetVO> {
        if (checkerSetList.isNullOrEmpty()) {
            logger.info("empty input checker set list!")
            return emptyList()
        }
        val checkerSetVOList = mutableListOf<CheckerSetVO>()
        checkerSetList.forEach {
            val formatCheckerSet = CheckerSetVO()
            formatCheckerSet.checkerSetId = it.checkerSetId
            if (null != it.version) {
                formatCheckerSet.version = it.version
            } else {
                formatCheckerSet.version = Int.MAX_VALUE
            }
            checkerSetVOList.add(formatCheckerSet)
        }
        return checkerSetVOList
    }

    private fun setCheckerSetsAccordingToCodeCCLanguage(
        taskInfoEntity: TaskInfoEntity,
        checkerSetType: Set<ComConstants.OpenSourceCheckerSetType>
    ): List<CheckerSetVO> {
        val finalCheckerSetType = if (checkerSetType.contains(ComConstants.OpenSourceCheckerSetType.BOTH)) {
            checkerSetType.plus(ComConstants.OpenSourceCheckerSetType.FULL)
                .plus(ComConstants.OpenSourceCheckerSetType.SIMPLIFIED)
        } else {
            checkerSetType
        }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val checkerSetVOList = mutableListOf<CheckerSetVO>()
        metaLangList.forEach {
            if ((it.paramCode.toLong() and (taskInfoEntity.codeLang
                    ?: 0L)) > 0L && !it.openSourceCheckerSets.isNullOrEmpty()
            ) {
                it.openSourceCheckerSets.forEach { checkerSet ->
                    if (checkerSet.checkerSetType.isNullOrBlank() || finalCheckerSetType.contains(
                            ComConstants.OpenSourceCheckerSetType.valueOf(
                                checkerSet.checkerSetType
                            )
                        )
                    ) {
                        val formatCheckerSet = CheckerSetVO()
                        formatCheckerSet.checkerSetId = checkerSet.checkerSetId
                        formatCheckerSet.toolList = checkerSet.toolList
                        if (null != checkerSet.version) {
                            formatCheckerSet.version = checkerSet.version
                        } else {
                            formatCheckerSet.version = Int.MAX_VALUE
                        }
                        checkerSetVOList.add(formatCheckerSet)
                    }
                }
            }
        }
        return checkerSetVOList
    }

    /**
     * 更新任务信息
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     */
    private fun updateTaskInfo(taskDetailVO: TaskDetailVO, taskInfoEntity: TaskInfoEntity, userName: String) {
        taskInfoEntity.status = TaskConstants.TaskStatus.ENABLE.value()
        taskInfoEntity.disableTime = ""
        taskInfoEntity.codeLang = taskDetailVO.codeLang

        taskInfoEntity.atomCode = ComConstants.AtomCode.CODECC_V3.code()
        // 更新任务中文名
        // taskInfoEntity.nameCn = taskDetailVO.nameCn
        // 更新蓝盾任务名
        taskInfoEntity.projectName = taskDetailVO.projectName
        // 更新编译平台
        /*taskInfoEntity.compilePlat = taskDetailVO.compilePlat
        taskInfoEntity.osType = taskDetailVO.osType
        taskInfoEntity.projectBuildType = taskDetailVO.projectBuildType
        taskInfoEntity.projectBuildCommand = taskDetailVO.projectBuildCommand*/
        if (taskDetailVO.gongfengProjectId != null && taskDetailVO.gongfengProjectId != 0) {
            taskInfoEntity.gongfengProjectId = taskDetailVO.gongfengProjectId
        }
        taskInfoEntity.updatedBy = userName
        taskInfoEntity.updatedDate = System.currentTimeMillis()

        val customProjEntity = if (!taskDetailVO.pipelineId.isNullOrBlank()) {
            customProjRepository.findFirstByPipelineId(taskDetailVO.pipelineId)
        } else if (taskDetailVO.taskId != 0L) {
            customProjRepository.findFirstByTaskId(taskDetailVO.taskId)
        } else {
            null
        }
        if ((null != customProjEntity && !customProjEntity.pipelineId.isNullOrBlank() && !customProjEntity.url.isNullOrBlank()) && null == taskInfoEntity.customProjInfo) {
            taskInfoEntity.customProjInfo = customProjEntity
            customProjEntity.taskId = taskInfoEntity.taskId
            customProjRepository.save(customProjEntity)
        } else if (null != customProjEntity && !customProjEntity.entityId.isNullOrBlank() && customProjEntity.url.isNullOrBlank() && customProjEntity.pipelineId.isNullOrBlank()) {
            taskInfoEntity.customProjInfo = null
        } else {
            taskInfoEntity.customProjInfo = null
        }
        taskRepository.save(taskInfoEntity)
    }

    /**
     * 插入重试表
     */
    private fun insertRetry(taskInfoEntity: TaskInfoEntity) {
        if(taskInfoEntity.projectId == "CUSTOMPROJ_PCG_RD"){
            logger.info("pcg project no need insert retry table")
            return
        }
        val currentTime = System.currentTimeMillis()
        val newTaskRetryRecordEntity = NewTaskRetryRecordEntity()
        with(taskInfoEntity) {
            newTaskRetryRecordEntity.taskId = taskId
            newTaskRetryRecordEntity.pipelineId = pipelineId
            newTaskRetryRecordEntity.projectId = projectId
            newTaskRetryRecordEntity.taskOwner =
                if (taskInfoEntity.taskOwner.isNullOrEmpty()) "CodeCC" else taskInfoEntity.taskOwner[0]
            newTaskRetryRecordEntity.retryFlag = false
            newTaskRetryRecordEntity.uploadTime = currentTime
        }
        newTaskRetryRecordRepository.save(newTaskRetryRecordEntity)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenSourceTaskRegisterServiceImpl::class.java)
    }
}
