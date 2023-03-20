package com.tencent.devops.turbo.job

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.util.DateTimeUtils
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.MathUtil
import com.tencent.devops.common.util.constants.EXCHANGE_METRICS_STATISTIC_TURBO_DAILY
import com.tencent.devops.metrics.pojo.message.TurboReportEvent
import com.tencent.devops.turbo.dao.mongotemplate.TurboSummaryDao
import com.tencent.devops.turbo.pojo.TurboDaySummaryOverviewModel
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Suppress("SpringJavaAutowiredMembersInspection")
class BkMetricsDailyJob @Autowired constructor(
    private val eventDispatcher: SampleEventDispatcher,
    private val turboSummaryDao: TurboSummaryDao
): Job {
    companion object {
        private val logger = LoggerFactory.getLogger(BkMetricsDailyJob::class.java)
        private const val DEFAULT_PAGE_SIZE = 2000
    }

    override fun execute(context: JobExecutionContext) {
        logger.info("BkMetricsDailyJob context: ${JsonUtil.toJson(context.jobDetail)}")

        val jobParam = context.jobDetail.jobDataMap
        val pageSize = if (!jobParam.containsKey("pageSize")) {
            DEFAULT_PAGE_SIZE
        } else {
            jobParam["pageSize"] as Int
        }

        // 生成统计时间戳
        val statisticsLocalDate = if (jobParam.containsKey("statisticsDate")) {
            val dateStr = jobParam["statisticsDate"] as String
            DateTimeUtils.dateStr2LocalDate(dateStr = dateStr)
        } else {
            // 统计昨天
            LocalDate.now().minusDays(1)
        }
        val statisticsDateStr = DateTimeUtils.localDate2DateStr(statisticsLocalDate)

        // 分页从0开始统计，表示第一页
        var pageNum = 0
        do {
            val projectDaySummaryPage = turboSummaryDao.findProjectBySummaryDatePage(
                summaryDate = statisticsLocalDate,
                pageNum = pageNum,
                pageSize = pageSize
            )
            if (projectDaySummaryPage.isNullOrEmpty()) {
                break
            }

            projectDaySummaryPage.forEach {
                processAndSend(statisticsDate = statisticsDateStr, overviewModel = it)
            }

            pageNum++
        } while (projectDaySummaryPage.size >= pageSize)
        logger.info("BkMetricsDailyJob execute finish")
    }

    /**
     * 计算节省时间及推送数据
     */
    private fun processAndSend(statisticsDate: String, overviewModel: TurboDaySummaryOverviewModel) {
        val estimateTime = overviewModel.estimateTime ?: 0.0
        val executeTime = overviewModel.executeTime ?: 0.0
        // 单位：秒
        val saveTime = MathUtil.roundToTwoDigits(((estimateTime - executeTime) * 3600)).toDouble()

        eventDispatcher.dispatch(
            TurboReportEvent(
                statisticsTime = statisticsDate,
                projectId = overviewModel.projectId!!,
                turboSaveTime = saveTime
            )
        )
    }
}
