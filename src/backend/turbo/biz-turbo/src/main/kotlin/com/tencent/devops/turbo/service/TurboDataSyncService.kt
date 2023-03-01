package com.tencent.devops.turbo.service

import com.tencent.devops.common.util.constants.EXCHANGE_TURBO_WORK_STATS
import com.tencent.devops.common.util.constants.ROUTE_TURBO_WORK_STATS
import com.tencent.devops.common.web.mq.CORE_RABBIT_TEMPLATE_NAME
import com.tencent.devops.turbo.dto.TurboRecordCreateDto
import com.tencent.devops.turbo.dto.TurboRecordUpdateDto
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 依赖流水线和在构建机执行命令的数据同步流程区别：
 * (1)依赖流水线同步流程
 *    1. 执行时插件调用turbo接口创建记录，并且生成build_id（不能用流水线构建id，因为有可能一条流水线多个插件）,并且已知build_id和turbo_instance_id的关联关系
 *    2. 在更新记录定时任务时，查询出不为已完成的记录，已该记录的build_id为条件从TBS服务查询出记录详情
 *    3. 将详情信息以build_id为条件，更新至记录表，并计算统计信息
 * (2)在机器直接执行命令流程
 *    1. 执行命令时对于turbo后端服务无感
 *    2. 在创建记录定时任务时，同步过去一段时间创建的加速记录，并且通过tbsRecordId查询到在记录表中该记录不存在
 *    3. 以加速方案id和ip为条件，查询加速方案实例表，如果没有记录则创建一条加速方案实例，如果有记录则取出加速方案实例id
 *    4. 将实例id赋值给同步的记录详情，更新至详情表
 */
@Suppress("ComplexMethod","NestedBlockDepth","ComplexCondition","MaxLineLength")
@Service
class TurboDataSyncService @Autowired constructor(
    @Qualifier(CORE_RABBIT_TEMPLATE_NAME)
    private val rabbitTemplate: RabbitTemplate,
    private val turboPlanService: TurboPlanService,
    private val turboPlanInstanceService: TurboPlanInstanceService,
    private val turboRecordService: TurboRecordService,
    private val turboSummaryService: TurboSummaryService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TurboDataSyncService::class.java)
    }

    /**
     * 创建编译加速记录
     */
    fun createTurboRecord(
        turboRecordCreateDto: TurboRecordCreateDto,
        turboEngineConfig: TTurboEngineConfigEntity
    ) {
        /**
         * 创建加速记录（主要为构建机执行命令的编译加速配置的定时任务）
         */
        // 1.获取对应加速方案实例id
        val turboPlanId = (turboRecordCreateDto.dataMap["project_id"] as String?)?.substringBefore("_")
        val clientIp = turboRecordCreateDto.dataMap["client_ip"] as String?
        val tbsRecordId = turboRecordCreateDto.dataMap["task_id"] as String?
        val buildId = turboRecordCreateDto.dataMap["build_id"] as String?
        logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] create param-> client ip: $clientIp | tbs record id: $tbsRecordId")
        if (turboPlanId.isNullOrBlank() || clientIp.isNullOrBlank() || tbsRecordId.isNullOrBlank()) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] turbo plan id or client ip is null, client ip: $clientIp, tbs record id: $tbsRecordId")
            return
        }
        if (turboRecordService.existsByTBSRecordId(tbsRecordId)) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] turbo record with tbs record id $tbsRecordId already exists!")
            return
        }
        if (!buildId.isNullOrBlank() && turboRecordService.existsByBuildId(buildId)) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] turbo record with build id $buildId already exists!")
            return
        }

        val turboPlanEntity = turboPlanService.findTurboPlanById(turboPlanId)
        if (turboPlanEntity?.openStatus == null || !turboPlanEntity.openStatus!!) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] turbo plan not found")
            return
        }

        /**
         * 这里有几类场景：
         * 1.需要插入实例的情况有 ->
         *   (1) 新版本数据
         *   (2) 老版本数据但是不关联流水线
         * 2.不需要插入实例的情况有 ->
         *   (1) 老版本数据并且关联流水线
         */
        val turboPlanInstanceInfo = if (null == turboPlanEntity.migrated || !turboPlanEntity.migrated!! ||
            (turboPlanEntity.migrated!! && (null != turboPlanEntity.pipelineRelated && !turboPlanEntity.pipelineRelated!!))
        ) {
            turboPlanInstanceService.upsertInstanceByPlanIdAndIp(
                turboPlanId = turboPlanId,
                projectId = turboPlanEntity.projectId,
                clientIp = clientIp
            )
        } else {
            turboPlanInstanceService.findFirstByTurboPlanId(turboPlanId)
        }
        val turboPlanInstanceEntity = turboPlanInstanceInfo.first
        if (null == turboPlanInstanceEntity || turboPlanInstanceEntity.id.isNullOrBlank()) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] no turbo plan instance found")
            return
        }
        if (turboPlanInstanceInfo.second) {
            turboPlanService.addTurboPlanInstance(turboPlanId)
        }
        // 2.插入编译加速记录表
        val turboRecordEntity = turboRecordService.insertTurboReport(
            turboDataMap = turboRecordCreateDto.dataMap,
            engineCode = turboEngineConfig.engineCode,
            turboPlanId = turboPlanId,
            projectId = turboPlanEntity.projectId,
            turboPlanInstanceId = turboPlanInstanceEntity.id!!,
            spelExpression = turboEngineConfig.spelExpression,
            spelParamMap = turboEngineConfig.spelParamMap.keys,
            clientIp = clientIp,
            pipelineId = turboPlanInstanceEntity.pipelineId,
            pipelineName = turboPlanInstanceEntity.pipelineName,
            pipelineElementId = turboPlanInstanceEntity.pipelineElementId
        )

        // 这里判断report状态status字段,finish才去查询统计信息,用于统计数据落地
        if (turboRecordEntity?.status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
            rabbitTemplate.convertAndSend(
                EXCHANGE_TURBO_WORK_STATS,
                ROUTE_TURBO_WORK_STATS,
                turboRecordEntity.tbsRecordId!!
            )
        }

        // 3.进行各个表的统计工作,如果状态为完成，则要统计时间(各个表的统计)
        // (1)完成编译加速实例的统计工作
        turboPlanInstanceService.updateStatInfo(
            turboPlanInstanceId = turboPlanInstanceEntity.id!!,
            estimateTimeSecond = turboRecordEntity?.estimateTimeSecond,
            executeTimeSecond = turboRecordEntity?.executeTimeSecond,
            startTime = turboRecordEntity?.startTime ?: LocalDateTime.now(),
            status = turboRecordEntity?.status ?: EnumDistccTaskStatus.STAGING.getTBSStatus(),
            createFlag = true
        )

        // (2)完成编译加速方案的统计工作
        turboPlanService.updateStatInfo(
            turboPlanId = turboPlanId,
            estimateTimeHour = turboRecordEntity?.estimateTimeSecond?.toDouble()?.div(3600),
            executeTimeHour = turboRecordEntity?.executeTimeSecond?.toDouble()?.div(3600),
            status = turboRecordEntity?.status ?: EnumDistccTaskStatus.STAGING.getTBSStatus(),
            createFlag = true
        )

        // (3)完成总览页面的统计工作
        turboSummaryService.upsertSummaryInfo(
            projectId = turboPlanEntity.projectId,
            engineCode = turboEngineConfig.engineCode,
            status = turboRecordEntity?.status ?: EnumDistccTaskStatus.STAGING.getTBSStatus(),
            executeTime = turboRecordEntity?.executeTimeSecond?.toDouble()?.div(3600),
            estimateTime = turboRecordEntity?.estimateTimeSecond?.toDouble()?.div(3600),
            createFlag = true
        )
    }

    /**
     * 更新编译加速记录
     */
    fun updateTurboRecord(
        turboDataMap: Map<String, Any>,
        turboEngineConfig: TTurboEngineConfigEntity,
        turboRecordUpdateDto: TurboRecordUpdateDto
    ) {
        // 1.获取对应加速方案实例id
        val tbsRecordId = turboDataMap["task_id"] as String?
        val buildId = turboDataMap["build_id"] as String?
        val status = turboDataMap["status"] as String?
        val startTime = (turboDataMap["start_time"] as? Int?)?.toLong() ?: (System.currentTimeMillis() / 1000)
        logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] update param-> tbs record id: $tbsRecordId | build id: $buildId | status: $status | start time: $startTime")
        if ((tbsRecordId.isNullOrBlank() && buildId.isNullOrBlank()) || status.isNullOrBlank()) {
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] tbs record id or status is null, tbs record id: $tbsRecordId, status: $status, build id: $buildId")
            return
        }

        // 2. 更新编译加速记录信息
        val turboRecordEntity = turboRecordService.updateTurboReport(
            turboDataMap = turboDataMap,
            tbsRecordId = tbsRecordId,
            engineCode = turboEngineConfig.engineCode,
            buildId = buildId,
            status = status,
            spelExpression = turboEngineConfig.spelExpression,
            spelParamMap = turboEngineConfig.spelParamMap.keys,
            startTime = startTime
        )

        if (null == turboRecordEntity) {
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] no turbo record has been updated")
            return
        }

        // --这里判断report状态status字段,finish才去查询统计信息,用于统计数据落地
        if (turboRecordEntity.status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
            rabbitTemplate.convertAndSend(
                EXCHANGE_TURBO_WORK_STATS,
                ROUTE_TURBO_WORK_STATS,
                turboRecordEntity.tbsRecordId!!
            )
        }

        // 3.进行各个表的统计工作,如果状态为完成，则要统计时间(各个表的统计)
        // (1)完成编译加速实例的统计工作
        turboPlanInstanceService.updateStatInfo(
            turboPlanInstanceId = turboRecordEntity.turboPlanInstanceId,
            startTime = Instant.ofEpochSecond(startTime).atZone(ZoneOffset.ofHours(8)).toLocalDateTime(),
            estimateTimeSecond = turboRecordEntity.estimateTimeSecond,
            executeTimeSecond = turboRecordEntity.executeTimeSecond,
            status = status,
            createFlag = false
        )

        // (2)完成编译加速方案的统计工作
        turboPlanService.updateStatInfo(
            turboPlanId = turboRecordEntity.turboPlanId!!,
            estimateTimeHour = turboRecordEntity.estimateTimeSecond.toDouble().div(3600),
            executeTimeHour = turboRecordEntity.executeTimeSecond.toDouble().div(3600),
            status = status,
            createFlag = false
        )

        // (3)完成总览页面的统计工作
        turboSummaryService.upsertSummaryInfo(
            projectId = turboRecordEntity.projectId,
            engineCode = turboEngineConfig.engineCode,
            status = status,
            executeTime = turboRecordEntity.executeTimeSecond.toDouble().div(3600),
            estimateTime = turboRecordEntity.estimateTimeSecond.toDouble().div(3600),
            createFlag = false
        )
    }

    /**
     * 刷新编译加速记录信息
     */
    fun refreshTurboRecord(
        turboPlanId: String,
        turboEngineConfig: TTurboEngineConfigEntity
    ) {
        val startCalculateTime = System.currentTimeMillis()
        // 1. 查询出该编译加速方案下面的所有编译加速方案实例
        val turboPlanInstanceList = turboPlanInstanceService.findByTurboPlanId(
            turboPlanId = turboPlanId
        )
        logger.info("turbo plan instance size: ${turboPlanInstanceList.size}")
        var totalPlanEstimateSecond = 0L
        // 2. 遍历编译加速方案实例
        if (!turboPlanInstanceList.isNullOrEmpty()) {
            turboPlanInstanceList.forEach { turboPlanInstanceEntity ->
                var totalInstanceEstimateSecond = 0L
                // 3. 遍历编译加速方案实例，查询出每个实例对应的编译加速记录清单
                val turboRecordList = turboRecordService.findTurboRecordListByTurboInstanceId(turboPlanInstanceEntity.id!!)
                if (!turboRecordList.isNullOrEmpty()) {
                    logger.info("turbo record list size: ${turboRecordList.size} with turbo instance plan id ${turboPlanInstanceEntity.id}")
                    // 4. 遍历每个加速实例下的加速记录，更新信息
                    turboRecordList.forEach { turboRecordEntity ->
                        if (turboRecordEntity.status == EnumDistccTaskStatus.FINISH.getTBSStatus()) {
                            val startTime = (turboRecordEntity.rawData["start_time"] as? Int?)?.toLong() ?: (System.currentTimeMillis() / 1000)
                            val turboRecordNewEntity = turboRecordService.updateTurboReportForRefresh(
                                turboDataMap = turboRecordEntity.rawData,
                                turboRecordId = turboRecordEntity.id!!,
                                engineCode = turboEngineConfig.engineCode,
                                status = turboRecordEntity.status,
                                spelExpression = turboEngineConfig.spelExpression,
                                spelParamMap = turboEngineConfig.spelParamMap.keys,
                                startTime = startTime
                            )
                            totalInstanceEstimateSecond += (turboRecordNewEntity?.estimateTimeSecond ?: 0L)
                            totalPlanEstimateSecond += (turboRecordNewEntity?.estimateTimeSecond ?: 0L)
                        }
                    }
                }
                turboPlanInstanceService.updateEstimateTime(turboPlanInstanceEntity.id!!, totalInstanceEstimateSecond)
            }
        }
        turboPlanService.updateEstimateTime(turboPlanId, totalPlanEstimateSecond)
        logger.info("turbo plan data update finish with id $turboPlanId, time cost: ${System.currentTimeMillis() - startCalculateTime}")
    }

    /**
     * 刷新项目维度统计表
     */
    fun refreshSummaryInfo(projectId: String) {
        val turboRecordList = turboRecordService.findListByProjectId(projectId)
        if (!turboRecordList.isNullOrEmpty()) {
            turboRecordList.groupBy { LocalDate.of(it.createdDate.year, it.createdDate.month, it.createdDate.dayOfMonth) }.forEach { (t, u) ->
                val sumEstimateTime = u.sumOf { it.estimateTimeSecond }.toDouble().div(3600)
                logger.info("summary date: $t, sum estimate time: $sumEstimateTime")
                turboSummaryService.updateEstimateTime(projectId, t, sumEstimateTime)
            }
        }
    }
}
