package com.tencent.devops.turbo.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_PARAM_INVALID
import com.tencent.devops.common.db.PageUtils
import com.tencent.devops.common.util.enums.ConfigParamType
import com.tencent.devops.turbo.dao.mongotemplate.TurboEngineConfigDao
import com.tencent.devops.turbo.dao.repository.TurboEngineConfigRepository
import com.tencent.devops.turbo.job.TBSCreateDataJob
import com.tencent.devops.turbo.job.TBSUpdateDataJob
import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import com.tencent.devops.turbo.model.pojo.DisplayFieldEntity
import com.tencent.devops.turbo.model.pojo.ParamConfigEntity
import com.tencent.devops.turbo.model.pojo.ParamEnumEntity
import com.tencent.devops.turbo.pojo.ParamConfigModel
import com.tencent.devops.turbo.pojo.ParamEnumModel
import com.tencent.devops.turbo.pojo.ParamEnumSimpleModel
import com.tencent.devops.turbo.pojo.TurboDisplayFieldModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigModel
import com.tencent.devops.turbo.pojo.TurboEngineConfigPriorityModel
import com.tencent.devops.turbo.sdk.TBSSdkApi
import com.tencent.devops.turbo.vo.TurboEngineConfigVO
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Suppress("MaxLineLength")
@Service
class TurboEngineConfigService @Autowired constructor(
    private val turboEngineConfigRepository: TurboEngineConfigRepository,
    private val turboEngineConfigDao: TurboEngineConfigDao,
    private val scheduler: Scheduler
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TurboEngineConfigService::class.java)
        private const val triggerGroup = "turboTriggerGroup"
        private const val jobGroup = "turboJobGroup"
        private const val cronExpressionSubfix = " * * * * ?"
    }

    private val spelExpressionCache = Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build<String, Expression> { engineCode ->
            getSpelExpression(
                engineCode
            )
        }

    /**
     * 通过引擎代码查询配置信息
     */
    fun findEngineConfigByEngineCode(engineCode: String): TTurboEngineConfigEntity? {
        return turboEngineConfigRepository.findByEngineCode(engineCode)
    }

    /**
     * 新增引擎配置信息
     */
    fun addNewEngineConfig(
        turboEngineConfigModel: TurboEngineConfigModel,
        user: String
    ): Long? {
        logger.info("add new engine config, engine info: $turboEngineConfigModel")
        val preTTurboEngineConfigEntity =
            turboEngineConfigRepository.findByEngineCode(turboEngineConfigModel.engineCode!!)
        if (null != preTTurboEngineConfigEntity && !preTTurboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("turbo engine config with code ${turboEngineConfigModel.engineCode} has already exists!")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "engine code exists!")
        }
        val testValue = validateSpelExpression(
            engineCode = turboEngineConfigModel.engineCode!!,
            spelExpression = turboEngineConfigModel.spelExpression!!,
            spelParamMap = turboEngineConfigModel.spelParamMap
        )
        // 查询原有最大排序号，赋值+1
        val maxTurboEngineCodeEntity = turboEngineConfigDao.findEngineConfigWithMaxPriorityNum()
        // 保存编译加速模式信息
        with(turboEngineConfigModel) {
            val createCronSecond = addCreateScheduledJob(this)
            val turboEngineConfigEntity = TTurboEngineConfigEntity(
                engineCode = engineCode!!,
                engineName = engineName!!,
                desc = desc ?: "",
                spelExpression = spelExpression!!,
                spelParamMap = spelParamMap ?: mapOf(),
                priorityNum = (((maxTurboEngineCodeEntity?.priorityNum) ?: 0) + 1),
                paramConfig = paramConfig?.map {
                    ParamConfigEntity(
                        paramKey = it.paramKey,
                        paramName = it.paramName,
                        paramType = it.paramType,
                        paramProps = it.paramProps,
                        paramEnum = it.paramEnum?.map { paramEnumEntity ->
                            ParamEnumEntity(
                                paramValue = paramEnumEntity.paramValue,
                                paramName = paramEnumEntity.paramName,
                                visualRange = paramEnumEntity.visualRange
                            )
                        },
                        tips = it.tips,
                        displayed = it.displayed,
                        defaultValue = it.defaultValue,
                        required = it.required
                    )
                },
                createCronExpression = "$createCronSecond$cronExpressionSubfix",
                updateCronExpression = "${createCronSecond + 30}$cronExpressionSubfix",
                docUrl = docUrl,
                userManual = userManual,
                displayFields = displayFields!!.map {
                    DisplayFieldEntity(
                        fieldKey = it.fieldKey,
                        fieldName = it.fieldName,
                        link = it.link,
                        linkTemplate = it.linkTemplate,
                        linkVariable = it.linkVariable
                    )
                },
                recommend = recommend,
                recommendReason = recommendReason,
                pluginTips = pluginTips,
                updatedBy = user,
                updatedDate = LocalDateTime.now(),
                createdBy = user,
                createdDate = LocalDateTime.now()
            )
            turboEngineConfigRepository.save(turboEngineConfigEntity)
        }
        logger.info("finish create turbo engine config info!")
        return testValue
    }

    /**
     * 更新引擎配置信息
     */
    fun updateEngineConfig(engineCode: String, turboEngineConfigModel: TurboEngineConfigModel, user: String): Long? {
        val testValue = validateSpelExpression(
            engineCode = engineCode,
            spelExpression = turboEngineConfigModel.spelExpression!!,
            spelParamMap = turboEngineConfigModel.spelParamMap
        )
        with(turboEngineConfigModel) {
            turboEngineConfigDao.updateConfigInfo(
                engineCode = engineCode,
                engineName = engineName!!,
                desc = desc,
                spelExpression = spelExpression!!,
                spelParamMap = spelParamMap,
                userManual = userManual,
                docUrl = docUrl,
                recommend = recommend,
                recommendReason = recommendReason,
                pluginTips = pluginTips,
                paramConfig = paramConfig?.map {
                    ParamConfigEntity(
                        paramKey = it.paramKey,
                        paramName = it.paramName,
                        paramType = it.paramType,
                        paramProps = it.paramProps,
                        paramEnum = it.paramEnum?.map { paramEnumEntity ->
                            ParamEnumEntity(
                                paramValue = paramEnumEntity.paramValue,
                                paramName = paramEnumEntity.paramName,
                                visualRange = paramEnumEntity.visualRange
                            )
                        },
                        tips = it.tips,
                        displayed = it.displayed,
                        defaultValue = it.defaultValue,
                        required = it.required,
                        dataType = it.dataType,
                        paramUrl = it.paramUrl
                    )
                },
                displayFields = displayFields!!.map {
                    DisplayFieldEntity(
                        fieldKey = it.fieldKey,
                        fieldName = it.fieldName,
                        link = it.link,
                        linkTemplate = it.linkTemplate,
                        linkVariable = it.linkVariable
                    )
                },
                user = user
            )
        }
        return testValue
    }

    /**
     * 校验spel表达式的有效性
     */
    private fun validateSpelExpression(engineCode: String, spelExpression: String, spelParamMap: Map<String, Any?>?): Long? {
        val parser = SpelExpressionParser()
        val testValue: Long?
        // 校验spel表达式是否规范
        try {
            val context = StandardEvaluationContext()
            if (!spelParamMap.isNullOrEmpty()) {
                for (paramMap in spelParamMap) {
                    context.setVariable(paramMap.key, paramMap.value)
                }
            }
            val expression = parser.parseExpression(spelExpression)
            testValue = expression.getValue(context, Long::class.java)
            logger.info("test value is $testValue")
            spelExpressionCache.put(engineCode, expression)
            logger.info("validate spel expression success!")
        } catch (e: Exception) {
            logger.info("validate spel expression failed!")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "validate spel expression failed!")
        }
        return testValue
    }

    /**
     * 通过引擎代码查询引擎详情
     */
    fun queryEngineConfigInfo(engineCode: String): TurboEngineConfigVO {
        val turboEngineConfigEntity = turboEngineConfigRepository.findByEngineCode(engineCode)
        if (null == turboEngineConfigEntity) {
            logger.info("no turbo engine config info found")
            throw TurboException(TURBO_PARAM_INVALID, "no turbo engine config found")
        }
        return with(turboEngineConfigEntity) {
            TurboEngineConfigVO(
                engineCode = engineCode,
                engineName = engineName,
                priorityNum = priorityNum,
                desc = desc,
                spelExpression = spelExpression,
                spelParamMap = spelParamMap,
                enabled = enabled,
                userManual = userManual,
                docUrl = docUrl,
                recommend = recommend,
                recommendReason = recommendReason,
                paramConfig = paramConfig?.map {
                    ParamConfigModel(
                        paramKey = it.paramKey,
                        paramName = it.paramName,
                        paramType = it.paramType,
                        paramProps = it.paramProps,
                        paramEnum = it.paramEnum?.map { paramEnumEntity ->
                            ParamEnumModel(
                                paramValue = paramEnumEntity.paramValue,
                                paramName = paramEnumEntity.paramName,
                                visualRange = paramEnumEntity.visualRange
                            )
                        },
                        tips = it.tips,
                        displayed = it.displayed,
                        defaultValue = it.defaultValue,
                        required = it.required,
                        dataType = it.dataType,
                        paramUrl = it.paramUrl
                    )
                },
                displayFields = displayFields?.map {
                    TurboDisplayFieldModel(
                        fieldKey = it.fieldKey,
                        fieldName = it.fieldName,
                        link = it.link,
                        linkTemplate = it.linkTemplate,
                        linkVariable = it.linkVariable
                    )
                },
                pluginTips = pluginTips,
                updatedBy = updatedBy,
                updatedDate = updatedDate
            )
        }
    }

    /**
     * 查询推荐编译加速模式
     */
    fun queryRecommendEngineConfig(): List<TurboEngineConfigVO> {
        val turboEngineConfigList = turboEngineConfigRepository.findByRecommendAndEnabled(
            recommend = true,
            enabled = true
        )
        if (turboEngineConfigList.isNullOrEmpty()) {
            logger.info("no recommend turbo engine config found!")
            return listOf()
        }
        return turboEngineConfigList.map { turboEnginConfigEntity ->
            with(turboEnginConfigEntity) {
                TurboEngineConfigVO(
                    engineCode = engineCode,
                    engineName = engineName,
                    priorityNum = priorityNum,
                    desc = desc,
                    spelExpression = spelExpression,
                    spelParamMap = spelParamMap,
                    enabled = enabled,
                    userManual = userManual,
                    docUrl = docUrl,
                    recommend = recommend,
                    recommendReason = recommendReason,
                    pluginTips = pluginTips,
                    paramConfig = paramConfig?.map {
                        ParamConfigModel(
                            paramKey = it.paramKey,
                            paramName = it.paramName,
                            paramType = it.paramType,
                            paramProps = it.paramProps,
                            paramEnum = it.paramEnum?.map { paramEnumEntity ->
                                ParamEnumModel(
                                    paramValue = paramEnumEntity.paramValue,
                                    paramName = paramEnumEntity.paramName,
                                    visualRange = paramEnumEntity.visualRange
                                )
                            },
                            tips = it.tips,
                            displayed = it.displayed,
                            defaultValue = it.defaultValue,
                            required = it.required
                        )
                    },
                    displayFields = displayFields?.map {
                        TurboDisplayFieldModel(
                            fieldKey = it.fieldKey,
                            fieldName = it.fieldName,
                            link = it.link,
                            linkTemplate = it.linkTemplate,
                            linkVariable = it.linkVariable
                        )
                    },
                    updatedBy = updatedBy,
                    updatedDate = updatedDate
                )
            }
        }
    }

    /**
     * 恢复编译加速模式
     */
    fun resumeEngineConfig(engineCode: String, user: String) {
        logger.info("resume engine config, engine code: $engineCode")
        // 1.更新编译加速模式状态
        turboEngineConfigDao.updateConfigStatus(engineCode, true, user)
        // 2. 恢复定时任务
        resumeScheduledJob(engineCode)
    }

    /**
     * 失效编译加速模式
     */
    fun disableEngineConfig(engineCode: String, user: String) {
        logger.info("disable engine config, engine code: $engineCode")
        // 1.更新编译加速模式状态
        turboEngineConfigDao.updateConfigStatus(engineCode, false, user)
        // 2. 暂停定时任务
        pauseScheduledJob(engineCode)
    }

    /**
     * 删除编译加速模式
     */
    fun deleteEngineConfig(engineCode: String, user: String) {
        logger.info("delete engine config, engine code: $engineCode, user: $user")
        // 1. 删除编译加速模式
        turboEngineConfigRepository.removeByEngineCode(engineCode)
        // 2. 删除定时任务
        deleteScheduledJob(engineCode)
    }

    /**
     * 恢复对应模式的定时任务
     */
    private fun resumeScheduledJob(engineCode: String) {
        commonResumeScheduleJob(engineCode, "create")
        commonResumeScheduleJob(engineCode, "update")
    }

    /**
     * 停用对应模式的定时任务
     */
    private fun pauseScheduledJob(engineCode: String) {
        commonPauseScheduleJob(engineCode, "create")
        commonPauseScheduleJob(engineCode, "update")
    }

    /**
     * 删除对应模式的定时任务
     */
    private fun deleteScheduledJob(engineCode: String) {
        commonDeleteScheduleJob(engineCode, "create")
        commonDeleteScheduleJob(engineCode, "update")
    }

    /**
     * 添加更新编译加速记录的定时任务
     */
    private fun addUpdateScheduledJob(
        turboEngineConfigModel: TurboEngineConfigModel,
        createCronSecond: Int
    ) {
        val updateCronSecond = createCronSecond + 15
        commonScheduledJob(
            turboEngineConfigModel = turboEngineConfigModel,
            scheduleSecond = updateCronSecond,
            scheduleFlag = "update"
        )
    }

    /**
     * 添加创建编译加速记录的定时任务
     */
    private fun addCreateScheduledJob(turboEngineConfigModel: TurboEngineConfigModel): Int {
        val createCronSecond = (0..14).random()
        commonScheduledJob(
            turboEngineConfigModel = turboEngineConfigModel,
            scheduleSecond = createCronSecond,
            scheduleFlag = "create"
        )
        addUpdateScheduledJob(turboEngineConfigModel, createCronSecond)
        return createCronSecond
    }

    /**
     * 通用添加定时任务方法
     */
    private fun commonScheduledJob(
        turboEngineConfigModel: TurboEngineConfigModel,
        scheduleSecond: Int,
        scheduleFlag: String
    ) {
        val triggerName = "${turboEngineConfigModel.engineCode}_$scheduleFlag"
        val jobName = "${turboEngineConfigModel.engineCode}_$scheduleFlag"
        var trigger = TriggerBuilder.newTrigger().withIdentity(
            triggerName,
            triggerGroup
        ).withSchedule(
            CronScheduleBuilder.cronSchedule("$scheduleSecond$cronExpressionSubfix")
                .withMisfireHandlingInstructionDoNothing()
        ).build()
        val jobKey = JobKey.jobKey(jobName, jobGroup)
        val jobDataMap = mapOf(
            "engineCode" to turboEngineConfigModel.engineCode
        )
        val jobDetail =
            JobBuilder.newJob(if (scheduleFlag == "create") TBSCreateDataJob::class.java else TBSUpdateDataJob::class.java)
                .withIdentity(jobKey)
                .usingJobData(JobDataMap(jobDataMap)).build()
        val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
        try {
            scheduler.scheduleJob(jobDetail, trigger)
        } catch (e: SchedulerException) {
            logger.info("schedule $scheduleFlag job fail with scheduler exception!")
            trigger = trigger.triggerBuilder.withIdentity(triggerKey).withSchedule(
                CronScheduleBuilder.cronSchedule("$scheduleSecond$cronExpressionSubfix")
            ).build()
            scheduler.rescheduleJob(triggerKey, trigger)
        } catch (e: Exception) {
            logger.info("schedule $scheduleFlag job fail with exception!")
            scheduler.deleteJob(jobKey)
            throw TurboException(errorMessage = "add schedule fail!")
        }
    }

    /**
     * 停用定时任务通用方法
     */
    private fun commonPauseScheduleJob(
        engineCode: String,
        scheduleFlag: String
    ) {
        val triggerName = "${engineCode}_$scheduleFlag"
        val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
        try {
            scheduler.pauseTrigger(triggerKey)
        } catch (e: Exception) {
            logger.info("pause scheduler trigger fail! engine code: $engineCode")
            throw TurboException(errorMessage = "pause schedule fail!")
        }
    }

    /**
     * 通用删除定时任务方法
     */
    private fun commonDeleteScheduleJob(
        engineCode: String,
        scheduleFlag: String
    ) {
        val triggerName = "${engineCode}_$scheduleFlag"
        val jobName = "${engineCode}_$scheduleFlag"
        val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
        val jobKey = JobKey.jobKey(jobName, jobGroup)
        try {
            scheduler.pauseTrigger(triggerKey)
            scheduler.unscheduleJob(triggerKey)
            scheduler.deleteJob(jobKey)
        } catch (e: Exception) {
            logger.info("pause scheduler trigger fail! engine code: $engineCode")
            throw TurboException(errorMessage = "pause schedule fail!")
        }
    }

    /**
     * 重启定时任务通用方法
     */
    private fun commonResumeScheduleJob(
        engineCode: String,
        scheduleFlag: String
    ) {
        val triggerName = "${engineCode}_$scheduleFlag"
        val triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup)
        try {
            scheduler.resumeTrigger(triggerKey)
        } catch (e: Exception) {
            logger.info("pause scheduler trigger fail! engine code: $engineCode")
            throw TurboException(errorMessage = "resume schedule fail!")
        }
    }

    /**
     * 获取编译加速模式清单
     */
    fun getEngineConfigList(projectId: String): List<TurboEngineConfigVO> {
        val pageable = PageUtils.convertPageWithMultiFieldsAndDirection(null, null, mapOf("recommend" to "DESC", "priority_num" to "ASC"))
        val turboEngineConfigList = turboEngineConfigRepository.findByEnabled(true, pageable)
        return turboEngineConfigList.map {
            TurboEngineConfigVO(
                engineCode = it.engineCode,
                engineName = it.engineName,
                priorityNum = it.priorityNum,
                userManual = it.userManual,
                desc = it.desc,
                paramConfig = it.paramConfig?.filter { param -> param.displayed }?.map { param ->
                    ParamConfigModel(
                        paramKey = param.paramKey,
                        paramName = param.paramName,
                        paramType = param.paramType,
                        paramProps = param.paramProps,
                        paramEnum = param.paramEnum?.filter { paramEnumEntity ->
                            paramEnumEntity.visualRange.isNullOrEmpty() ||
                                paramEnumEntity.visualRange.contains(projectId)
                        }?.map { paramEnumEntity ->
                            ParamEnumModel(
                                paramValue = paramEnumEntity.paramValue,
                                paramName = paramEnumEntity.paramName,
                                visualRange = paramEnumEntity.visualRange
                            )
                        },
                        tips = param.tips,
                        displayed = param.displayed,
                        defaultValue = param.defaultValue,
                        required = param.required,
                        dataType = param.dataType,
                        paramUrl = param.paramUrl
                    )
                },
                recommend = it.recommend,
                recommendReason = it.recommendReason,
                pluginTips = it.pluginTips,
                updatedBy = it.updatedBy,
                updatedDate = it.updatedDate
            )
        }
    }

    /**
     * 获取所有加速模式清单
     */
    fun getAllEngineConfigList(): List<TurboEngineConfigVO> {
        val findAll = turboEngineConfigRepository.findAll()
        return findAll.map {
            TurboEngineConfigVO(
                engineCode = it.engineCode,
                engineName = it.engineName,
                priorityNum = it.priorityNum,
                desc = it.desc,
                spelExpression = it.spelExpression,
                spelParamMap = it.spelParamMap,
                paramConfig = null,
                enabled = it.enabled,
                userManual = it.userManual,
                docUrl = it.docUrl,
                recommend = it.recommend,
                recommendReason = it.recommendReason,
                pluginTips = it.pluginTips,
                updatedBy = it.updatedBy,
                updatedDate = it.updatedDate
            )
        }
    }

    /**
     * 获取spel缓存表达式
     */
    fun getSpelExpressionByCache(engineCode: String): Expression? {
        return spelExpressionCache.get(engineCode)
    }

    /**
     * 批量更新编译加速模式优先级
     */
    fun updateEngineConfigPriority(turboPriorityList: List<TurboEngineConfigPriorityModel>, user: String) {
        logger.info("update engine config priority map")
        val writeResult = turboEngineConfigDao.batchUpdatePriorityNum(turboPriorityList, user)
        if (writeResult.wasAcknowledged()) {
            logger.info("write result success!")
        }
    }

    /**
     * 添加worker version逻辑
     */
    fun addWorkerVersion(engineCode: String, paramKey: String, paramEnum: ParamEnumModel): Boolean {
        logger.info("add new worker version")
        val turboEngineConfigEntity = turboEngineConfigRepository.findByEngineCode(engineCode)
        if (null == turboEngineConfigEntity || turboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("no turbo engine config found with engine code: $engineCode")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config found")
        }
        val paramConfig = turboEngineConfigEntity.paramConfig?.findLast { it.paramKey == paramKey }
        if (null == paramConfig) {
            logger.info("no param config with engine code: $engineCode, param key: $paramKey")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config param found")
        }
        if (paramConfig.paramType != ConfigParamType.SELECT) {
            logger.info("param type not equal to SELECT")
            return false
        }
        if (paramConfig.paramEnum.isNullOrEmpty()) {
            paramConfig.paramEnum = listOf()
        }
        val tempParamEnum = paramConfig.paramEnum!!.toMutableList()
        tempParamEnum.add(
            with(paramEnum) {
                ParamEnumEntity(
                    paramValue = paramValue,
                    paramName = paramName,
                    visualRange = visualRange
                )
            }
        )
        paramConfig.paramEnum = tempParamEnum
        turboEngineConfigRepository.save(turboEngineConfigEntity)
        return true
    }

    /**
     * 删除worker version逻辑
     */
    fun deleteWorkerVersion(engineCode: String, paramKey: String, enumParamValue: String): Boolean {
        logger.info("delete new worker version")
        val turboEngineConfigEntity = turboEngineConfigRepository.findByEngineCode(engineCode)
        if (null == turboEngineConfigEntity || turboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("no turbo engine config found with engine code: $engineCode")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config found")
        }
        val paramConfig = turboEngineConfigEntity.paramConfig?.findLast { it.paramKey == paramKey }
        if (null == paramConfig) {
            logger.info("no param config with engine code: $engineCode, param key: $paramKey")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config param found")
        }
        if (paramConfig.paramType != ConfigParamType.SELECT) {
            logger.info("param type not equal to SELECT")
            return false
        }
        if (paramConfig.paramEnum.isNullOrEmpty()) {
            paramConfig.paramEnum = listOf()
        }
        val tempParamEnum = paramConfig.paramEnum!!.toMutableList()
        tempParamEnum.removeIf { it.paramValue == enumParamValue }
        paramConfig.paramEnum = tempParamEnum
        turboEngineConfigRepository.save(turboEngineConfigEntity)
        return true
    }

    /**
     * 更新worker version逻辑
     */
    fun updateWorkerVersion(engineCode: String, paramKey: String, enumParamValue: String, paramEnum: ParamEnumSimpleModel): Boolean {
        logger.info("update new worker version")
        val turboEngineConfigEntity = turboEngineConfigRepository.findByEngineCode(engineCode)
        if (null == turboEngineConfigEntity || turboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("no turbo engine config found with engine code: $engineCode")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config found")
        }
        val paramConfig = turboEngineConfigEntity.paramConfig?.findLast { it.paramKey == paramKey }
        if (null == paramConfig) {
            logger.info("no param config with engine code: $engineCode, param key: $paramKey")
            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "no turbo engine config param found")
        }
        if (paramConfig.paramType != ConfigParamType.SELECT) {
            logger.info("param type not equal to SELECT")
            return false
        }
        if (paramConfig.paramEnum.isNullOrEmpty()) {
            paramConfig.paramEnum = listOf()
        }
        val tempParamEnum = paramConfig.paramEnum!!.toMutableList()
        tempParamEnum.replaceAll {
            if (it.paramValue == enumParamValue)
                with(paramEnum) {
                    ParamEnumEntity(
                        paramValue = enumParamValue,
                        paramName = paramName,
                        visualRange = visualRange
                    )
                }
            else it
        }
        paramConfig.paramEnum = tempParamEnum
        turboEngineConfigRepository.save(turboEngineConfigEntity)
        return true
    }

    /**
     * spel表达式缓存方法
     */
    private fun getSpelExpression(engineCode: String): Expression? {
        val turboEngineConfigEntity = turboEngineConfigRepository.findByEngineCode(engineCode)
        return if (null == turboEngineConfigEntity || turboEngineConfigEntity.spelExpression.isBlank()) {
            null
        } else {
            val parser = SpelExpressionParser()
            parser.parseExpression(turboEngineConfigEntity.spelExpression)
        }
    }

    /**
     * 根据区域队列名获取对应的编译器版本清单
     */
    fun getCompilerVersionListByQueueName(
        engineCode: String,
        projectId: String,
        queueName: String?
    ): List<ParamEnumModel> {
        logger.info("request param[$engineCode | $projectId | $queueName]")
        if (queueName.isNullOrEmpty()) {
            logger.info("getCompilerVersionList queueName is invalid")
            return listOf()
        }

        val queryTurboCompileList = TBSSdkApi.queryTurboCompileList(
            engineCode = engineCode,
            queryParam = mapOf(
                "queue_name" to queueName
            )
        )
        logger.info("queryTurboCompileList: ${queryTurboCompileList.size}")
        return queryTurboCompileList
            .filter {
                it.visualRange.isEmpty() || it.visualRange.contains(projectId)
            }
            .map {
                ParamEnumModel(
                    paramValue = it.paramValue,
                    paramName = it.paramName,
                    visualRange = it.visualRange
                )
            }
    }
}
