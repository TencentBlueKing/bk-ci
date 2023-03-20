package com.tencent.devops.turbo.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.base.CaseFormat
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_NO_DATA_FOUND
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.db.PageUtils
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.util.IOUtil
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.MathUtil
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.dao.mongotemplate.TurboRecordDao
import com.tencent.devops.turbo.dao.repository.TurboRecordRepository
import com.tencent.devops.turbo.dto.TurboRecordPluginUpdateDto
import com.tencent.devops.turbo.dto.TurboRecordRefreshModel
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.model.TTurboPlanEntity
import com.tencent.devops.turbo.model.TTurboRecordEntity
import com.tencent.devops.turbo.pojo.TurboRecordModel
import com.tencent.devops.turbo.sdk.TBSSdkApi
import com.tencent.devops.turbo.vo.TurboDisplayFieldVO
import com.tencent.devops.turbo.vo.TurboRecordDisplayVO
import com.tencent.devops.turbo.vo.TurboRecordHistoryVO
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.reflect.full.memberProperties

@Suppress("MaxLineLength")
@Service
class TurboRecordService @Autowired constructor(
    private val turboRecordRepository: TurboRecordRepository,
    private val turboRecordDao: TurboRecordDao,
    private val turboEngineConfigService: TurboEngineConfigService,
    private val turboRecordSeqNumService: TurboRecordSeqNumService,
    private val eventDispatcher: SampleEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TurboRecordService::class.java)
    }

    private val recordPropertyMap = TTurboRecordEntity::class.memberProperties.associateBy { it.name }
    private val planPropertyMap = TTurboPlanEntity::class.memberProperties.associateBy { it.name }

    @Value("\${devops.rootpath}")
    private val devopRootPath: String? = null

    @Value("\${tbs.dashboard:#{null}}")
    private val tbsDashboardUrl: String? = null

    /**
     * 通过记录id查找
     */
    fun findByRecordId(turboRecordId: String): TTurboRecordEntity? {
        return turboRecordRepository.findByIdOrNull(turboRecordId)
    }

    /**
     * 插入新编译加速记录
     */
    fun insertTurboReport(
        turboDataMap: Map<String, Any?>,
        engineCode: String,
        turboPlanId: String,
        projectId: String,
        turboPlanInstanceId: String,
        spelExpression: String,
        spelParamMap: Set<String>,
        pipelineId: String? = null,
        pipelineElementId: String? = null,
        pipelineName: String? = null,
        clientIp: String? = null,
        devopsBuildId: String? = null
    ): TTurboRecordEntity? {
        val tbsRecordId = turboDataMap["task_id"] as String?
        val buildId = turboDataMap["build_id"] as String?
        val startTime = (turboDataMap["start_time"] as? Int?)?.toLong() ?: (System.currentTimeMillis() / 1000L)
        val status = (turboDataMap["status"] ?: EnumDistccTaskStatus.STAGING.getTBSStatus()) as String
        val extraMap = getExtraMap(turboDataMap)
        extraMap.putAll(turboDataMap)
        val executeNum = turboRecordSeqNumService.getLatestSeqNum(projectId)
        val turboRecordEntity = TTurboRecordEntity(
            turboPlanId = turboPlanId,
            engineCode = engineCode,
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineElementId = pipelineElementId,
            pipelineName = pipelineName,
            clientIp = clientIp,
            turboPlanInstanceId = turboPlanInstanceId,
            buildId = buildId,
            devopsBuildId = devopsBuildId,
            tbsRecordId = tbsRecordId,
            rawData = extraMap,
            executeNum = executeNum,
            status = status,
            startTime = Instant.ofEpochSecond(if (startTime == 0L) (System.currentTimeMillis() / 1000L) else startTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime(),
            createdDate = LocalDateTime.now(),
            createdBy = codeccAdmin,
            updatedDate = LocalDateTime.now(),
            updatedBy = codeccAdmin
        )
        if (status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
            val endTime = (turboDataMap["end_time"] as? Int?)?.toLong() ?: (System.currentTimeMillis() / 1000)
            turboRecordEntity.endTime = Instant.ofEpochSecond(endTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()
            turboRecordEntity.executeTimeSecond = endTime - startTime
            turboRecordEntity.executeTimeValue =
                "${(endTime - startTime) / 3600L}h ${((endTime - startTime) % 3600L) / 60L}m ${(endTime - startTime) % 60L}s"
            val context = StandardEvaluationContext()
            if (!spelParamMap.isNullOrEmpty()) {
                for (paramName in spelParamMap) {
                    context.setVariable(paramName, turboDataMap[paramName])
                }
            }
            val estimateTime = turboEngineConfigService.getSpelExpressionByCache(engineCode)?.getValue(context, Long::class.java)
            turboRecordEntity.estimateTimeSecond = estimateTime ?: 0L
            if (null != estimateTime) {
                turboRecordEntity.estimateTimeValue =
                    "${estimateTime / 3600L}h ${(estimateTime % 3600L) / 60L}m ${estimateTime % 60L}s"
            }
            turboRecordEntity.turboRatio =
                "${MathUtil.roundToTwoDigits(((estimateTime ?: 0L) - (endTime - startTime)).toDouble() * 100 / (estimateTime?.toDouble() ?: 1.0))}%"
        }
        return turboRecordRepository.save(turboRecordEntity)
    }

    /**
     * 更新编译加速记录信息
     */
    fun updateTurboReport(
        turboDataMap: Map<String, Any?>,
        tbsRecordId: String?,
        engineCode: String,
        buildId: String?,
        status: String,
        spelExpression: String,
        spelParamMap: Set<String>,
        startTime: Long
    ): TTurboRecordEntity? {
        var executeTimeSecond: Long? = null
        var executeTimeValue: String? = null
        var estimateTimeSecond: Long? = null
        var estimateTimeValue: String? = null
        var turboRatio: String? = null
        logger.info("update before finish, spel param map: $spelParamMap")
        if (status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
            val endTime = ((turboDataMap["end_time"] ?: "0") as Int).toLong()
            executeTimeSecond = endTime - startTime
            executeTimeValue =
                "${(endTime - startTime) / 3600L}h ${((endTime - startTime) % 3600L) / 60L}m ${(endTime - startTime) % 60L}s"
            val context = StandardEvaluationContext()
            if (!spelParamMap.isNullOrEmpty()) {
                for (paramName in spelParamMap) {
                    context.setVariable(paramName, turboDataMap[paramName])
                }
            }
            val estimateTime = turboEngineConfigService.getSpelExpressionByCache(engineCode)?.getValue(context, Long::class.java)
            estimateTimeSecond = estimateTime ?: 0L
            if (null != estimateTime) {
                estimateTimeValue =
                    "${estimateTime / 3600L}h ${(estimateTime % 3600L) / 60L}m ${estimateTime % 60L}s"
            }
            turboRatio =
                "${MathUtil.roundToTwoDigits(((estimateTime ?: 0L) - (endTime - startTime)).toDouble() * 100 / (estimateTime?.toDouble() ?: 1.0))}%"
        }
        logger.info("update after finish")
        val extraMap = getExtraMap(turboDataMap)
        extraMap.putAll(turboDataMap)
        return turboRecordDao.updateRecordInfo(
            tbsRecordId = tbsRecordId,
            buildId = buildId,
            turboDataMap = extraMap,
            status = status,
            executeTimeSecond = executeTimeSecond,
            executeTimeValue = executeTimeValue,
            estimateTimeSecond = estimateTimeSecond,
            estimateTimeValue = estimateTimeValue,
            turboRatio = turboRatio
        )
    }

    /**
     * 获取turbo记录中额外记录映射
     */
    private fun getExtraMap(turboDataMap: Map<String, Any?>): MutableMap<String, Any?> {
        return if (turboDataMap.containsKey("extra_record")) {
            val extraRecordString = turboDataMap["extra_record"] as String?
            if (!extraRecordString.isNullOrBlank()) {
                try {
                    JsonUtil.to(extraRecordString, object : TypeReference<MutableMap<String, Any?>>() {})
                } catch (e: Exception) {
                    mutableMapOf<String, Any?>()
                }
            } else {
                mutableMapOf<String, Any?>()
            }
        } else {
            mutableMapOf<String, Any?>()
        }
    }

    /**
     * 通过tbs记录id查询
     */
    fun existsByTBSRecordId(tbsRecordId: String): Boolean {
        return turboRecordRepository.existsByTbsRecordId(tbsRecordId)
    }

    /**
     * 通过引擎代码和状态条件查询
     */
    fun findByEngineCodeAndStatusNotIn(engineCode: String, status: Set<String>): List<TTurboRecordEntity> {
        return turboRecordRepository.findByEngineCodeAndStatusNotIn(engineCode, status)
    }

    /**
     * 通过构建id查询
     */
    fun existsByBuildId(buildId: String): Boolean {
        return turboRecordRepository.existsByBuildId(buildId)
    }

    /**
     * 获取加速历史列表
     */
    @BkTimed("api_get_turbo_record_history_page")
    fun getTurboRecordHistoryList(
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?,
        turboRecordModel: TurboRecordModel
    ): Page<TurboRecordHistoryVO> {

        val sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField ?: "execute_num")

        val turboRecordHistoryList = turboRecordDao.getTurboRecordHistoryList(
            pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType ?: "DESC"),
            turboRecordModel = turboRecordModel,
            startTime = turboRecordModel.startTime,
            endTime = turboRecordModel.endTime?.plusDays(1)
        )
        val defectStatModelList = turboRecordHistoryList.records

        if (defectStatModelList.isEmpty()) {
            return Page(0, 0, 0, listOf())
        }

        val dataList: List<TurboRecordHistoryVO> = defectStatModelList.map {
            val turboRecordHistoryVO = TurboRecordHistoryVO()
            BeanUtils.copyProperties(it, turboRecordHistoryVO)
            if (it.status == EnumDistccTaskStatus.FAILED.getTBSStatus()) {
                turboRecordHistoryVO.message = it.rawData["client_message"] as String?
            }
            turboRecordHistoryVO
        }

        return Page(
            turboRecordHistoryList.count, turboRecordHistoryList.page, turboRecordHistoryList.pageSize, turboRecordHistoryList.totalPages,
            dataList
        )
    }

    /**
     * 获取编译加速记录统计信息
     */
    fun getTurboRecordStatInfo(turboRecordId: String): String? {
        val turboRecordEntity = turboRecordRepository.findByIdOrNull(turboRecordId)
        if (null == turboRecordEntity) {
            logger.info("no turbo record info found with id: $turboRecordId")
            throw TurboException(errorCode = TURBO_NO_DATA_FOUND, errorMessage = "no turbo record found!")
        }
        if (turboRecordEntity.tbsRecordId.isNullOrBlank()) {
            return null
        }
        val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(turboRecordEntity.engineCode)
        if (null == turboEngineConfigEntity || turboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("no turbo engine info found with engine code: ${turboRecordEntity.engineCode}")
            throw TurboException(errorCode = TURBO_NO_DATA_FOUND, errorMessage = "no turbo engine config info found!")
        }
        val turboStatInfo = TBSSdkApi.queryTurboStatInfo(
            engineCode = turboRecordEntity.engineCode,
            taskId = turboRecordEntity.tbsRecordId!!
        )
        if (turboStatInfo.isNullOrEmpty()) {
            return null
        }
        return IOUtil.gzipUnCompress(Base64.getDecoder().decode(turboStatInfo[0].jobStats))
    }

    /**
     * 获取编译加速记录显示信息
     */
    @BkTimed("get_turbo_record_detail")
    @Suppress("ComplexMethod")
    fun getTurboRecordDisplayInfo(turboRecordEntity: TTurboRecordEntity, turboPlanEntity: TTurboPlanEntity): TurboRecordDisplayVO {
        val displayFields = mutableListOf(
            TurboDisplayFieldVO(
                fieldName = "任务ID",
                fieldValue = turboRecordEntity.id!!
            ),
            TurboDisplayFieldVO(
                fieldName = "方案",
                fieldValue = turboPlanEntity.planName,
                link = true,
                linkAddress = "$devopRootPath/console/turbo/${turboPlanEntity.projectId}/task/detail/${turboRecordEntity.turboPlanId!!}"
            )
        )
        val displayRange = mutableMapOf<String, Any?>()
        displayRange.putAll(turboRecordEntity.rawData)
        if (!turboPlanEntity.configParam.isNullOrEmpty()) {
            displayRange.putAll(turboPlanEntity.configParam!!)
        }
        val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(turboPlanEntity.engineCode)
        if (null == turboEngineConfigEntity) {
            logger.info("no turbo engine config found with engine code: ${turboPlanEntity.engineCode}")
            throw TurboException(errorCode = TURBO_NO_DATA_FOUND, errorMessage = "no turbo record found!")
        }
        if (!turboEngineConfigEntity.displayFields.isNullOrEmpty()) {
            val paramConfigMap = if (!turboEngineConfigEntity.paramConfig.isNullOrEmpty()) {
                turboEngineConfigEntity.paramConfig!!.associate {
                    it.paramKey to (
                        it.paramEnum?.associate { enumValue ->
                            enumValue.paramValue.toString() to enumValue.paramName
                        } ?: emptyMap()
                        )
                }
            } else {
                emptyMap()
            }
            // 根据配置的变量值拼接url
            turboEngineConfigEntity.displayFields!!.forEach {
                // 显示的值要看配置的枚举中是否包含，如果包含的话需要从枚举中取名字，显示名字
                val displayFieldValue = (paramConfigMap[it.fieldKey]?.get(displayRange[it.fieldKey].toString())) ?: displayRange[it.fieldKey]
                displayFields.add(
                    TurboDisplayFieldVO(
                        fieldName = it.fieldName,
                        fieldValue = displayFieldValue,
                        link = it.link,
                        linkAddress = if (it.link == true && !it.linkTemplate.isNullOrBlank()) {
                            it.linkVariable?.fold(it.linkTemplate!!) { acc, s ->
                                try {
                                    acc.replace("{$s}", (recordPropertyMap[s]?.call(turboRecordEntity) ?: planPropertyMap[s]?.call(turboPlanEntity.toString())).toString())
                                } catch (e: Exception) {
                                    acc
                                }
                            } ?: it.linkTemplate
                        } else {
                            null
                        }
                    )
                )
            }
        }
        if (!turboRecordEntity.pipelineName.isNullOrBlank()) {
            displayFields.add(
                TurboDisplayFieldVO(
                    fieldName = "关联的流水线",
                    fieldValue = turboRecordEntity.pipelineName,
                    link = true,
                    linkAddress = "$devopRootPath/console/pipeline/${turboPlanEntity.projectId}/${turboRecordEntity.pipelineId}/detail/${turboRecordEntity.devopsBuildId}"
                )
            )
        }
        val recordViewUrl = if (!turboRecordEntity.tbsRecordId.isNullOrBlank() && turboPlanEntity.engineCode.startsWith("disttask-") && !tbsDashboardUrl.isNullOrBlank()) {
            "$tbsDashboardUrl${turboRecordEntity.tbsRecordId}"
        } else {
            null
        }
        return TurboRecordDisplayVO(
            startTime = turboRecordEntity.startTime,
            status = turboRecordEntity.status,
            elapsedTime = turboRecordEntity.executeTimeValue,
            pipelineId = turboRecordEntity.pipelineId,
            pipelineName = turboRecordEntity.pipelineName,
            clientIp = turboRecordEntity.clientIp,
            displayFields = displayFields,
            recordViewUrl = recordViewUrl
        )
    }

    /**
     * 更新记录状态
     */
    fun updateRecordStatus(tbsRecordId: String?, buildId: String?, status: String, user: String) {
        turboRecordDao.updateRecordStatus(
            tbsRecordId = tbsRecordId,
            buildId = buildId,
            status = status,
            user = user
        )
    }

    /**
     * 插件扫描完成后调用后端接口，
     */
    fun processAfterPluginFinish(buildId: String, user: String): String? {
        val turboRecordEntity = turboRecordRepository.findByBuildId(buildId)
        if (null != turboRecordEntity) {
            /**
             * 发送延时队列，如果该记录没有同步至平台，则将状态更新至失败
             */
            eventDispatcher.dispatch(
                TurboRecordPluginUpdateDto(buildId, user, 90 * 1000)
            )
        }
        return turboRecordEntity?.id
    }

    /**
     * 用于插件更新记录状态
     */
    fun updateRecordStatusForPlugin(buildId: String, status: String, user: String) {
        turboRecordDao.updateRecordStatusForPlugin(buildId, status, user)
    }

    // ///////////////////////////以下逻辑为数据刷新逻辑，刷新后需要去除////////////////////////////////
    /**
     * 通过编译加速方案id查询
     */
    fun findTurboRecordListByTurboInstanceId(turboPlanInstanceId: String): List<TTurboRecordEntity> {
        return turboRecordDao.findByTurboPlanInstanceId(turboPlanInstanceId)
    }

    /**
     * 刷新编译加速记录信息
     */
    fun updateTurboReportForRefresh(
        turboDataMap: Map<String, Any?>,
        turboRecordId: String,
        engineCode: String,
        status: String,
        spelExpression: String,
        spelParamMap: Set<String>,
        startTime: Long
    ): TTurboRecordEntity? {
        val estimateTimeSecond: Long?
        var estimateTimeValue: String? = null
        val turboRatio: String?
        logger.info("update data for refresh, spel param map: $spelParamMap")
        return if (status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
            val endTime = ((turboDataMap["end_time"] ?: "0") as Int).toLong()
            val context = StandardEvaluationContext()
            if (!spelParamMap.isNullOrEmpty()) {
                for (paramName in spelParamMap) {
                    context.setVariable(paramName, turboDataMap[paramName])
                }
            }
            val estimateTime = turboEngineConfigService.getSpelExpressionByCache(engineCode)?.getValue(context, Long::class.java)
            estimateTimeSecond = estimateTime ?: 0L
            if (null != estimateTime) {
                estimateTimeValue =
                    "${estimateTime / 3600L}h ${(estimateTime % 3600L) / 60L}m ${estimateTime % 60L}s"
            }
            turboRatio =
                "${MathUtil.roundToTwoDigits(((estimateTime ?: 0L) - (endTime - startTime)).toDouble() * 100 / (estimateTime?.toDouble() ?: 1.0))}%"
            logger.info("update data for refresh finish")
            turboRecordDao.updateRecordForRefresh(
                turboRecordId = turboRecordId,
                estimateTimeSecond = estimateTimeSecond,
                estimateTimeValue = estimateTimeValue,
                turboRatio = turboRatio
            )
        } else {
            null
        }
    }

    /**
     * 通过项目id查询
     */
    fun findListByProjectId(projectId: String): List<TurboRecordRefreshModel>? {
        return turboRecordDao.findByProjectId(projectId)
    }
}
