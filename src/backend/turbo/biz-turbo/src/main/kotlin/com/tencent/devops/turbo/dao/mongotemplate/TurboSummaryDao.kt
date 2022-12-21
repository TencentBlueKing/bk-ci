package com.tencent.devops.turbo.dao.mongotemplate

import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.model.TTurboDaySummaryEntity
import com.tencent.devops.turbo.pojo.TurboDaySummaryOverviewModel
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class TurboSummaryDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 插入更新编译加速按日统计信息
     */
    fun upsertTurboSummary(
        projectId: String,
        executeTime: Double?,
        estimateTime: Double?,
        createFlag: Boolean
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
            .addCriteria(Criteria.where("summary_day").`is`(LocalDate.now()))
        val update = Update()
        if (null != executeTime) {
            update.inc("execute_time", executeTime)
        }
        if (null != estimateTime) {
            update.inc("estimate_time", estimateTime)
        }
        if (createFlag) {
            update.inc("execute_count", 1)
        }
        update.set("created_by", codeccAdmin)
            .set("created_date", LocalDateTime.now())
            .set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())

        val findAndModifyOptions = FindAndModifyOptions()
        findAndModifyOptions.upsert(true)
        mongoTemplate.findAndModify(query, update, findAndModifyOptions, TTurboDaySummaryEntity::class.java)
    }

    /**
     * 查询开始时间和结束时间之间的数据 并以时间分组统计编译次数
     */
    fun getCompileNumberTrendData(
        projectId: String,
        startTime: LocalDate?,
        endTime: LocalDate?
    ): List<TurboDaySummaryOverviewModel> {
        val fieldsObj = Document()
        fieldsObj["summary_day"] = true
        fieldsObj["execute_count"] = true
        val query = BasicQuery(Document(), fieldsObj)
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        if (startTime != null && endTime != null) {
            query.addCriteria(Criteria.where("summary_day").gte(startTime).lte(endTime))
        }
        return mongoTemplate.find(query, TurboDaySummaryOverviewModel::class.java, "t_turbo_day_summary_entity")
    }

    /**
     * 查询开始时间和结束时间之间的数据 并以时间分组 统计实际耗时和预估耗时
     */
    fun getTimeConsumingTrendData(
        projectId: String,
        startTime: LocalDate?,
        endTime: LocalDate?
    ): List<TurboDaySummaryOverviewModel> {
        val fieldsObj = Document()
        fieldsObj["summary_day"] = true
        fieldsObj["estimate_time"] = true
        fieldsObj["execute_time"] = true
        val query = BasicQuery(Document(), fieldsObj)
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        if (startTime != null && endTime != null) {
            query.addCriteria(Criteria.where("summary_day").gte(startTime).lte(endTime))
        }
        return mongoTemplate.find(query, TurboDaySummaryOverviewModel::class.java, "t_turbo_day_summary_entity")
    }

    /**
     * 获取实际总耗时和预计总耗时
     */
    fun getExecuteTimeSumAndEstimateTimeSum(projectId: String): MutableList<TurboDaySummaryOverviewModel> {

        val match = Aggregation.match(Criteria.where("project_id").`is`(projectId))

        val group = Aggregation.group("project_id")
            .first("execute_time").`as`("execute_time").sum("execute_time").`as`("execute_time")
            .first("estimate_time").`as`("estimate_time").sum("estimate_time").`as`("estimate_time")

        val aggregation = Aggregation.newAggregation(match, group)
        val queryResults: AggregationResults<TurboDaySummaryOverviewModel> =
            mongoTemplate.aggregate(aggregation, "t_turbo_day_summary_entity", TurboDaySummaryOverviewModel::class.java)
        return queryResults.mappedResults
    }

    /**
     * 更新预估执行时间
     */
    fun updateEstimateTime(
        projectId: String,
        summaryDay: LocalDate,
        estimateTime: Double
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId).and("summary_day").`is`(summaryDay))
        val update = Update()
        update.set("estimate_time", estimateTime)
            .set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboDaySummaryEntity::class.java)
    }

    /**
     * 以项目维度分组统计预计时间和实际时间
     */
    fun findProjectBySummaryDatePage(
        summaryDate: LocalDate,
        pageNum: Int,
        pageSize: Int
    ): List<TurboDaySummaryOverviewModel> {
        val match = Aggregation.match(Criteria.where("summary_day").`is`(summaryDate))

        val group = Aggregation.group("project_id")
            .first("project_id").`as`("project_id")
            .sum("execute_time").`as`("execute_time")
            .sum("estimate_time").`as`("estimate_time")

        val skip = Aggregation.skip((pageNum * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val aggregation = Aggregation.newAggregation(match, group, skip, limit)
        val queryResults: AggregationResults<TurboDaySummaryOverviewModel> =
            mongoTemplate.aggregate(aggregation, "t_turbo_day_summary_entity", TurboDaySummaryOverviewModel::class.java)
        return queryResults.mappedResults
    }
}
