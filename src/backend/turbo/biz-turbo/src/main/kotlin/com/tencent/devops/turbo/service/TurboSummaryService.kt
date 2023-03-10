package com.tencent.devops.turbo.service

import com.tencent.devops.common.util.DateTimeUtils.YYYY_MM_DD_FORMAT
import com.tencent.devops.common.util.MathUtil
import com.tencent.devops.turbo.dao.mongotemplate.TurboSummaryDao
import com.tencent.devops.turbo.dao.repository.TurboDaySummaryRepository
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.enums.EnumEngineScene
import com.tencent.devops.turbo.model.TTurboDaySummaryEntity
import com.tencent.devops.turbo.model.pojo.EngineSceneEntity
import com.tencent.devops.turbo.pojo.TurboDaySummaryOverviewModel
import com.tencent.devops.turbo.pojo.TurboPlanInstanceModel
import com.tencent.devops.turbo.vo.EngineSceneVO
import com.tencent.devops.turbo.vo.TurboOverviewStatRowVO
import com.tencent.devops.turbo.vo.TurboOverviewTrendVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Suppress("MaxLineLength")
@Service
class TurboSummaryService @Autowired constructor(
    private val turboSummaryDao: TurboSummaryDao,
    private val turboPlanService: TurboPlanService,
    private val turboDaySummaryRepository: TurboDaySummaryRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TurboSummaryService::class.java)
    }

    /**
     * 更新按日统计，编译加速方案和方案实例信息
     */
    fun updateSummaryPlanAndInstanceInfo(
        turboPlanInstanceModel: TurboPlanInstanceModel,
        user: String
    ): String {
        // 1. 更新按日统计信息
        turboSummaryDao.upsertTurboSummary(
            projectId = turboPlanInstanceModel.projectId!!,
            executeTime = null,
            estimateTime = null,
            createFlag = true
        )
        // 2. 更新方案，方案实例信息
        return turboPlanService.addInstanceAndCountById(turboPlanInstanceModel, user)
    }

    /**
     * 插入更新编译加速按日统计信息
     */
    fun upsertSummaryInfo(
        projectId: String,
        engineCode: String,
        status: String,
        executeTime: Double?,
        estimateTime: Double?,
        createFlag: Boolean
    ) {
        if (status != EnumDistccTaskStatus.FINISH.getTBSStatus() && !createFlag) {
            logger.info("status is not finish and create flag is false")
            return
        }
        val tTurboDaySummaryEntity = turboSummaryDao.upsertTurboSummary(
            projectId = projectId,
            executeTime = executeTime,
            estimateTime = estimateTime,
            createFlag = createFlag
        )

        if (createFlag) {
            val engineSceneList = tTurboDaySummaryEntity!!.engineSceneList ?: mutableListOf()
            val engineSceneEntityMap = engineSceneList.associateBy { it.sceneCode }.toMutableMap()

            val engineSceneEntity = engineSceneEntityMap.computeIfAbsent(engineCode) {
                when (engineCode) {
                    EnumEngineScene.DISTTASKCC.getCode() -> {
                        EngineSceneEntity(EnumEngineScene.DISTTASKCC.getName(), engineCode)
                    }
                    // 暂存，后面从workStat数据中识别区分
                    EnumEngineScene.UE4COMPILE.getCode() -> {
                        EngineSceneEntity(EnumEngineScene.UE4COMPILE.getName(), engineCode)
                    }
                    // distcc和未知的场景都默认
                    else -> {
                        EngineSceneEntity(engineCode, engineCode)
                    }
                }
            }

            engineSceneEntity.incCount()
            tTurboDaySummaryEntity.engineSceneList = engineSceneEntityMap.values.toList()
            turboDaySummaryRepository.save(tTurboDaySummaryEntity)
        }
    }

    /**
     * 获取总览页面统计栏数据
     */
    fun getOverviewStatRowData(projectId: String): TurboOverviewStatRowVO {

        // 获取加速方案数 和 加速次数
        val engineCountAndExecuteCount = turboPlanService.getInstanceNumAndExecuteCount(projectId)

        return if (engineCountAndExecuteCount.isNotEmpty()) {
            // 实际总耗时
            val executeTimeHour = engineCountAndExecuteCount[0].executeTime!!
            // 预估总耗时
            val estimateTimeHour = engineCountAndExecuteCount[0].estimateTime!!
            val instanceNum = engineCountAndExecuteCount[0].instanceNum!!
            val executeCount = engineCountAndExecuteCount[0].executeCount!!

            TurboOverviewStatRowVO(
                instanceNum = instanceNum,
                executeCount = executeCount,
                // 总耗时
                executeTimeHour = MathUtil.roundToTwoDigits(executeTimeHour),
                savingRate = if (estimateTimeHour == 0.0) "0.00" else MathUtil.roundToTwoDigits((estimateTimeHour - executeTimeHour).div(estimateTimeHour).times(100))
            )
        } else {
            TurboOverviewStatRowVO()
        }
    }

    /**
     * 获取总览页面耗时分布趋势图数据
     */
    fun getTimeConsumingTrendData(dateType: String, projectId: String): List<TurboOverviewTrendVO> {
        val startDay = getStartTimeAndEndTimeByDatetype(dateType)
        // 查询开始时间和结束时间之间 实际耗时数据 及 预估耗时数据 并以日期分组
        val timeConsumingTrendDataList = turboSummaryDao.getTimeConsumingTrendData(
            projectId, startDay,
            LocalDate.now()
        )

        return getCompileNumberAndTimeConsumingTrendData(timeConsumingTrendDataList, startDay)
    }

    /**
     * 获取总览页面编译次数趋势图数据
     */
    fun getCompileNumberTrendData(dateType: String, projectId: String): List<TurboOverviewTrendVO> {

        val startDay = getStartTimeAndEndTimeByDatetype(dateType)

        // 查询开始时间和结束时间之间的数据 并以时间分组
        val compileNumberTrendDataList = turboSummaryDao.getCompileNumberTrendData(
            projectId,
            startDay,
            LocalDate.now()
        )
        return getCompileNumberAndTimeConsumingTrendData(compileNumberTrendDataList, startDay)
    }

    /**
     * 根据参数Datetype获取开始时间和结束时间及时间段
     */
    private fun getStartTimeAndEndTimeByDatetype(dateType: String): LocalDate {
        return when (dateType) {
            "week" -> {
                // 如果DateType = 周 则获取最近一周的日期
                LocalDate.now().minusDays(6)
            }
            "month" -> {
                // 如果DateType = 月 则获取最近一月的日期
                LocalDate.now().minusMonths(1)
            }
            else -> {
                // 如果DateType = 年 则获取最近一年的日期
                LocalDate.now().minusYears(1)
            }
        }
    }

    /**
     * 获取 编译次数/耗时分布 趋势图数据
     */
    private fun getCompileNumberAndTimeConsumingTrendData(compileNumberAndTimeConsumingTrendDataList: List<TurboDaySummaryOverviewModel>, startDay: LocalDate): MutableList<TurboOverviewTrendVO> {
        val compileNumberTrendDataMap =
            compileNumberAndTimeConsumingTrendDataList.groupBy { DateTimeFormatter.ofPattern("yyyy-MM-dd").format(it.summaryDay) }

        val compileNumberAndTimeConsumingTrendData = mutableListOf<TurboOverviewTrendVO>()
        for (i in 0..startDay.until(LocalDate.now(), ChronoUnit.DAYS)) {
            val dayStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(startDay.plusDays(i))
            val turboDaySummaryOverviewList = compileNumberTrendDataMap[dayStr]
            val turboDaySummaryOverviewModel =
                if (turboDaySummaryOverviewList.isNullOrEmpty()) null else turboDaySummaryOverviewList[0]
            compileNumberAndTimeConsumingTrendData.add(
                TurboOverviewTrendVO(
                    date = dayStr,
                    executeCount = turboDaySummaryOverviewModel?.executeCount ?: 0,
                    executeTime = turboDaySummaryOverviewModel?.executeTime ?: 0.0,
                    estimateTime = turboDaySummaryOverviewModel?.estimateTime ?: 0.0
                )
            )
        }
        return compileNumberAndTimeConsumingTrendData
    }

    /**
     * 查询所有总结清单
     */
    fun findAllSummaryList(projectId: String): List<TTurboDaySummaryEntity> {
        return turboDaySummaryRepository.findAll()
    }

    /**
     * 更新预估时间
     */
    fun updateEstimateTime(
        projectId: String,
        summaryDay: LocalDate,
        estimateTime: Double
    ) {
        turboSummaryDao.updateEstimateTime(projectId, summaryDay, estimateTime)
    }

    /**
     * 获取总览页格场景的加速次数趋势图数据
     */
    fun getExecuteCountTrendData(dateType: String, projectId: String): List<TurboOverviewTrendVO> {
        val startDay = getStartTimeAndEndTimeByDatetype(dateType)

        val executeCountTrendDataList = turboSummaryDao.getExecuteCountTrendData(
            projectId,
            startDay,
            LocalDate.now()
        )
        val executeCountTrendDataMap =
            executeCountTrendDataList.groupBy { DateTimeFormatter.ofPattern(YYYY_MM_DD_FORMAT).format(it
                .summaryDay) }

        val executeCountTrendData = mutableListOf<TurboOverviewTrendVO>()
        for (i in 0..startDay.until(LocalDate.now(), ChronoUnit.DAYS)) {
            val dayStr = DateTimeFormatter.ofPattern(YYYY_MM_DD_FORMAT).format(startDay.plusDays(i))
            val turboDaySummaryOverviewList = executeCountTrendDataMap[dayStr]
            val turboDaySummaryOverviewModel =
                if (turboDaySummaryOverviewList.isNullOrEmpty()) null else turboDaySummaryOverviewList[0]
            val engineSceneList = turboDaySummaryOverviewModel?.engineSceneList?.map {
                    EngineSceneVO(it.sceneName, it.sceneCode, it.executeCount)
            }
            logger.info("engineSceneVOList: ${engineSceneList?.size}")

            executeCountTrendData.add(
                TurboOverviewTrendVO(
                    date = dayStr,
                    engineSceneList = engineSceneList ?: listOf(),
                    executeCount = turboDaySummaryOverviewModel?.executeCount ?: 0,
                    executeTime = turboDaySummaryOverviewModel?.executeTime ?: 0.0,
                    estimateTime = turboDaySummaryOverviewModel?.estimateTime ?: 0.0
                )
            )
        }
        return executeCountTrendData
    }
}
