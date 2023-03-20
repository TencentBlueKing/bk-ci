package com.tencent.devops.turbo.dao.mongotemplate

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.dto.TurboRecordRefreshModel
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.model.TTurboRecordEntity
import com.tencent.devops.turbo.pojo.TurboPlanInstanceStatModel
import com.tencent.devops.turbo.pojo.TurboPlanStatModel
import com.tencent.devops.turbo.pojo.TurboRecordModel
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("MaxLineLength")
@Repository
class TurboRecordDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 根据实例id计算平均值
     */
    fun findAverageByInstanceId(turboPlanInstanceId: String): List<TurboPlanInstanceStatModel> {
        val match = Aggregation.match(
            Criteria.where("turbo_plan_instance_id").`is`(turboPlanInstanceId).and("status").`is`(EnumDistccTaskStatus.FINISH.getTBSStatus())
        )
        val group = Aggregation.group("turbo_plan_instance_id").avg("estimate_time").`as`("averageEstimateTime").avg("execute_time")
            .`as`("averageExecuteTime")
        val aggregate = Aggregation.newAggregation(match, group)

        return mongoTemplate.aggregate(
            aggregate,
            "t_turbo_report_entity",
            TurboPlanInstanceStatModel::class.java
        ).mappedResults
    }

    /**
     * 根据加速方案id计算总和
     */
    fun findSumByPlanId(turboPlanId: String): List<TurboPlanStatModel> {
        val match = Aggregation.match(
            Criteria.where("turbo_plan_id").`is`(turboPlanId).and("status").`is`(EnumDistccTaskStatus.FINISH.getTBSStatus())
        )
        val group = Aggregation.group("turbo_plan_id").sum("estimate_time").`as`("sumEstimateTime").sum("execute_time")
            .`as`("sumExecuteTime")
        val aggregate = Aggregation.newAggregation(match, group)

        return mongoTemplate.aggregate(
            aggregate,
            "t_turbo_report_entity",
            TurboPlanStatModel::class.java
        ).mappedResults
    }

    /**
     * 更新编译加速记录信息
     */
    fun updateRecordInfo(
        tbsRecordId: String?,
        buildId: String?,
        turboDataMap: Map<String, Any?>,
        status: String,
        executeTimeSecond: Long?,
        executeTimeValue: String?,
        estimateTimeSecond: Long?,
        estimateTimeValue: String?,
        turboRatio: String?
    ): TTurboRecordEntity? {
        val query = Query()
        if (!buildId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        } else if (!tbsRecordId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("tbs_record_id").`is`(tbsRecordId))
        }
        // 需要添加状态不为完成或者失败
        query.addCriteria(
            Criteria.where("status").nin(
                EnumDistccTaskStatus.FINISH.getTBSStatus(),
                EnumDistccTaskStatus.FAILED.getTBSStatus()
            )
        )
        val update = Update()
        if (!tbsRecordId.isNullOrBlank()) {
            update.set("tbs_record_id", tbsRecordId)
        }
        if (!buildId.isNullOrBlank()) {
            update.set("build_id", buildId)
        }
        update.set("raw_data", turboDataMap)
        update.set("status", status)
        if (null != estimateTimeSecond) {
            update.set("estimate_time", estimateTimeSecond)
        }
        if (!estimateTimeValue.isNullOrBlank()) {
            update.set("estimate_time_value", estimateTimeValue)
        }
        if (null != executeTimeSecond) {
            update.set("execute_time", executeTimeSecond)
        }
        if (!executeTimeValue.isNullOrBlank()) {
            update.set("execute_time_value", executeTimeValue)
        }
        if (!turboRatio.isNullOrBlank()) {
            update.set("turbo_ratio", turboRatio)
        }
        update.set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())
            .set("created_by", codeccAdmin)
            .set("created_date", LocalDateTime.now())
        val findAndModifyOptions = FindAndModifyOptions()
        findAndModifyOptions.returnNew(true)
        return mongoTemplate.findAndModify(query, update, findAndModifyOptions, TTurboRecordEntity::class.java)
    }

    /**
     * 获取加速历史列表
     */
    fun getTurboRecordHistoryList(pageable: Pageable, turboRecordModel: TurboRecordModel, startTime: LocalDate?, endTime: LocalDate?): Page<TTurboRecordEntity> {
        val criteriaList = mutableListOf<Criteria>()

        // 项目Id
        val projectId = turboRecordModel.projectId
        if (!projectId.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("project_id").`in`(projectId))
        }
        // 加速方案Id
        val turboPlanId = turboRecordModel.turboPlanId
        if (!turboPlanId.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("turbo_plan_id").`in`(turboPlanId))
        }
        // 流水线ID
        val pipelineId = turboRecordModel.pipelineId
        if (!pipelineId.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("pipeline_id").`in`(pipelineId))
        }
        // 客户端ip
        val clientIp = turboRecordModel.clientIp
        if (!clientIp.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("client_ip").`in`(clientIp))
        }
        // 状态
        val status = turboRecordModel.status
        if (!status.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("status").`in`(status))
        }
        // 时间
        if (null != startTime && null != endTime) {
            criteriaList.add(Criteria.where("start_time").gte(startTime).lt(endTime))
        }

        val criteria = Criteria()
        if (!criteriaList.isNullOrEmpty()) {
            criteria.andOperator(*criteriaList.toTypedArray())
        }
        val query = Query()
        query.addCriteria(criteria)
        query.with(pageable)
        val totalCount = mongoTemplate.count(Query(criteria), "t_turbo_report_entity")

        // 分页排序
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val queryResults = mongoTemplate.find(query, TTurboRecordEntity::class.java)

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults)
    }

    /**
     * 更新记录失败时间
     */
    fun updateRecordStatus(
        tbsRecordId: String?,
        buildId: String?,
        status: String,
        user: String
    ) {
        val query = Query()
        if (!buildId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        } else if (!tbsRecordId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("tbs_record_id").`is`(tbsRecordId))
        }
        // 只更新创建时间距今6小时以上的记录
        query.addCriteria(Criteria.where("created_date").lt(LocalDateTime.now().minusHours(6L)))

        val update = Update()
        update.set("status", status)
            .set("updated_date", LocalDateTime.now())
            .set("updated_by", user)
        mongoTemplate.updateFirst(query, update, TTurboRecordEntity::class.java)
    }

    /**
     * 更新记录状态
     */
    fun updateRecordStatusForPlugin(
        buildId: String,
        status: String,
        user: String
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("build_id").`is`(buildId))
            .addCriteria(
                Criteria.where("status").nin(
                    EnumDistccTaskStatus.FINISH.getTBSStatus(),
                    EnumDistccTaskStatus.FAILED.getTBSStatus()
                )
            )
        val update = Update()
        update.set("status", status)
            .set("updated_date", LocalDateTime.now())
            .set("updated_by", user)
        mongoTemplate.updateFirst(query, update, TTurboRecordEntity::class.java)
    }

    /**
     * 通过方案id寻找记录清单
     */
    fun findByTurboPlanInstanceId(
        turboPlanInstanceId: String
    ): List<TTurboRecordEntity> {
        val query = Query()
        query.addCriteria(Criteria.where("turbo_plan_instance_id").`is`(turboPlanInstanceId))
        return mongoTemplate.find(query, TTurboRecordEntity::class.java)
    }

    /**
     * 刷新数据逻辑
     */
    fun updateRecordForRefresh(
        turboRecordId: String,
        estimateTimeSecond: Long?,
        estimateTimeValue: String?,
        turboRatio: String?
    ): TTurboRecordEntity? {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboRecordId))
        val update = Update()
        if (null != estimateTimeSecond) {
            update.set("estimate_time", estimateTimeSecond)
        }
        if (!estimateTimeValue.isNullOrBlank()) {
            update.set("estimate_time_value", estimateTimeValue)
        }
        if (!turboRatio.isNullOrBlank()) {
            update.set("turbo_ratio", turboRatio)
        }
        update.set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())
        val findAndModifyOptions = FindAndModifyOptions()
        findAndModifyOptions.returnNew(true)
        return mongoTemplate.findAndModify(query, update, findAndModifyOptions, TTurboRecordEntity::class.java)
    }

    /**
     * 根据项目id查询
     */
    fun findByProjectId(
        projectId: String
    ): List<TurboRecordRefreshModel>? {
        val fieldsObj = Document()
        fieldsObj["created_date"] = true
        fieldsObj["estimate_time"] = true
        val query = BasicQuery(Document(), fieldsObj)
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
            .addCriteria(Criteria.where("estimate_time").ne(null))
            .addCriteria(Criteria.where("created_date").ne(null))
        return mongoTemplate.find(query, TurboRecordRefreshModel::class.java, "t_turbo_report_entity")
    }
}
